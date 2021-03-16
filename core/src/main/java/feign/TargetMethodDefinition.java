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

package feign;

import feign.http.HttpHeader;
import feign.http.HttpMethod;
import feign.http.RequestSpecification;
import feign.impl.type.TypeDefinition;
import feign.impl.type.TypeDefinitionFactory;
import feign.support.Assert;
import feign.template.TemplateParameter;
import feign.template.UriTemplate;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Definition of a method on a Target.
 */
@ThreadSafe
@Immutable
public final class TargetMethodDefinition {

  private final Target<?> target;
  private final String name;
  private final String tag;
  private final HttpMethod method;
  private final transient TypeDefinition returnType;
  private final UriTemplate template;
  private final Collection<HttpHeader> headers;
  private final Map<Integer, TargetMethodParameterDefinition> parameterMap;
  private final Integer bodyArgumentIndex;
  private final boolean followRedirects;
  private final long connectTimeout;
  private final long readTimeout;

  public static Builder builder(Target<?> target) {
    return new Builder(target);
  }

  /**
   * Creates a new {@link TargetMethodDefinition} from an existing instance.
   *
   * @param targetMethodDefinition to copy.
   */
  public static Builder from(TargetMethodDefinition targetMethodDefinition) {
    /* create a new builder */
    Builder builder = new Builder(targetMethodDefinition.target);

    /* populate the builder with the instance values */
    builder.name(targetMethodDefinition.name)
        .tag(targetMethodDefinition.tag)
        .method(targetMethodDefinition.method)
        .body(targetMethodDefinition.bodyArgumentIndex)
        .followRedirects(targetMethodDefinition.followRedirects)
        .connectTimeout(targetMethodDefinition.connectTimeout)
        .readTimeout(targetMethodDefinition.readTimeout);

    if (targetMethodDefinition.returnType != null) {
      builder.returnType(targetMethodDefinition.returnType);
    }
    if (targetMethodDefinition.template != null) {
      builder.uri(targetMethodDefinition.template.toString());
    }

    targetMethodDefinition.parameterMap.forEach(
        builder::parameterDefinition);
    targetMethodDefinition.headers.forEach(builder::header);

    /* return the populated builder */
    return builder;
  }

  /**
   * Creates a new {@link TargetMethodDefinition}.
   *
   * @param target for this definition.
   */
  private TargetMethodDefinition(Target<?> target,
      String name,
      String tag,
      TypeDefinition returnType,
      UriTemplate template,
      HttpMethod method,
      Collection<HttpHeader> headers,
      Map<Integer, TargetMethodParameterDefinition> parameterMap,
      Integer bodyArgumentIndex,
      boolean followRedirects,
      long connectTimeout,
      long readTimeout) {
    Assert.isNotNull(target, "target is required.");
    this.target = target;
    this.name = name;
    this.tag = tag;
    this.returnType = returnType;
    this.template = template;
    this.method = method;
    this.bodyArgumentIndex = bodyArgumentIndex;
    this.followRedirects = followRedirects;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;

    if (headers == null) {
      this.headers = Collections.emptyList();
    } else {
      this.headers = headers.stream().map(
          header -> new HttpHeader(header.name(), header.values()))
          .collect(Collectors.toUnmodifiableList());
    }

    if (parameterMap == null) {
      this.parameterMap = Collections.emptyMap();
    } else {
      this.parameterMap = parameterMap.entrySet()
          .stream()
          .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
    }
  }


  /**
   * Name of the Method on the Target this configuration is based.
   *
   * @return method name.
   */
  public String getName() {
    return name;
  }

  /**
   * Generic Return Type specified on the Method.
   *
   * @return return Type.
   */
  public TypeDefinition getReturnType() {
    return returnType;
  }

  /**
   * {@link TargetMethodParameterDefinition} registered at the specified method argument index.
   *
   * @param argumentIndex of the parameter.
   * @return the {@link TargetMethodParameterDefinition} registered, if one exists.
   */
  public Optional<TargetMethodParameterDefinition> getParameterDefinition(Integer argumentIndex) {
    return Optional.ofNullable(parameterMap.get(argumentIndex));
  }

