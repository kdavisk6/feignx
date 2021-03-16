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
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Configuration Definition.
 */
public interface FeignConfiguration {

  /**
   * Client instance to use to execute requests.
   *
   * @return a client instance.
   */
  Client getClient();

  /**
   * Request Encoder to apply to requests.
   *
   * @return a request encoder instance.
   */
  RequestEncoder getRequestEncoder();

  /**
   * Response Decoder to apply to responses.
   *
   * @return a response decoder instance.
   */
  ResponseDecoder getResponseDecoder();

  /**
   * Contract to apply to Targets.
   *
   * @return a contract instance.
   */
  Contract getContract();

  /**
   * Executor to use when executing requests on a Client.
   *
   * @return an executor instance.
   */
  Executor getExecutor();

  /**
   * Interceptors to apply to requests.
   *
   * @return a list of interceptor instances.
   */
  List<RequestInterceptor> getRequestInterceptors();

  /**
   * Exception Handler to apply in case of an exception.
   *
   * @return an exception handler instance.
   */
  ExceptionHandler getExceptionHandler();

  /**
   * Target instance for this configuration.
   *
   * @param <T> type of the Target.
   * @return the Target instance.
   */
  <T> Target<T> getTarget();

  /**
   * Logger to use when logging request and responses.
   *
   * @return the log instance.
   */
  Logger getLogger();

  /**
   * Retry to use when processing requests.
   *
   * @return the retry instance.
   */
  Retry getRetry();

  /**
   * URI for all requests.
   *
   * @return the base URI.
   */
  URI getUri();
}
