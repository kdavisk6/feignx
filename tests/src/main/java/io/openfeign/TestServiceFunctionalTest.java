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
import feign.http.HttpMethod;
import feign.impl.TypeDrivenMethodHandlerFactory;
import feign.impl.UriTarget;
import feign.template.SimpleTemplateParameter;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Functional Test that demonstrates what a static version of a Feign Target could look like.
 */
public class TestServiceFunctionalTest {

  static class TestServiceImpl implements TestService {

    private final TargetMethodHandlerFactory methodHandlerFactory;
    private final FeignConfiguration feignConfiguration;

    public TestServiceImpl() {
      this.methodHandlerFactory = new TypeDrivenMethodHandlerFactory();
      this.feignConfiguration = Feign.builder()
          .uri(URI.create("https://api.github.com"))
          .decoder(new JacksonDecoder())
          .build();
    }

    @Override
    public List<String> getContributors(String owner, String repository) {
      TargetMethodDefinition definition = TargetMethodDefinition.builder(
          new UriTarget<>(TestService.class, this.feignConfiguration.getUri().toString()))
          .method(HttpMethod.GET)
          .uri("/repos/{owner}/{repo}/contributors")
          .name("getContributors")
          .returnType(List.class)
          .tag(this.getMethodTag(
              TestService.class.getName(), "getContributors", "String",
              "String"))
          .templateParameter(0, new SimpleTemplateParameter("owner"))
          .templateParameter(1, new SimpleTemplateParameter("repo"))
          .build();

      TargetMethodHandler<List<String>> targetMethodHandler =
          this.methodHandlerFactory.create(definition, this.feignConfiguration);

      try {
        return targetMethodHandler.execute(new Object[]{owner, repository});
      } catch (Throwable th) {
        throw new RuntimeException(th);
      }
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

  public static void main(String[] args) {
    TestService testService = new TestServiceImpl();
    List<String> contributors = testService.getContributors("openfeign", "feignx");
    System.out.println(contributors);
  }
}
