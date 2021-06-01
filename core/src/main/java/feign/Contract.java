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

package feign;

import feign.contract.TargetDefinition;

/**
 * Represents the agreement between the specific implementation and the user, enforcing how
 * each Target method can be defined.
 */
public interface Contract {

  /**
   * Create a new {@link TargetDefinition} from the {@link FeignConfiguration} provided.
   *
   * @param targetType with the Target configuration.
   * @return a new {@link TargetDefinition} instance.
   */
  TargetDefinition apply(Class<?> targetType, FeignConfiguration configuration);

}
