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
import feign.Contract;
import feign.ExceptionHandler;
import feign.FeignConfiguration;
import feign.FeignConfigurationBuilder;
import feign.Logger;
import feign.RequestEncoder;
import feign.RequestInterceptor;
import feign.ResponseDecoder;
import feign.Retry;
import feign.Target;
import feign.support.Assert;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Base Configuration Builder.  Can be extended to add additional fluent methods customizing
 * specific Feign extensions.
 *
 * @param <B> {@link FeignConfigurationBuilder} to chain.
 * @param <C> {@link FeignConfiguration} being built.
 */
public abstract class AbstractFeignConfigurationBuilder
    <B extends AbstractFeignConfigurationBuilder<B, C>, C extends FeignConfiguration>
    implements FeignConfigurationBuilder<B, C> {

  protected final B self;
  protected URI uri;
  protected Client client;
  protected RequestEncoder encoder;
  protected List<RequestInterceptor> interceptors = new ArrayList<>();
  protected ResponseDecoder decoder;
  protected Executor executor;
  protected Contract contract;
  protected ExceptionHandler exceptionHandler;
  protected Target<?> target;
  protected Logger logger;
  protected Retry retry;

  /**
   * Creates a new FeignConfigurationBuilder.
   *
   * @param self reference to the builder implementation to chain.
   */
  protected AbstractFeignConfigurationBuilder(Class<B> self) {
    this.self = self.cast(this);
  }

  /**
   * Client implementation to use.
   *
   * @param client instance.
   * @return the builder reference chain.
   */
  @Override
  public B client(Client client) {
    Assert.isNotNull(client, "client cannot be null.");
    this.client = client;
    return this.self;
  }

  /**
   * Request Encoder implementation to use.
   *
   * @param encoder instance.
   * @return the builder reference chain.
   */
  @Override
  public B encoder(RequestEncoder encoder) {
    Assert.isNotNull(encoder, "encoder cannot be null");
    this.encoder = encoder;
    return this.self;
  }

  /**
   * Response Decoder implementation to use.
   *
   * @param decoder instance.
   * @return the builder reference chain.
   */
  @Override
  public B decoder(ResponseDecoder decoder) {
    Assert.isNotNull(decoder, "decoder cannot be null.");
    this.decoder = decoder;
    return this.self;
  }

  /**
   * Executor to use when {@link Client}s are submitting requests.
   *
   * @param executor instance.
   * @return the builder reference chain.
   */
  @Override
  public B executor(Executor executor) {
    Assert.isNotNull(executor, "executor cannot be null.");
    this.executor = executor;
    return this.self;
  }

  /**
   * Contract to apply to the Target instances.
   *
   * @param contract to apply.
   * @return the builder reference chain.
   */
  @Override
  public B contract(Contract contract) {
    Assert.isNotNull(contract, "contract cannot be null.");
    this.contract = contract;
    return this.self;
  }

  /**
   * Target instance to wrap.
   *
   * @param target instance.
   * @return the builder reference chain.
   */
  @Override
  public B target(Target<?> target) {
    Assert.isNotNull(target, "target cannot be null.");
    this.target = target;
    return this.self;
  }

  /**
   * Exception Handler implementation to use.
   *
   * @param exceptionHandler instance.
   * @return the builder reference chain.
   */
  @Override
  public B exceptionHandler(ExceptionHandler exceptionHandler) {
    Assert.isNotNull(exceptionHandler, "exception handler cannot be null.");
    this.exceptionHandler = exceptionHandler;
    return this.self;
  }

  /**
   * Request Interceptor to apply.
   *
   * @param interceptor instance.
   * @return the builder reference chain.
   */
  @Override
  public B interceptor(RequestInterceptor interceptor) {
    Assert.isNotNull(interceptor, "interceptor cannot be null.");
    this.interceptors.add(interceptor);
    return this.self;
  }

  /**
   * Logger instance.
   *
   * @param logger instance to use.
   * @return the builder reference chain.
   */
  @Override
  public B logger(Logger logger) {
    Assert.isNotNull(logger, "log cannot be null");
    this.logger = logger;
    return this.self;
  }

  /**
   * Retry instance.
   *
   * @param retry instance to use.
   * @return the builder reference chain.
   */
  @Override
  public B retry(Retry retry) {
    Assert.isNotNull(retry, "retry cannot be null");
    this.retry = retry;
    return this.self;
  }

  /**
   * URI to use for all requests.
   *
   * @param uri to base all requests on.
   * @return the builder reference chain
   */
  @Override
  public B uri(URI uri) {
    Assert.isNotNull(uri, "uri cannot be null");
    this.uri = uri;
    return this.self;
  }
}
