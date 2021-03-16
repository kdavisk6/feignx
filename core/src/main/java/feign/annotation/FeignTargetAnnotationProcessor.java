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

import feign.Contract;
import feign.FeignTarget;
import feign.impl.type.TypeDefinitionFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

/**
 * Annotation Processor for {@link feign.FeignTarget} annotated interfaces.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class FeignTargetAnnotationProcessor extends AbstractProcessor {

  private Types types;
  private Elements elements;
  private Messager messager;
  private final TypeDefinitionFactory typeDefinitionFactory = TypeDefinitionFactory.getInstance();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.types = processingEnv.getTypeUtils();
    this.elements = processingEnv.getElementUtils();
    this.messager = processingEnv.getMessager();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      roundEnv.getElementsAnnotatedWith(FeignTarget.class)
          .stream()
          .filter(element -> ElementKind.INTERFACE == element.getKind())
          .forEach(element -> {
            TypeElement typeElement = (TypeElement) element;
            final String target = typeElement.getQualifiedName().toString();

            FeignTarget feignTarget = element.getAnnotation(FeignTarget.class);
            AnnotatedTargetDefinition metadata = this.annotatedTargetMetadata(feignTarget, target);

            /* obtain a contract instance for the rest of this process */
            AnnotatedContract contract = this.getContract(metadata);

            /* check the target element for top level information */
            AnnotatedTargetMethodDefinition.Builder methodMetadata = AnnotatedTargetMethodDefinition
                .builder();

            this.processAnnotationOnClass(typeElement, contract, methodMetadata);

            typeElement.getEnclosedElements()
                .stream()
                .filter(executableElement -> ElementKind.METHOD == executableElement.getKind())
                .map(ExecutableElement.class::cast)
                .forEach(method -> {
                  this.processAnnotationOnMethod(method, contract, methodMetadata);

                  List<? extends VariableElement> parameters = method.getParameters();
                  if (!parameters.isEmpty()) {
                    for (int i = 0; i < parameters.size(); i++) {
                      VariableElement parameter = parameters.get(i);
                      this.processAnnotationOnParameter(parameter, i, contract, methodMetadata);
                    }
                  }
                });
          });
      return true;
    } catch (Exception ex) {
      this.messager.printMessage(Kind.ERROR, ex.getMessage());
    }
    return false;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(FeignTarget.class.getTypeName());
  }

  private void processAnnotationOnClass(TypeElement typeElement, AnnotatedContract contract,
      AnnotatedTargetMethodDefinition.Builder builder) {
    Collection<Class<? extends Annotation>> classAnnotations = contract
        .getSupportedClassAnnotations();
    if (!classAnnotations.isEmpty()) {
      classAnnotations.forEach(annotation -> {
        Annotation ann = typeElement.getAnnotation(annotation);
        if (ann != null) {
          System.out.println(ann);
        }
      });
    }
  }

  private void processAnnotationOnMethod(ExecutableElement element, AnnotatedContract contract,
      AnnotatedTargetMethodDefinition.Builder builder) {

    builder.name(element.getSimpleName().toString())
        .returnTypeClassName(element.getReturnType().toString());

    Collection<Class<? extends Annotation>> classAnnotations = contract
        .getSupportedMethodAnnotations();
    if (!classAnnotations.isEmpty()) {
      classAnnotations.forEach(annotation -> {
        Annotation ann = element.getAnnotation(annotation);
        if (ann != null) {
          System.out.println(ann);
        }
      });
    }
  }

  private void processAnnotationOnParameter(VariableElement element, Integer index,
      AnnotatedContract contract, AnnotatedTargetMethodDefinition.Builder builder) {

    Collection<Class<? extends Annotation>> classAnnotations = contract
        .getSupportedParameterAnnotations();
    if (!classAnnotations.isEmpty()) {
      classAnnotations.forEach(annotation -> {
        Annotation ann = element.getAnnotation(annotation);
        if (ann != null) {
          System.out.println(ann);
        }
      });
    }
  }

  private AnnotatedTargetDefinition annotatedTargetMetadata(FeignTarget feignTarget,
      String targetType) {
    AnnotatedTargetDefinition.Builder builder =
        AnnotatedTargetDefinition.builder(feignTarget.value(), targetType, feignTarget.uri());

    /* process each class reference one by one, dealing with type mirrors as we go */
    try {
      builder.client(this.getQualifiedClassName(feignTarget.client()));
    } catch (MirroredTypeException mte) {
      builder.client(this.getQualifiedNameFromException(mte));
    }

    try {
      builder.contract(this.getQualifiedClassName(feignTarget.contract()));
    } catch (MirroredTypeException mte) {
      builder.contract(this.getQualifiedNameFromException(mte));
    }

    try {
      builder.exceptionHandler(this.getQualifiedClassName(feignTarget.exceptionHandler()));
    } catch (MirroredTypeException mte) {
      builder.exceptionHandler(this.getQualifiedNameFromException(mte));
    }

    try {
      builder.requestEncoder(this.getQualifiedClassName(feignTarget.encoder()));
    } catch (MirroredTypeException mte) {
      builder.requestEncoder(this.getQualifiedNameFromException(mte));
    }

    try {
      builder.responseDecoder(this.getQualifiedClassName(feignTarget.decoder()));
    } catch (MirroredTypeException mte) {
      builder.responseDecoder(this.getQualifiedNameFromException(mte));
    }

    try {
      builder.logger(this.getQualifiedClassName(feignTarget.logger()));
    } catch (MirroredTypeException mte) {
      builder.logger(this.getQualifiedNameFromException(mte));
    }

    return builder.build();
  }

  private String getQualifiedNameFromException(MirroredTypeException mte) {
    DeclaredType typeMirror = (DeclaredType) mte.getTypeMirror();
    TypeElement typeElement = (TypeElement) typeMirror.asElement();
    return typeElement.getQualifiedName().toString();
  }

  private <T> String getQualifiedClassName(Class<T> type) {
    return type.getCanonicalName();
  }

  private AnnotatedContract getContract(AnnotatedTargetDefinition metadata) {
    /* obtain a contract reference for the remainder of this process */
    String contractClassName = metadata.getContractClassName();
    AnnotatedContract contract;
    try {
      Class<?> contractClass = Class.forName(contractClassName);
      contract = (AnnotatedContract) contractClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException cnfe) {
      throw new IllegalStateException(
          "Contract Class " + contractClassName + " cannot be found.  "
              + "Please ensure that it is on the classpath, and not part of the project currently "
              + "being built.  Contracts must be built prior to use.");
    } catch (NoSuchMethodException nsme) {
      throw new IllegalStateException("Contract Class " + contractClassName
          + " does not have a default, no argument constructor.");
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException ie) {
      throw new IllegalStateException(
          "Error occurred during contract initialization.  Contract " + contractClassName
              + " could not be created.", ie);
    }
    return contract;
  }
}
