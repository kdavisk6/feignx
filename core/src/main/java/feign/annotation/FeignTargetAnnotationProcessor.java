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

import feign.FeignTarget;
import feign.impl.type.TypeDefinition;
import feign.impl.type.TypeDefinitionFactory;
import java.lang.reflect.Type;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Annotation Processor for {@link feign.FeignTarget} annotated interfaces.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class FeignTargetAnnotationProcessor extends AbstractProcessor {

  private Types types;
  private Elements elements;
  private final TypeDefinitionFactory typeDefinitionFactory = TypeDefinitionFactory.getInstance();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.types = processingEnv.getTypeUtils();
    this.elements = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    roundEnv.getElementsAnnotatedWith(FeignTarget.class)
        .stream()
        .filter(element -> ElementKind.INTERFACE == element.getKind())
        .forEach(element -> {
          TypeElement typeElement = (TypeElement) element;
          final String target = typeElement.getQualifiedName().toString();
          System.out.println(target);
          element.getEnclosedElements()
              .stream()
              .filter(method -> ElementKind.METHOD == method.getKind())
              .map(ExecutableElement.class::cast)
              .forEach(method -> {
                final String name = method.getSimpleName().toString();
                final TypeMirror returnType = method.getReturnType();

              });

        });
    return false;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(FeignTarget.class.getTypeName());
  }
}
