/*
 * Copyright 2019-2020 OpenFeign Contributors
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
import feign.support.StringUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Uri Template based on RFC 6570.
 */
public class UriTemplate {

  private static final String UNDEF = "undef";
  private final String uri;
  private final List<Chunk> chunks = new ArrayList<>();

  /**
   * Creates a new Uri Template.
   *
   * @param uri containing the template.
   * @return a new UriTemplate instance.
   * @throws IllegalArgumentException if the provided uri is not valid.
   */
  public static UriTemplate create(String uri) {
    return new UriTemplate(uri);
  }

  /**
   * Create a new UriTemplate instance.
   *
   * @param uri to parse.
   */
  private UriTemplate(String uri) {
    Assert.isNotEmpty(uri, "A uri is required.");
    this.uri = uri;
    this.parse(uri);
  }

  /**
   * Expand the template.
   *
   * @param variables with possible expression values.
   * @return a valid, expanded URI.
   */
  public URI expand(Map<TemplateParameter, ?> variables) {
    StringBuilder uri = new StringBuilder();
    for (Chunk chunk : chunks) {
      if (Expression.class.isAssignableFrom(chunk.getClass())) {
        /* cast to an expression */
        Expression expression = (Expression) chunk;

        /* create a new String Builder for the expression */
        StringBuilder result = new StringBuilder();
        List<ExpressionVariable> expressionVariables = expression.getVariables();
        for (ExpressionVariable variable : expressionVariables) {
          this.getParameterForExpression(variable, variables)
              .flatMap(templateParameter -> Optional.ofNullable(
                  expandVariable(
                      templateParameter, variable, variables.get(templateParameter))))
              .ifPresent(value -> appendExpressionResult(result, expression, value));
        }

        if (result.length() != 0) {
          uri.append(result);
        }
      } else {
        /* chunk is a literal, append the literal */
        uri.append(chunk.getValue());
      }
    }
    return URI.create(uri.toString());
  }

  private void appendExpressionResult(StringBuilder builder, Expression expression, String result) {
    if (builder.length() == 0) {
      builder.append(expression.getStartSeparator());
    } else {
      builder.append(expression.getSeparator());
    }
    builder.append(result);
  }

  /**
   * Locate the {@link TemplateParameter} for the given {@link ExpressionVariable}.
   *
   * @param variable to evaluate.
   * @param parameters to search.
   * @return the {@link TemplateParameter} for the variable or {@literal null} if not found.
   */
  private Optional<TemplateParameter> getParameterForExpression(
      ExpressionVariable variable, Map<TemplateParameter, ?> parameters) {
    return parameters.keySet().stream()
        .filter(
            templateParameter -> templateParameter.name()
                .equalsIgnoreCase(variable.getName()))
        .findFirst();
  }

  /**
   * Expand an {@link ExpressionVariable}, using the value provided.
   *
   * @param parameter for the ExpressionVariable, contains the expander to use.
   * @param variable to expand.
   * @param value to use when expanding.
   * @return the expanded variable value or {@literal null} if the value is not defined.
   */
  private String expandVariable(
      TemplateParameter parameter, ExpressionVariable variable, Object value) {
    if (value == null) {
      return null;
    }

    /* delegate to the expander */
    ExpressionExpander expander = parameter.expander();
    return expander.expand(variable, value);
  }

  /**
   * The list of Expressions for this template.
   *
   * @return the expression list.
   */
  Collection<Chunk> getExpressions() {
    return this.chunks.stream()
        .filter(chunk -> chunk instanceof Expression)
        .collect(Collectors.toSet());
  }

  /**
   * Parse the URI.
   *
   * @param uri to parse.
   */
  private void parse(String uri) {
    if (StringUtils.isNotEmpty(uri)) {
      ChunkTokenizer tokenizer = new ChunkTokenizer(uri);
      while (tokenizer.hasNext()) {
        String chunk = tokenizer.next();
        if (Expressions.isExpression(chunk)) {
          Expression expression = Expressions.create(chunk);
          this.chunks.add(expression);
        } else {
          this.chunks.add(new Literal(chunk));
        }
      }
    }
  }

  @Override
  public String toString() {
    return this.uri;
  }

  /**
   * Splits a Uri into Chunks that exists inside and outside of an expression, delimited by curly
   * braces "{}". Nested expressions are treated as literals, for example "foo{bar{baz}}" will be
   * treated as "foo, {bar{baz}}". Inspired by Apache CXF Jax-RS.
   */
  static class ChunkTokenizer {

    private List<String> tokens = new ArrayList<>();
    private int index;

    ChunkTokenizer(String template) {
      boolean outside = true;
      int level = 0;
      int lastIndex = 0;
      int idx;

      /* loop through the template, character by character */
      for (idx = 0; idx < template.length(); idx++) {
        if (template.charAt(idx) == '{') {
          /* start of an expression */
          if (outside) {
            /* outside of an expression */
            if (lastIndex < idx) {
              /* this is the start of a new token */
              tokens.add(template.substring(lastIndex, idx));
            }
            lastIndex = idx;

            /*
             * no longer outside of an expression, additional characters will be treated as in an
             * expression
             */
            outside = false;
          } else {
            /* nested braces, increase our nesting level */
            level++;
          }
        } else if (template.charAt(idx) == '}' && !outside) {
          /* the end of an expression */
          if (level > 0) {
            /*
             * sometimes we see nested expressions, we only want the outer most expression
             * boundaries.
             */
            level--;
          } else {
            /* outermost boundary */
            if (lastIndex < idx) {
              /* this is the end of an expression token */
              tokens.add(template.substring(lastIndex, idx + 1));
            }
            lastIndex = idx + 1;

            /* outside an expression */
            outside = true;
          }
        }
      }
      if (lastIndex < idx) {
        /* grab the remaining chunk */
        tokens.add(template.substring(lastIndex, idx));
      }
    }

    boolean hasNext() {
      return this.tokens.size() > this.index;
    }

    String next() {
      if (hasNext()) {
        return this.tokens.get(this.index++);
      }
      throw new IllegalStateException("No More Elements");
    }
  }

}
