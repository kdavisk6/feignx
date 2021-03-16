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

package io.openfeign;

import feign.FeignTarget;
import feign.contract.Param;
import feign.contract.Request;
import feign.http.HttpMethod;
import java.util.List;

@FeignTarget
public interface TestService {

  @Request(method = HttpMethod.GET, value = "/repos/{owner}/{repo}/contributors")
  List<String> getContributors(@Param("owner") String owner,
      @Param("repo") String repository);

}
