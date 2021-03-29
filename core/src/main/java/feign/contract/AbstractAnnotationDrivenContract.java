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

import feign.Contract;
import feign.Target;
import feign.TargetMethodDefinition;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contract implementation that relies on Annotations to define the {@link TargetMethodDefinition}s
 * for the the methods on a given Target.
 */
public abstract class AbstractAnnotationDrivenContract implements Contract {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractAnnotationDrivenContract.class);

  protected final Map<Class<? extends Annotation>,
      AnnotationProcessor<Annotation>> annotationProcessors = new ConcurrentHashMap<>();

  protected final Map<Class<? extends Annotation>,
      ParameterAnnotationProcessor<Annotation>> parameterAnnotationProcessors =
      new ConcurrentHashMap<>();

  /**
   * Using the Contract annotations, process the Target and create the appropriate {@link
   * TargetMethodDefinition}s.
   *
   * @param target to apply this contract to.
   * @return a Collection of {@link TargetMethodDefinition}s with each methods configuration.
   */
  @Override
  public Collection<TargetMethodDefinition> apply(Target<?> target) {
    Set<TargetMethodDefinition> methods = new LinkedHashSet<>();

    /* special metadata object tha contains the class level configuration that will be
     * used by all methods on this target.
     */
    Class<?> targetType = target.type();
    TargetMethodDefinition.Builder builder = TargetMethodDefinition.builder(target);

    logger.debug("Applying Contract to {}", targetType.getSimpleName());

    this.processAnnotations(targetType.getAnnotations(), builder);

    TargetMethodDefinition root = builder.build();

    for (Method method : targetType.getMethods()) {
      /* create a new metadata object from the root */
      TargetMethodDefinition.Builder methodBuilder = TargetMethodDefinition.from(root);
      methodBuilder.name(method.getName());
      methodBuilder.returnType(method.getGenericReturnType());
      methodBuilder.tag(this.getMethodTag(targetType, method));

      /* process the method */
      this.processAnnotations(method.getAnnotations(), methodBuilder);

      /* process method parameters */
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        Parameter parameter = parameters[i];
        this.processAnnotationsOnParameter(parameter.getAnnotations(),
            parameter.getName(),
            i,
            parameter.getType().getCanonicalName(), methodBuilder);
      }

      /* build the instance */
      TargetMethodDefinition methodMetadata = methodBuilder.build();

      /* determine if implicit body parameter identification is required */
      if (methodMetadata.getBody() == -1
          && parameters.length > methodMetadata.getParameterDefinitions().size()) {
        /* there are parameters on this method that are not registered.  in these cases, we
         * allow users to define which parameter they want as the Request Body without an explicit
         * annotation, look for that parameter and register it.
         */
        logger.debug(
            "No explicit Request Body parameter found on Method: {}, using first parameter without"
                + " an annotation, if present.", method.getName());
        for (int i = 0; i < parameters.length; i++) {
          Parameter parameter = parameters[i];
          if (parameter.getAnnotations().length == 0) {
            /* assume this is our body */
            logger.debug("Marking Parameter {}:{} as the Request Body.",
                parameter.getName(), parameter.getType().getSimpleName());

            /* update the builder and build again */
            methodBuilder.body(i);
            methodMetadata = methodBuilder.build();
            break;
          }
        }
      }

      if (!methodMetadata.isEmpty()) {
        methods.add(methodMetadata);
      }
    }
    logger.debug("Contract parsing completed.  Identified {} methods: [{}]",
        methods.size(), methods);
    return methods;
  }

  private void processAnnotations(Annotation[] annotations,
      TargetMethodDefinition.Builder builder) {
    Arrays.stream(annotations)
        .filter(annotation -> this.annotationProcessors.containsKey(annotation.annotationType()))
        .forEach(annotation -> {
          AnnotationProcessor<Annotation> annotationProcessor = this.annotationProcessors
              .get(annotation.annotationType());
          annotationProcessor.process(annotation, builder);
        });
  }

  private void processAnnotationsOnParameter(Annotation[] annotations, String name, Integer index,
      String type, TargetMethodDefinition.Builder builder) {
    Arrays.stream(annotations)
        .filter(
            annotation -> this.parameterAnnotationProcessors
                .containsKey(annotation.annotationType()))
        .forEach(annotation -> {
          ParameterAnnotationProcessor<Annotation> annotationProcessor =
              this.parameterAnnotationProcessors.get(annotation.annotationType());
          annotationProcessor.process(annotation, name, index, type, builder);
        });
  }

  @SuppressWarnings("unchecked")
  protected <A extends Annotation> void registerAnnotationProcessor(
      Class<A> annotation, AnnotationProcessor<A> processor) {
    this.annotationProcessors
        .computeIfAbsent(annotation, annotationType -> (AnnotationProcessor<Annotation>) processor);
  }

  @SuppressWarnings("unchecked")
  protected <A extends Annotation> void registerParameterAnnotationProcessor(
      Class<A> annotation, ParameterAnnotationProcessor<A> processor) {
    this.parameterAnnotationProcessors
        .computeIfAbsent(annotation,
            annotationType -> (ParameterAnnotationProcessor<Annotation>) processor);
  }

  /**
   * Constructs a name for a Method that is formatted as a See Tag.
   *
   * @param targetType containing the method.
   * @param method to inspect.
   * @return a See Tag inspired name for the method.
   */
  private String getMethodTag(Class<?> targetType, Method method) {
    StringBuilder sb = new StringBuilder()
        .append(targetType.getSimpleName())
        .append("#")
        .append(method.getName())
        .append("(");
    List<Type> parameters = Arrays.asList(method.getGenericParameterTypes());
    Iterator<Type> iterator = parameters.iterator();
    while (iterator.hasNext()) {
      Type parameter = iterator.next();
      sb.append(parameter.getTypeName());
      if (iterator.hasNext()) {
        sb.append(",");
      }
    }
    sb.append(")");
    return sb.toString();
  }
}
