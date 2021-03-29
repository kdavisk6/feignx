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
import feign.TargetMethodDefinition;
import feign.TargetMethodParameterDefinition;
import feign.contract.AnnotationProcessor;
import feign.contract.ParameterAnnotationProcessor;
import feign.http.HttpHeader;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
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

  private Messager messager;
  private Elements elements;
  private Types types;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
    this.elements = processingEnv.getElementUtils();
    this.types = processingEnv.getTypeUtils();
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
            AnnotatedTargetDefinition.Builder metadata = this
                .annotatedTargetMetadata(feignTarget, target);

            /* obtain a contract instance for the rest of this process */
            AnnotatedContract contract = this.getContract(metadata.getContractClassName());

            /* create our builders */
            TargetMethodDefinition.Builder rootBuilder = TargetMethodDefinition
                .builder(new AnnotatedTarget<>(target));

            /* check the target element for top level information */
            this.processAnnotationOnClass(typeElement, contract, rootBuilder);

            /* lock in our shared method definition */
            TargetMethodDefinition root = rootBuilder.build();

            /* next, deal with any methods and their parameters */
            typeElement.getEnclosedElements()
                .stream()
                .filter(executableElement -> ElementKind.METHOD == executableElement.getKind())
                .map(ExecutableElement.class::cast)
                .forEach(method -> {
                  AnnotatedTargetMethodDefinition.Builder annotationMetadata =
                      AnnotatedTargetMethodDefinition.builder();

                  TargetMethodDefinition.Builder methodMetadata = TargetMethodDefinition.from(root);
                  this.processAnnotationOnMethod(method, contract, methodMetadata,
                      annotationMetadata);

                  List<? extends VariableElement> parameters = method.getParameters();
                  if (!parameters.isEmpty()) {
                    for (int i = 0; i < parameters.size(); i++) {
                      VariableElement parameter = parameters.get(i);
                      this.processAnnotationOnParameter(parameter, i, contract, methodMetadata);
                    }
                  }

                  TargetMethodDefinition definition = methodMetadata.build();
                  annotationMetadata
                      .name(definition.getName())
                      .body(definition.getBody())
                      .connectTimeout(definition.getConnectTimeout())
                      .requestTimeout(definition.getReadTimeout())
                      .followRedirects(definition.isFollowRedirects())
                      .method(definition.getMethod())
                      .uri(definition.getUri());

                  if (!definition.getHeaders().isEmpty()) {
                    for (HttpHeader header : definition.getHeaders()) {
                      annotationMetadata.header(header);
                    }
                  }

                  if (!definition.getParameterDefinitions().isEmpty()) {
                    for (TargetMethodParameterDefinition parameter : definition
                        .getParameterDefinitions()) {
                      annotationMetadata.parameter(parameter);
                    }
                  }
                  metadata.methodDefinition(annotationMetadata.build());
                });
            System.out.println(metadata.build());
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
      TargetMethodDefinition.Builder methodMetadata) {
    this.processAnnotations(typeElement, contract, contract.getSupportedClassAnnotations(),
        methodMetadata);
  }

  private void processAnnotationOnMethod(ExecutableElement element, AnnotatedContract contract,
      TargetMethodDefinition.Builder methodMetadata,
      AnnotatedTargetMethodDefinition.Builder annotationMetadata) {

    methodMetadata.name(element.getSimpleName().toString());
    annotationMetadata.name(element.getSimpleName().toString())
        .returnTypeClassName(element.getReturnType().toString());

    this.processAnnotations(element, contract, contract.getSupportedMethodAnnotations(),
        methodMetadata);

  }

  private void processAnnotations(Element element, AnnotatedContract contract,
      Collection<Class<? extends Annotation>> annotations,
      TargetMethodDefinition.Builder methodMetadata) {

    if (!annotations.isEmpty()) {
      annotations.forEach(annotation -> {
        Annotation ann = element.getAnnotation(annotation);
        if (ann != null) {
          AnnotationProcessor<Annotation> processor = contract.getAnnotationProcessor(ann);
          if (processor != null) {
            processor.process(ann, methodMetadata);
          }
        }
      });
    }
  }

  private void processAnnotationOnParameter(VariableElement element, Integer index,
      AnnotatedContract contract,
      TargetMethodDefinition.Builder methodMetadata) {

    Collection<Class<? extends Annotation>> classAnnotations = contract
        .getSupportedParameterAnnotations();
    if (!classAnnotations.isEmpty()) {
      classAnnotations.forEach(annotation -> {
        Annotation ann = element.getAnnotation(annotation);
        if (ann != null) {
          ParameterAnnotationProcessor<Annotation> processor =
              contract.getParameterAnnotationProcessor(ann);
          if (processor != null) {
            DeclaredType declaredType = (DeclaredType) element.asType();

            processor.process(
                ann, element.getSimpleName().toString(),
                index,
                this.getQualifiedNameFromElement((TypeElement) declaredType.asElement()),
                methodMetadata);
          }
        }
      });
    }
  }

  private AnnotatedTargetDefinition.Builder annotatedTargetMetadata(FeignTarget feignTarget,
      String targetClassName) {
    TypeElement targetElement = this.elements.getTypeElement(targetClassName);
    String targetType = targetElement.getSimpleName().toString();

    PackageElement packageElement = this.elements.getPackageOf(targetElement);
    String targetPackage = "";
    if (!packageElement.isUnnamed()) {
      targetPackage = packageElement.getQualifiedName().toString();
    }

    AnnotatedTargetDefinition.Builder builder =
        AnnotatedTargetDefinition
            .builder(targetPackage, feignTarget.value(), targetType, feignTarget.uri());

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

    return builder;
  }

  private String getQualifiedNameFromException(MirroredTypeException mte) {
    DeclaredType typeMirror = (DeclaredType) mte.getTypeMirror();
    return this.getQualifiedNameFromElement((TypeElement) typeMirror.asElement());
  }

  private String getQualifiedNameFromElement(TypeElement typeElement) {
    return typeElement.getQualifiedName().toString();
  }

  private <T> String getQualifiedClassName(Class<T> type) {
    return type.getCanonicalName();
  }

  private AnnotatedContract getContract(String contractClassName) {
    /* obtain a contract reference for the remainder of this process */
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
