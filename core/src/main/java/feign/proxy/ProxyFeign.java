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

package feign.proxy;

import feign.Contract;
import feign.Feign;
import feign.FeignConfiguration;
import feign.contract.TargetDefinition;
import java.lang.reflect.Proxy;

/**
 * Feign implementation that creates a JDK Proxy for the {@link feign.Target} instance.
 */
public class ProxyFeign extends Feign {

  /**
   * Creates a JDK Proxy for the {@link feign.Target} provided in the configuration.
   *
   * @param configuration for this instance.
   * @param <T> of the {@link feign.Target} class to proxy.
   * @return a JDK Proxy instance.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected <T> T create(Class<?> targetType, FeignConfiguration configuration) {
    /* apply the contract to the target */
    Contract contract = configuration.getContract();
    TargetDefinition definition = contract.apply(targetType, configuration);

    /* create the provided target in a proxy */
    ProxyTarget<T> proxyTarget = new ProxyTarget<>(definition, configuration);

    /* create a new JDK Proxy for the Target */
    return (T) Proxy.newProxyInstance(
        proxyTarget.type().getClassLoader(),
        new Class<?>[] {proxyTarget.type()},
        proxyTarget);
  }
}