  /**
   * Unique Tag for this Method Configuration.
   *
   * @return a {@code javadoc.SeeTag} version of the method signature.
   */
  public String getTag() {
    return this.tag;
  }

  /**
   * Target Type Class name.
   *
   * @return the simple name of the Target Class.
   */
  public String getTargetType() {
    return this.target.type().getName();
  }

  /**
   * Argument Index registered for the value that should be used as the HttpRequest body.
   *
   * @return the request body argument index.
   */
  public Integer getBody() {
    return this.bodyArgumentIndex;
  }

  /**
   * The Uri for this Method.
   *
   * @return the template uri.
   */
  public String getUri() {
    return (this.template != null) ? this.template.toString() : "";
  }

  /**
   * The Headers registered.
   *
   * @return headers registered.
   */
  public Collection<HttpHeader> getHeaders() {
    return Collections.unmodifiableCollection(this.headers);
  }

  /**
   * The {@link TargetMethodParameterDefinition}s registered for this method.
   *
   * @return the parameters registered.
   */
  public Collection<TargetMethodParameterDefinition> getParameterDefinitions() {
    return Collections.unmodifiableCollection(this.parameterMap.values());
  }

  /**
   * The Http Method registered for this method call.
   *
   * @return the http method.
   */
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * If requests made should follow 3xx responses automatically.
   *
   * @return if this request should follow redirects.
   */
  public boolean isFollowRedirects() {
    return followRedirects;
  }

  /**
   * How long to wait when connecting to the target.
   *
   * @return connection timeout in milliseconds.
   */
  public long getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * How long to wait for data when reading from the target.
   *
   * @return read timeout in milliseconds.
   */
  public long getReadTimeout() {
    return readTimeout;
  }


  /**
   * Creates a {@link RequestSpecification} using the method configuration and the variables
   * provided.
   *
   * @param variables to use when expanding the method's {@link UriTemplate}.
   * @return a {@link RequestSpecification} instance with the expanded URI, headers, and body.
   */
  public RequestSpecification requestSpecification(Map<TemplateParameter, ?> variables) {
    RequestSpecification requestSpecification = new RequestSpecification();
    requestSpecification.uri(this.template.expand(variables))
        .method(this.method)
        .connectTimeout(this.connectTimeout, TimeUnit.MILLISECONDS)
        .readTimeout(this.readTimeout, TimeUnit.MILLISECONDS)
        .followRedirects(this.followRedirects);
    if (!this.headers.isEmpty()) {
      for (HttpHeader header : headers) {
        header.values().forEach(value -> requestSpecification.header(header.name(), value));
      }
    }

    /* target the request */
    this.target.apply(requestSpecification);
    return requestSpecification;
  }

