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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import feign.Client;
import feign.Contract;
import feign.ExceptionHandler;
import feign.FeignConfiguration;
import feign.Logger;
import feign.RequestEncoder;
import feign.ResponseDecoder;
import feign.Retry;
import feign.Target;
import feign.TargetMethodDefinition;
import feign.TargetMethodHandler;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TypeDrivenMethodHandlerFactoryTest {

  private FeignConfiguration feignConfiguration;

  private TargetMethodDefinition.Builder targetMethodDefinition;

  private TypeDrivenMethodHandlerFactory methodHandlerFactory;

  @BeforeEach
  void setUp() {
    this.feignConfiguration = new BaseFeignConfiguration(
        mock(Target.class),
        mock(Contract.class),
        mock(RequestEncoder.class),
        Collections.emptyList(),
        mock(Client.class),
        mock(ResponseDecoder.class),
        mock(ExceptionHandler.class),
        mock(Executor.class),
        mock(Logger.class),
        mock(Retry.class),
        mock(URI.class));

    this.methodHandlerFactory = new TypeDrivenMethodHandlerFactory();
  }

  @Test
  void createsBlockingHandler_byDefault() {
    this.targetMethodDefinition =
        TargetMethodDefinition.builder(new UriTarget<>(Blog.class, "https://www.example.com"));
    targetMethodDefinition.returnType(String.class);
    TargetMethodHandler targetMethodHandler =
        this.methodHandlerFactory
            .create(this.targetMethodDefinition.build(), this.feignConfiguration);
    assertThat(targetMethodHandler).isInstanceOf(BlockingTargetMethodHandler.class);
  }

  @Test
  void createsAsyncHandler_whenReturnType_isFuture() {
    this.targetMethodDefinition =
        TargetMethodDefinition.builder(new UriTarget<>(Blog.class, "https://www.example.com"));
    targetMethodDefinition.returnType(Future.class);
    TargetMethodHandler targetMethodHandler =
        this.methodHandlerFactory
            .create(this.targetMethodDefinition.build(), this.feignConfiguration);
    assertThat(targetMethodHandler).isInstanceOf(AsyncTargetMethodHandler.class);
  }

  @Test
  void createsAsyncHandler_whenReturnType_isCompletableFuture() {
    this.targetMethodDefinition =
        TargetMethodDefinition.builder(new UriTarget<>(Blog.class, "https://www.example.com"));
    targetMethodDefinition.returnType(CompletableFuture.class);
    TargetMethodHandler targetMethodHandler =
        this.methodHandlerFactory
            .create(this.targetMethodDefinition.build(), this.feignConfiguration);
    assertThat(targetMethodHandler).isInstanceOf(AsyncTargetMethodHandler.class);
  }

  interface Blog {

  }
}