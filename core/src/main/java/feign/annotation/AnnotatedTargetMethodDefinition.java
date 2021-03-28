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

package feign.annotation;

import feign.TargetMethodParameterDefinition;
import feign.http.HttpHeader;
import feign.http.HttpHeaders;
import feign.http.HttpMethod;
import feign.template.TemplateParameter;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArraySet;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
@Immutable
public class AnnotatedTargetMethodDefinition {

  private final String name;
  private final String uri;
  private final HttpMethod method;
  private final String returnTypeClassName;
  private final boolean followRedirects;
  private final long requestTimeout;
  private final long connectTimeout;
  private final Integer bodyIndex;
  private final HttpHeaders headers;
  private final Set<TargetMethodParameterDefinition> parameters;

  public static Builder builder() {
    return new Builder();
  }

  private AnnotatedTargetMethodDefinition(String name, String uri, HttpMethod method,
      String returnTypeClassName, boolean followRedirects, long requestTimeout, long connectTimeout,
      Integer bodyIndex, HttpHeaders headers,
      Set<TargetMethodParameterDefinition> parameters) {
    this.name = name;
    this.uri = uri;
    this.method = method;
    this.returnTypeClassName = returnTypeClassName;
    this.followRedirects = followRedirects;
    this.requestTimeout = requestTimeout;
    this.connectTimeout = connectTimeout;
    this.bodyIndex = bodyIndex;
    this.headers = headers;
    this.parameters = Set.copyOf(parameters);
  }

  public String getUri() {
    return uri;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getReturnTypeClassName() {
    return returnTypeClassName;
  }

  public boolean isFollowRedirects() {
    return followRedirects;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  public long getConnectTimeout() {
    return connectTimeout;
  }

  public Integer getBodyIndex() {
    return bodyIndex;
  }

  public HttpHeaders getHeaders() {
    return headers;
  }

  public Set<TargetMethodParameterDefinition> getParameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ",
        AnnotatedTargetMethodDefinition.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("uri='" + uri + "'")
        .add("method=" + method)
        .add("returnTypeClassName='" + returnTypeClassName + "'")
        .add("followRedirects=" + followRedirects)
        .add("requestTimeout=" + requestTimeout)
        .add("connectTimeout=" + connectTimeout)
        .add("bodyIndex=" + bodyIndex)
        .add("headers=" + headers)
        .add("parameters=" + parameters)
        .toString();
  }

  public static class Builder {

    private String name;
    private String uri;
    private HttpMethod method;
    private String returnTypeClassName;
    private boolean followRedirects;
    private long requestTimeout;
    private long connectTimeout;
    private Integer bodyIndex;
    private final HttpHeaders headers = new HttpHeaders();
    private final Set<TargetMethodParameterDefinition> parameters = new CopyOnWriteArraySet<>();

    public Builder uri(String uri) {
      this.uri = uri;
      return this;
    }

    public Builder method(HttpMethod method) {
      this.method = method;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder returnTypeClassName(String returnTypeClassName) {
      this.returnTypeClassName = returnTypeClassName;
      return this;
    }

    public Builder followRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public Builder requestTimeout(long requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    public Builder connectTimeout(long connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public Builder body(Integer bodyIndex) {
      this.bodyIndex = bodyIndex;
      return this;
    }

    public Builder header(HttpHeader header) {
      this.headers.add(header);
      return this;
    }

    public Builder parameter(TargetMethodParameterDefinition parameter) {
      this.parameters.add(parameter);
      return this;
    }

    /**
     * Create a new {@link AnnotatedTargetMethodDefinition} instance.
     *
     * @return a new instance.
     */
    public AnnotatedTargetMethodDefinition build() {
      return new AnnotatedTargetMethodDefinition(this.name, this.uri, this.method,
          this.returnTypeClassName,
          this.followRedirects, this.requestTimeout, this.connectTimeout, this.bodyIndex,
          this.headers, this.parameters);
    }
  }
}
