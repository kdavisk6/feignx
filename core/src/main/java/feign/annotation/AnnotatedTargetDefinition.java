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

import feign.support.StringUtils;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Target Metadata obtained during annotation processing.  Used to generate the FeignTarget
 * implementations.
 */
@ThreadSafe
@Immutable
public class AnnotatedTargetDefinition {

  private final String targetPackage;
  private final String targetName;
  private final String targetType;
  private final String targetUri;
  private final String clientClassName;
  private final String contractClassName;
  private final String loggerClassName;
  private final String exceptionHandlerClassName;
  private final String requestEncoderClassName;
  private final String responseDecoderClassName;
  private final List<AnnotatedTargetMethodDefinition> methodDefinitions =
      new CopyOnWriteArrayList<>();

  public static Builder builder(String targetPackage, String targetName, String targetType,
      String targetUri) {
    return new Builder(targetPackage, targetName, targetType, targetUri);
  }

  private AnnotatedTargetDefinition(String targetName, String targetType, String targetPackage,
      String targetUri, String clientClassName, String contractClassName, String loggerClassName,
      String exceptionHandlerClassName, String requestEncoderClassName,
      String responseDecoderClassName, List<AnnotatedTargetMethodDefinition> methodDefinitions) {
    this.targetPackage = targetPackage;
    this.targetName = (!StringUtils.isEmpty(targetName)) ? targetName : targetType;
    this.targetType = targetType;
    this.targetUri = targetUri;
    this.clientClassName = clientClassName;
    this.contractClassName = contractClassName;
    this.loggerClassName = loggerClassName;
    this.exceptionHandlerClassName = exceptionHandlerClassName;
    this.requestEncoderClassName = requestEncoderClassName;
    this.responseDecoderClassName = responseDecoderClassName;
    this.methodDefinitions.addAll(methodDefinitions);
  }

  public String getTargetPackage() {
    return this.targetPackage;
  }

  public String getTargetName() {
    return targetName;
  }

  public String getTargetType() {
    return targetType;
  }

  public String getTargetUri() {
    return targetUri;
  }

  public String getClientClassName() {
    return clientClassName;
  }

  public String getContractClassName() {
    return contractClassName;
  }

  public String getLoggerClassName() {
    return loggerClassName;
  }

  public String getExceptionHandlerClassName() {
    return exceptionHandlerClassName;
  }

  public String getRequestEncoderClassName() {
    return requestEncoderClassName;
  }

  public String getResponseDecoderClassName() {
    return responseDecoderClassName;
  }

  public List<AnnotatedTargetMethodDefinition> getMethodDefinitions() {
    return List.copyOf(this.methodDefinitions);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", AnnotatedTargetDefinition.class.getSimpleName() + "[",
        "]")
        .add("targetPackage='" + targetPackage + "'")
        .add("targetName='" + targetName + "'")
        .add("targetType='" + targetType + "'")
        .add("targetUri='" + targetUri + "'")
        .add("clientClassName='" + clientClassName + "'")
        .add("contractClassName='" + contractClassName + "'")
        .add("loggerClassName='" + loggerClassName + "'")
        .add("exceptionHandlerClassName='" + exceptionHandlerClassName + "'")
        .add("requestEncoderClassName='" + requestEncoderClassName + "'")
        .add("responseDecoderClassName='" + responseDecoderClassName + "'")
        .add("methodDefinitions=" + methodDefinitions)
        .toString();
  }

  /**
   * Builder for Annotated Target Metadata.
   */
  public static class Builder {

    private final String targetPackage;
    private final String targetName;
    private final String targetType;
    private final String targetUri;
    private String clientClassName;
    private String contractClassName;
    private String loggerClassName;
    private String exceptionHandlerClassName;
    private String requestEncoderClassName;
    private String responseDecoderClassName;
    private final List<AnnotatedTargetMethodDefinition> methodDefinitions =
        new CopyOnWriteArrayList<>();

    Builder(String targetPackage, String targetName, String targetType, String targetUri) {
      this.targetPackage = targetPackage;
      this.targetName = targetName;
      this.targetType = targetType;
      this.targetUri = targetUri;
    }

    public Builder client(String clientClassName) {
      this.clientClassName = clientClassName;
      return this;
    }

    public Builder contract(String contractClassName) {
      this.contractClassName = contractClassName;
      return this;
    }

    public Builder logger(String loggerClassName) {
      this.loggerClassName = loggerClassName;
      return this;
    }

    public Builder exceptionHandler(String exceptionHandlerClassName) {
      this.exceptionHandlerClassName = exceptionHandlerClassName;
      return this;
    }

    public Builder requestEncoder(String requestEncoderClassName) {
      this.requestEncoderClassName = requestEncoderClassName;
      return this;
    }

    public Builder responseDecoder(String responseDecoderClassName) {
      this.responseDecoderClassName = responseDecoderClassName;
      return this;
    }

    public Builder methodDefinition(AnnotatedTargetMethodDefinition methodDefinition) {
      this.methodDefinitions.add(methodDefinition);
      return this;
    }

    public String getContractClassName() {
      return this.contractClassName;
    }

    /**
     * Creates a new {@link AnnotatedTargetDefinition} instance.
     *
     * @return an {@link AnnotatedTargetDefinition} instance.
     */
    public AnnotatedTargetDefinition build() {
      return new AnnotatedTargetDefinition(this.targetName, this.targetType, this.targetPackage,
          this.targetUri, this.clientClassName, this.contractClassName, this.loggerClassName,
          this.exceptionHandlerClassName, this.requestEncoderClassName,
          this.responseDecoderClassName, this.methodDefinitions);
    }
  }

}
