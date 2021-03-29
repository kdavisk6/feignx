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

import feign.TargetMethodDefinition.Builder;
import feign.TargetMethodParameterDefinition;
import feign.template.ExpressionExpander;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class ParamAnnotationProcessor implements ParameterAnnotationProcessor<Param> {


  @Override
  public void process(Param annotation, String name, Integer index, String type, Builder builder) {
    String parameter = annotation.value();

    /* get the expander class name, may be in an annotation processor so handle any
     * mirrors.
     */
    String expanderClassName;
    try {
      Class<? extends ExpressionExpander> expanderClass = annotation.expander();
      expanderClassName = expanderClass.getName();
    } catch (MirroredTypeException mirroredTypeException) {
      DeclaredType typeMirror = (DeclaredType) mirroredTypeException.getTypeMirror();
      TypeElement typeElement = (TypeElement) typeMirror.asElement();
      expanderClassName = typeElement.getQualifiedName().toString();
    }

    /* register the parameter definition */
    builder.parameterDefinition(index, TargetMethodParameterDefinition.builder()
        .parameter(parameter)
        .name(name)
        .index(index)
        .type(type)
        .expanderClassName(expanderClassName)
        .build());
  }

}
