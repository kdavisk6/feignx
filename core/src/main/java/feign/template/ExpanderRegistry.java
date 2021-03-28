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

/**
 * Factory for creating {@link ExpressionExpander} instances.
 */
public interface ExpanderRegistry {

  /**
   * Retrieves an {@link ExpressionExpander} based on the type.
   *
   * @param type of the value to be expanded.
   * @return an {@link ExpressionExpander} instance.
   * @throws IllegalStateException if the expander instance could not be created.
   */
  ExpressionExpander getExpanderByType(Class<?> type);

  /**
   * Retrieves an {@link ExpressionExpander} instance from the expander type provided.
   *
   * @param expanderClass to retrieve.
   * @return an {@link ExpressionExpander} instance of the type provided.
   * @throws IllegalStateException if the expander instance could be created.
   */
  ExpressionExpander getExpander(Class<? extends ExpressionExpander> expanderClass,
      String typeClassName);
}
