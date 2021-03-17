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

import feign.Target;
import feign.http.RequestSpecification;

public class AnnotatedTarget<T> implements Target<T> {

  AnnotatedTarget(String name) {

  }

  @Override
  public Class<T> type() {
    return null;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public void apply(RequestSpecification requestSpecification) {

  }
}
