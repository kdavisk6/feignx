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

package feign.impl.type;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

/**
 * Type Definition for a Parameterized Type where one of the Type Variable's defined is a {@code ?}
 * wildcard.
 */
public class WildCardTypeDefinition extends AbstractTypeDefinition implements WildcardType {

  private final List<TypeDefinition> upperBounds;
  private final List<TypeDefinition> lowerBounds;

  /**
   * Creates a new WildcardType definition.
   */
  WildCardTypeDefinition() {
    super();
    this.upperBounds = new ArrayList<>();
    this.lowerBounds = new ArrayList<>();
  }

  @Override
  public Class<?> getType() {
    /* only one upper or lower bound is allowed, so first default to the lower bound */
    if (!this.lowerBounds.isEmpty()) {
      return this.lowerBounds.get(0).getType();
    } else if (!this.upperBounds.isEmpty()) {
      return this.upperBounds.get(0).getType();
    }
    return null;
  }

  @Override
  public Type[] getUpperBounds() {
    return this.upperBounds.toArray(new Type[]{});
  }

  @Override
  public Type[] getLowerBounds() {
    return this.lowerBounds.toArray(new Type[]{});
  }

  void addUpperBound(TypeDefinition typeDefinition) {
    this.upperBounds.add(typeDefinition);
  }

  void addLowerBound(TypeDefinition typeDefinition) {
    this.lowerBounds.add(typeDefinition);
  }
}