  /**
   * Determines if this Method Metadata contains no information.
   *
   * @return {@literal true} when no metadata present, {@literal false} otherwise.
   */
  public boolean isEmpty() {
    return this.template == null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TargetMethodDefinition)) {
      return false;
    }
    TargetMethodDefinition that = (TargetMethodDefinition) obj;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TargetMethodDefinition.class.getSimpleName() + " [", "]")
        .add("target=" + target)
        .add("name='" + name + "'")
        .add("tag='" + tag + "'")
        .add("returnType=" + returnType)
        .add("template=" + template)
        .add("method=" + method)
        .add("followRedirects=" + followRedirects)
        .add("connectTimeout=" + connectTimeout)
        .add("readTimeout=" + readTimeout)
        .toString();
  }

  /**
   * Builder for a Target Method Definition.
   */
  public static class Builder {

    private final Target<?> target;
    private String name;
    private String tag;
    private transient TypeDefinition returnType;
    private UriTemplate template;
    private HttpMethod method = HttpMethod.GET;
    private final Collection<HttpHeader> headers = new CopyOnWriteArraySet<>();
    private final Map<Integer, TargetMethodParameterDefinition> parameterMap =
        new ConcurrentHashMap<>();
    private Integer bodyArgumentIndex = -1;
    private boolean followRedirects;
    private long connectTimeout = RequestOptions.DEFAULT_CONNECT_TIMEOUT;
    private long readTimeout = RequestOptions.DEFAULT_READ_TIMEOUT;
    private final TypeDefinitionFactory typeDefinitionFactory = TypeDefinitionFactory.getInstance();

    Builder(Target<?> target) {
      this.target = target;
    }

    /**
     * Name of the Method.
     *
     * @param name method name.
     * @return the reference chain.
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Tag for the Method.
     *
     * @param tag method tag.
     * @return the reference chain.
     */
    public Builder tag(String tag) {
      this.tag = tag;
      return this;
    }

    /**
     * Method Return Type.
     *
     * @param returnType of the method.
     * @return the reference chain.
     */
    public Builder returnType(Type returnType) {
      this.returnType = this.typeDefinitionFactory.create(returnType, this.target.type());
      return this;
    }

    /**
     * HttpRequest connection timeout, in milliseconds.
     *
     * @param connectTimeout in milliseconds.
     * @return the reference chain.
     */
    public Builder connectTimeout(long connectTimeout) {
      this.connectTimeout =
          (connectTimeout > 0) ? connectTimeout : RequestOptions.DEFAULT_CONNECT_TIMEOUT;
      return this;
    }

    /**
     * Response Read Timeout, in milliseconds.
     *
     * @param readTimeout in milliseconds.
     * @return the reference chain.
     */
    public Builder readTimeout(long readTimeout) {
      this.readTimeout = (readTimeout > 0) ? readTimeout : RequestOptions.DEFAULT_READ_TIMEOUT;
      return this;
    }

    /**
     * HttpRequest URI for the Method.
     *
     * @param uri from the method.
     * @return the reference chain.
     */
    public Builder uri(String uri) {
      Assert.isNotNull(uri, "uri is required");
      if (this.template != null) {
        /* if the provided uri is absolute, we need to replace it, encode it so we can parse it */
        String encodedUri = uri.replaceAll("\\{", "%7B")
            .replaceAll("}", "%7D");
        if (!URI.create(encodedUri).isAbsolute()) {
          /* append the new uri */
          uri = this.template.toString() + uri;
        }
      }
      this.template = UriTemplate.create(uri);
      return this;
    }

    /**
     * Registers a {@link TargetMethodParameterDefinition} at the method argument index.
     *
     * @param argumentIndex in the method signature.
     * @param definition to register.
     * @return the reference chain.
     */
    public Builder parameterDefinition(
        Integer argumentIndex, TargetMethodParameterDefinition definition) {
      this.parameterMap.put(argumentIndex, definition);
      return this;
    }

    /**
     * Registers a {@link HttpHeader} for the method.
     *
     * @param header to register.
     * @return the reference chain.
     */
    public Builder header(HttpHeader header) {
      Assert.isNotNull(header, "header is required.");
      this.headers.add(header);
      return this;
    }

    /**
     * Http Method for the HttpRequest defined.
     *
     * @param httpMethod for the request.
     * @return the reference chain.
     */
    public Builder method(HttpMethod httpMethod) {
      this.method = httpMethod;
      return this;
    }

    /**
     * Registers which argument in the method signature containing the HttpRequest body.
     *
     * @param argumentIndex for the HttpRequest body object.
     * @return the reference chain.
     */
    public Builder body(Integer argumentIndex) {
      this.bodyArgumentIndex = (argumentIndex > 0) ? argumentIndex : -1;
      return this;
    }

    /**
     * Determines if this request should automatically follow 3xx responses.
     *
     * @param followRedirects flag.
     * @return the reference chain.
     */
    public Builder followRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    /**
     * Creates a new Target Method Definition.
     *
     * @return a new instance.
     */
    public TargetMethodDefinition build() {
      return new TargetMethodDefinition(target, name, tag, returnType, template, method, headers,
          parameterMap, bodyArgumentIndex, followRedirects, connectTimeout, readTimeout);
    }
  }
}
