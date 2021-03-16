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

package feign.template;

import feign.support.Assert;
import feign.template.expander.SimpleExpander;
import java.util.Objects;

/**
 * Template Parameter implementation that acts as a simple value object.
 */
public class SimpleTemplateParameter implements TemplateParameter {

  private final String name;
  private String type;
  private ExpressionExpander expander = new SimpleExpander();

  /**
   * Creates a new {@link SimpleTemplateParameter}.
   *
   * @param name of the parameter.
   * @throws IllegalArgumentException if the name is {@literal null} or empty.
   */
  public SimpleTemplateParameter(String name) {
    Assert.isNotEmpty(name, "name is required");
    this.name = name;
  }

  /**
   * Creates a new {@link SimpleTemplateParameter}.
   *
   * @param name of the parameter.
   * @param expander to use when expanding the expression contained.
   */
  public SimpleTemplateParameter(String name, ExpressionExpander expander) {
    this(name);
    Assert.isNotNull(expander, "expander is required.");
    this.expander = expander;
  }

  /**
   * Creates a new {@link SimpleTemplateParameter}.
   *
   * @param name of the parameter.
   * @param type fully qualified type name.
   */
  public SimpleTemplateParameter(String name, String type) {
    this(name);
    Assert.isNotEmpty(type, "type is required.");
    this.type = type;
  }

  /**
   * Creates a new {@link SimpleTemplateParameter}.
   *
   * @param name of the parameter.
   * @param type fully qualified type name of the parameter.
   * @param expander to use when expandeing the expression contained.
   */
  public SimpleTemplateParameter(String name, String type, ExpressionExpander expander) {
    this(name, expander);
    Assert.isNotEmpty(type, "type is required.");
    this.type = type;
  }

  /**
   * The name of the parameter.
   *
   * @return parameter name.
   */
  @Override
  public String name() {
    return this.name;
  }

  /**
   * {@link ExpressionExpander} instance to use when expanding this parameter.
   *
   * @return the expander to use during expansion.
   */
  @Override
  public ExpressionExpander expander() {
    return this.expander;
  }

  /**
   * Fully Qualified Type name of the parameter.
   *
   * @return the type name;
   */
  @Override
  public String type() {
    return this.type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SimpleTemplateParameter)) {
      return false;
    }
    SimpleTemplateParameter that = (SimpleTemplateParameter) obj;
    return name.toLowerCase().equals(that.name.toLowerCase());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(name.toLowerCase());
  }
}
