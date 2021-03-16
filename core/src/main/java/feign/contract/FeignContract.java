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

package feign.contract;

import feign.template.expander.CachingExpanderRegistry;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Contract that uses Feign annotations.
 */
public class FeignContract extends AbstractAnnotationDrivenContract {

  /**
   * Creates a new Feign Contract.
   */
  public FeignContract() {
    super();
    this.registerAnnotationProcessor(Request.class, new RequestAnnotationProcessor());
    this.registerAnnotationProcessor(Headers.class, new HeadersAnnotationProcessor());
    this.registerParameterAnnotationProcessor(Param.class,
        new ParamAnnotationProcessor(new CachingExpanderRegistry()));
    this.registerParameterAnnotationProcessor(Body.class, new BodyAnnotationProcessor());
  }

}
