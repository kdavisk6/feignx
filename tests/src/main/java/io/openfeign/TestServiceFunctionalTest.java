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

package io.openfeign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.FeignConfiguration;
import feign.Response;
import feign.TargetMethodDefinition;
import feign.TargetMethodHandler;
import feign.TargetMethodHandlerFactory;
import feign.decoder.AbstractResponseDecoder;
import feign.http.HttpHeader;
import feign.http.HttpMethod;
import feign.impl.TypeDrivenMethodHandlerFactory;
import feign.impl.UriTarget;
import feign.template.SimpleTemplateParameter;
import feign.template.TemplateParameter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Functional Test that demonstrates what a static version of a Feign Target could look like.
 */
public class TestServiceFunctionalTest {

  static class TestServiceImpl implements TestService {

    private final TargetMethodHandlerFactory methodHandlerFactory;
    private final FeignConfiguration feignConfiguration;
    private final Map<String, TargetMethodHandler> methodHandlerMap = new ConcurrentHashMap<>();

    public TestServiceImpl() {
      this.methodHandlerFactory = new TypeDrivenMethodHandlerFactory();
      this.feignConfiguration = Feign.builder()
          .uri(URI.create("https://api.github.com"))
          .decoder(new JacksonDecoder())
          .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getContributors(String owner, String repository) {
      TargetMethodHandler targetMethodHandler =
          this.methodHandlerMap.computeIfAbsent("getContributors",
              methodName -> getTargetMethodHandler(
                  TestService.class,
                  HttpMethod.GET,
                  "/repos/{owner}/{repo}/contributors",
                  methodName, List.class, -1, -1, -1,
                  Collections.emptyList(),
                  List.of(
                      new SimpleTemplateParameter("owner", String.class.getName()),
                      new SimpleTemplateParameter("repo", String.class.getName()))));
      try {
        return (List<String>) targetMethodHandler.execute(new Object[]{owner, repository});
      } catch (Throwable th) {
        throw new RuntimeException(th);
      }
    }

    private TargetMethodHandler getTargetMethodHandler(
        Class<?> type,
        HttpMethod method,
        String uri, String methodName,
        Class<?> returnType,
        long readTimeout,
        long connectTimeout,
        int body,
        List<HttpHeader> headers,
        List<TemplateParameter> parameters) {
      TargetMethodDefinition.Builder builder = TargetMethodDefinition.builder(
          new UriTarget<>(type, this.feignConfiguration.getUri().toString()))
          .method(method)
          .uri(uri)
          .name(methodName)
          .returnType(returnType)
          .readTimeout(readTimeout)
          .body(body)
          .connectTimeout(connectTimeout);

      List<String> parameterTypes = new ArrayList<>();
      for (int i = 0; i < parameters.size(); i++) {
        TemplateParameter templateParameter = parameters.get(i);
        builder.templateParameter(i, templateParameter);
        parameterTypes.add(templateParameter.type());
      }

      builder.tag(this.getMethodTag(
          type.getName(), methodName, parameterTypes.toArray(new String[0])));

      for (HttpHeader header : headers) {
        builder.header(header);
      }

      return this.methodHandlerFactory.create(builder.build(), this.feignConfiguration);
    }

    private String getMethodTag(String targetType, String methodName, String... parameters) {
      StringBuilder sb = new StringBuilder()
          .append(targetType)
          .append("#")
          .append(methodName)
          .append("(");
      Iterator<String> iterator = Arrays.asList(parameters).iterator();
      while (iterator.hasNext()) {
        sb.append(iterator.next());
        if (iterator.hasNext()) {
          sb.append(",");
        }
      }
      sb.append(")");
      return sb.toString();
    }

  }

  public static class JacksonDecoder extends AbstractResponseDecoder {

    private final ObjectMapper objectMapper;

    public JacksonDecoder() {
      this.objectMapper = new ObjectMapper();
    }

    @Override
    protected <T> T decodeInternal(Response response, Class<T> type) {
      try {
        return this.objectMapper.readValue(response.toByteArray(), type);
      } catch (IOException ioe) {
        throw new IllegalStateException("Error occurred reading response", ioe);
      }
    }
  }

  /**
   * Run our Test.
   *
   * @param args to the test.
   */
  public static void main(String[] args) {
    TestService testService = new TestServiceImpl();
    List<String> contributors = testService.getContributors("openfeign", "feignx");
    System.out.println(contributors);
  }
}
