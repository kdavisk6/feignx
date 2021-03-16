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

import java.net.URI;
import java.util.concurrent.Executor;

/**
 * Configuration Builder Definition for any Feign instance.
 *
 * @param <B> type of Builder to chain.
 * @param <C> type of FeignConfiguration to generate.
 */
public interface FeignConfigurationBuilder<B extends FeignConfigurationBuilder,
    C extends FeignConfiguration> {

  /**
   * Request Encoder to use.
   *
   * @param encoder instance.
   * @return the builder chain.
   */
  B encoder(RequestEncoder encoder);

  /**
   * Response Decoder to use.
   *
   * @param decoder instance.
   * @return the builder chain.
   */
  B decoder(ResponseDecoder decoder);

  /**
   * Request Interceptor to apply.
   *
   * @param interceptor instance.
   * @return the builder chain.
   */
  B interceptor(RequestInterceptor interceptor);

  /**
   * Client to use.
   *
   * @param client instance.
   * @return the builder chain.
   */
  B client(Client client);

  /**
   * Executor to use when executing requests.
   *
   * @param executor instance.
   * @return the builder chain.
   */
  B executor(Executor executor);

  /**
   * Contract to apply to the any Targets.
   *
   * @param contract to apply.
   * @return the builder chain.
   */
  B contract(Contract contract);

  /**
   * Target to build off.
   *
   * @param target instance.
   * @return the builder chain.
   */
  B target(Target<?> target);

  /**
   * Exception Handler to use.
   *
   * @param exceptionHandler instance.
   * @return the builder chain.
   */
  B exceptionHandler(ExceptionHandler exceptionHandler);

  /**
   * Logger instance.
   *
   * @param logger instance to use.
   * @return the builder chain.
   */
  B logger(Logger logger);

  /**
   * Retry instance.
   *
   * @param retry instance to use.
   * @return the builder chain.
   */
  B retry(Retry retry);

  /**
   * URI for all requests.
   *
   * @param uri to base all requests on.
   * @return the builder chain.
   */
  B uri(URI uri);

  /**
   * Build the Configuration.
   *
   * @return a FeignConfiguration instance.
   */
  C build();
}
