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

/**
 * Functional Handler for a method found on a given Target.
 */
public interface TargetMethodHandler<T> {

  /**
   * Handle the method execution.
   *
   * @param args to apply.
   * @return the result of the operation.
   * @throws Throwable if the operation failed.
   */
  T execute(Object[] args) throws Throwable;

}
