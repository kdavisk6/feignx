/*
 * Copyright 2019-2021 OpenFeign Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feign.impl;

import feign.Client;
import feign.ExceptionHandler;
import feign.Logger;
import feign.Request;
import feign.RequestEncoder;
import feign.RequestEntity;
import feign.RequestInterceptor;
import feign.Response;
import feign.ResponseDecoder;
import feign.Retry;
import feign.TargetMethodDefinition;
import feign.TargetMethodHandler;
import feign.TargetMethodParameterDefinition;
import feign.exception.FeignException;
import feign.http.RequestSpecification;
import feign.impl.type.TypeDefinition;
import feign.support.Assert;
import feign.template.ExpanderRegistry;
import feign.template.ExpressionExpander;
import feign.template.SimpleTemplateParameter;
import feign.template.TemplateParameter;
import feign.template.expander.CachingExpanderRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.slf4j.LoggerFactory;

/**
 * Base HttpMethod Handler implementation.  HttpRequest preparation is always done on the calling
 * thread.  HttpRequest execution is delegated to the provided {@link Executor}.  Response
 * processing is the responsibility of the subclasses.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractTargetMethodHandler implements TargetMethodHandler {


  protected TargetMethodDefinition targetMethodDefinition;
  private final org.slf4j.Logger log;
  private final RequestEncoder encoder;
  private final List<RequestInterceptor> interceptors;
  private final Client client;
  private final ResponseDecoder decoder;
  private final ExceptionHandler exceptionHandler;
  private final Logger logger;
  private final Executor executor;
  private final Retry retry;
  private final Map<Integer, TemplateParameter> parameterMap = new ConcurrentHashMap<>();
  private ExpanderRegistry expanderRegistry = new CachingExpanderRegistry();

  /**
   * Creates a new Abstract Target HttpMethod Handler.
   *
   * @param targetMethodDefinition containing the method configuration.
   * @param encoder to use when preparing the request.
   * @param interceptors to apply to the request before processing.
   * @param client to send the request and create the response.
   * @param decoder to use when parsing the response.
   * @param exceptionHandler to delegate to when an exception occurs.
   * @param executor to request the request on.
   * @param logger for logging requests and responses.
   * @param retry for retrying requests.
   */
  protected AbstractTargetMethodHandler(
      TargetMethodDefinition targetMethodDefinition, RequestEncoder encoder,
      List<RequestInterceptor> interceptors, Client client, ResponseDecoder decoder,
      ExceptionHandler exceptionHandler, Executor executor, Logger logger,
      Retry retry) {
    Assert.isNotNull(targetMethodDefinition, "targetMethodDefinition is required.");
    Assert.isNotNull(encoder, "encoder is required.");
    Assert.isNotNull(client, "client is required.");
    Assert.isNotNull(decoder, "decoder is required.");
    Assert.isNotNull(exceptionHandler, "exceptionHandler is required.");
    Assert.isNotNull(executor, "executor is required.");
    Assert.isNotNull(retry, "retry is required");
    Assert.isNotNull(logger, "logger is required.");
    this.encoder = encoder;
    this.interceptors = (interceptors == null) ? Collections.emptyList() : interceptors;
    this.client = client;
    this.decoder = decoder;
    this.exceptionHandler = exceptionHandler;
    this.executor = executor;
    this.targetMethodDefinition = targetMethodDefinition;
    this.logger = logger;
    this.retry = retry;
    this.log = LoggerFactory.getLogger(targetMethodDefinition.getTargetType());

  }

  /**
   * Execute the HttpRequest pipeline.
   *
   * @param arguments for the method.
   * @return the result of the request.
   */
  @Override
  public Object execute(final Object[] arguments) {

    /* create an asynchronous chain, using the provided executor.  this implementation
     * is not non-blocking, however, it does try to take advantage of the executor provided.
     *
     * if the default executor is used, each one of these steps will be executed on the calling
     * thread and not on a different pool or on the default fork-join common pool, making
     * this call effectively synchronous.
     */
    CompletableFuture<Object> result =
        CompletableFuture.supplyAsync(() -> this.resolve(arguments), this.executor)
            .thenApplyAsync(this::intercept, this.executor)
            .thenApplyAsync(specification -> encode(specification, arguments), this.executor)
            .thenApplyAsync(RequestSpecification::build, this.executor)
            .thenApplyAsync(this::request, this.executor)
            .handleAsync((response, throwable) -> {
              if (throwable != null) {
                /* dispatch to the error handler */
                this.log.error("Error occurred during method processing.  "
                        + "Passing to Exception Handler.  Exception: {}: {}",
                    throwable.getClass().getSimpleName(), throwable.getMessage());
                throw exceptionHandler.apply(throwable);
              } else {
                try {
                  /* decode the response */
                  return decode(response);
                } catch (Exception ex) {
                  this.log.error(
                      "Error occurred processing the response.  Passing to Exception Handler."
                          + "  Exception: {} {}", ex.getClass().getSimpleName(), ex.getMessage());
                  throw exceptionHandler.apply(ex);
                }
              }
            }, this.executor);

    /* delegate any additional handling to the sub classes. */
    return this.handleResponse(result);
  }

  /**
   * Process the results of the HttpRequest.
   *
   * @param result Future containing the results of the request.
   * @return the result of the request, decoded if necessary.
   */
  protected abstract Object handleResponse(CompletableFuture<Object> result);

  /**
   * Resolve any parameters and variables for this Request.
   *
   * @param arguments containing the values to use.
   * @return a {@link RequestSpecification} instance.
   */
  protected RequestSpecification resolve(Object[] arguments) {
    this.log.debug("Started processing of request: {}", this.targetMethodDefinition.getTag());
    RequestSpecification requestSpecification = this.targetMethodDefinition
        .requestSpecification(this.mapArguments(arguments));
    this.log.debug("UriTemplate resolved: {}", requestSpecification.uri());
    return requestSpecification;
  }

  /**
   * Apply any {@link RequestInterceptor}s.
   *
   * @param requestSpecification to intercept.
   */
  protected RequestSpecification intercept(RequestSpecification requestSpecification) {
    if (!this.interceptors.isEmpty()) {
      this.log.debug("Applying interceptors");
      RequestSpecification result = requestSpecification;
      for (RequestInterceptor interceptor : this.interceptors) {
        /* apply all of the interceptors, in order */
        result = interceptor.apply(result);
      }
      return result;
    }

    /* no op */
    return requestSpecification;
  }

  /**
   * Determines which method argument contains the Request body, if any.
   *
   * @param arguments for the method.
   * @return the object to use as the request body, if any.
   */
  protected Optional<Object> getRequestBody(Object[] arguments) {
    Object body =
        (this.targetMethodDefinition.getBody() != -1) ? arguments[this.targetMethodDefinition
            .getBody()] : null;
    return Optional.ofNullable(body);
  }

  /**
   * Encode the Request Body, if required.
   *
   * @param requestSpecification to receive the encoded result.
   * @param arguments that may contain the request body.
   */
  protected RequestSpecification encode(RequestSpecification requestSpecification,
      Object[] arguments) {
    this.getRequestBody(arguments)
        .ifPresent(body -> {
          this.log.debug("Encoding Request Body: {}", body.getClass().getSimpleName());
          RequestEntity entity = this.encoder.apply(body, requestSpecification);
          if (entity != null) {
            requestSpecification.content(entity);
          }
        });
    return requestSpecification;
  }

  /**
   * Execute the Request on the Executor.
   *
   * @param request to request.
   * @return a {@link CompletableFuture} containing the Response.
   */
  protected Response request(final Request request) {
    try {
      this.logRequest(targetMethodDefinition.getTag(), request);
      final Response response =
          this.retry.execute(targetMethodDefinition.getTag(), request, client::request);
      this.logResponse(targetMethodDefinition.getTag(), response);
      return response;
    } catch (Throwable th) {
      throw new FeignException(th.getMessage(), th, this.targetMethodDefinition.getTag());
    }
  }

  /**
   * Decode the Response.
   *
   * @param response to decode.
   * @return the desired decoded result
   */
  protected Object decode(Response response) {
    TypeDefinition typeDefinition = targetMethodDefinition.getReturnType();
    Class<?> returnType = typeDefinition.getType();
    if (void.class == returnType || (response == null || response.body() == null)) {
      return null;
    } else if (Response.class == returnType) {
      /* no need to decode */
      this.log.debug("Response type is feign.Response, no decoding necessary.");
      return response;
    } else {
      try {
        /* decode the response */
        this.log.debug("Decoding Response: {}", response);
        return this.decode(response, typeDefinition);
      } catch (Exception ex) {
        throw new FeignException(ex.getMessage(), ex, this.targetMethodDefinition.getTag());
      }
    }
  }

  /**
   * Decode the Response, using the TypeDefinition provided.
   *
   * @param response to decode.
   * @param typeDefinition to use to determine what the resulting type should be.
   * @return the response body decoded into the desired type.
   * @throws Exception if the {@link Response} cannot be closed after decoding.
   */
  private Object decode(Response response, TypeDefinition typeDefinition) throws Exception {
    /* before jumping into and just passing the raw type the decoder, there are
     * certain types that act as containers.  when decoding these types, we want to
     * decode the 'contained' type and then wrap the result in the desired container,
     */
    try (response) {
      if (typeDefinition.isContainer()) {
        /* we want to decode the actual type */
        return this.decoder.decode(response, typeDefinition.getActualType());
      } else {
        return this.decoder.decode(response, typeDefinition.getType());
      }
    }
  }

  /**
   * Map the Argument list to any registered Template Parameters.
   *
   * @param arguments to map.
   * @return a new Map, where the argument matches corresponding Template parameter.
   * @throws IllegalStateException if the arguments could not be mapped.
   */
  private Map<TemplateParameter, Object> mapArguments(Object[] arguments) {
    Map<TemplateParameter, Object> variables = new LinkedHashMap<>();
    for (int i = 0; i < arguments.length; i++) {
      final int index = i;
      final Object argument = arguments[i];
      Optional<TargetMethodParameterDefinition> parameterDefinition =
          this.targetMethodDefinition.getParameterDefinition(i);
      parameterDefinition.ifPresent(parameter -> variables.put(
          getTemplateParameterForIndex(index, parameter), argument));
    }
    return variables;
  }

  /**
   * Retrieve the {@link TemplateParameter} for the specified method parameter index.
   *
   * @param index of the parameter.
   * @param definition of the parameter.
   * @return the {@link TemplateParameter} instance at this index.
   * @throws IllegalStateException if the {@link TemplateParameter} instance could not be retrieved.
   */
  private TemplateParameter getTemplateParameterForIndex(int index,
      TargetMethodParameterDefinition definition) {
    return this.parameterMap.computeIfAbsent(index,
        idx -> new SimpleTemplateParameter(definition.getParameter(),
            getExpressionExpanderFor(definition)));
  }

  /**
   * Obtain the {@link ExpressionExpander} instance for definition.
   *
   * @param parameterDefinition to evaluate.
   * @return the {@link ExpressionExpander} instance for the parameter.
   * @throws IllegalStateException if the {@link ExpressionExpander} instance could not be obtained.
   */
  @SuppressWarnings("unchecked")
  private ExpressionExpander getExpressionExpanderFor(
      TargetMethodParameterDefinition parameterDefinition) {
    String expanderClassName = parameterDefinition.getExpanderClassName();
    try {
      Class<? extends ExpressionExpander> expanderClass =
          (Class<? extends ExpressionExpander>) Class.forName(expanderClassName);
      return this.expanderRegistry.getExpander(expanderClass, parameterDefinition.getType());
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Expression Expander instance " + expanderClassName + " not found.", ex);
    }
  }

  /**
   * Log the Request.
   *
   * @param request to log.
   */
  protected void logRequest(String method, Request request) {
    this.logger.logRequest(method, request);
  }

  /**
   * Log the Response.
   *
   * @param response to log
   */
  protected void logResponse(String method, Response response) {
    this.logger.logResponse(method, response);
  }


  /**
   * Override the Expander Registry.
   *
   * @param expanderRegistry to use.
   */
  void setExpanderRegistry(ExpanderRegistry expanderRegistry) {
    this.expanderRegistry = expanderRegistry;
  }
}
