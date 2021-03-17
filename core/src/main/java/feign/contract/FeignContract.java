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

import feign.annotation.AnnotatedContract;
import feign.template.expander.CachingExpanderRegistry;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

/**
 * Contract that uses Feign annotations.
 */
public class FeignContract extends AbstractAnnotationDrivenContract implements AnnotatedContract {

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

  @Override
  public Collection<Class<? extends Annotation>> getSupportedClassAnnotations() {
    return Set.of(Request.class, Headers.class);
  }

  @Override
  public Collection<Class<? extends Annotation>> getSupportedMethodAnnotations() {
    return Set.of(Request.class, Headers.class);
  }

  @Override
  public Collection<Class<? extends Annotation>> getSupportedParameterAnnotations() {
    return Set.of(Param.class, Body.class);
  }

  @Override
  public <T extends Annotation> AnnotationProcessor<Annotation> getAnnotationProcessor(
      T annotation) {
    return this.annotationProcessors.get(annotation.annotationType());
  }

  @Override
  public <T extends Annotation> ParameterAnnotationProcessor<Annotation>
      getParameterAnnotationProcessor(T annotation) {
    return this.parameterAnnotationProcessors.get(annotation.annotationType());
  }
}
