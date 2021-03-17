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

import feign.annotation.AnnotatedContract;
import feign.contract.FeignContract;
import feign.http.client.UrlConnectionClient;
import feign.logging.SimpleLogger;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a given interface a Feign {@link feign.Target}.  Used by our annotation processor
 * to generate target implementations at compile time.  Provides and entry point for a
 * {@link FeignConfiguration} into the annotation processor.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Documented
@Inherited
public @interface FeignTarget {

  /**
   * Name of the Target.  Default to the annotated class name.
   *
   * @return target name.
   */
  String value() default "";

  /**
   * Encoder to use with this Target.
   *
   * @return RequestEncoder type.
   */
  Class<? extends RequestEncoder> encoder() default RequestEncoder.class;

  /**
   * {@link ResponseDecoder} to use with this Target.
   *
   * @return ResponseDecoder type.
   */
  Class<? extends ResponseDecoder> decoder() default ResponseDecoder.class;

  /**
   * Base URI for this Target.  Must be absolute.
   *
   * @return absolute uri for all requests.
   */
  String uri();

  /**
   * {@link Client} to use.
   *
   * @return Client type.
   */
  Class<? extends Client> client() default UrlConnectionClient.class;

  /**
   * {@link AnnotatedContract} to use.
   *
   * @return AnnotatedContract type.
   */
  Class<? extends AnnotatedContract> contract() default FeignContract.class;

  /**
   * {@link Logger} to use.
   *
   * @return Logger type.
   */
  Class<? extends Logger> logger() default SimpleLogger.class;

  /**
   * {@link ExceptionHandler} to use.
   *
   * @return ExceptionHandler type.
   */
  Class<? extends ExceptionHandler> exceptionHandler() default ExceptionHandler.class;

}
