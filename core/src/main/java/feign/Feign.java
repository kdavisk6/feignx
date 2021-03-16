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

import feign.ExceptionHandler.RethrowExceptionHandler;
import feign.contract.FeignContract;
import feign.decoder.StringDecoder;
import feign.encoder.StringEncoder;
import feign.http.client.UrlConnectionClient;
import feign.impl.AbstractFeignConfigurationBuilder;
import feign.impl.BaseFeignConfiguration;
import feign.impl.UriTarget;
import feign.logging.SimpleLogger;
import feign.proxy.ProxyFeign;
import feign.retry.NoRetry;

/**
 * Feign instance builder.  Provides access to a {@link FeignConfigurationBuilder}, using a
 * fluent-api to allow user's to configure each component of a {@link FeignConfiguration}
 * that will be used when executing methods on the {@link Target}.
 */
public abstract class Feign {

  /**
   * Returns a new {@link FeignConfigurationBuilder}.
   *
   * @return FeignConfigurationBuilder instance.
   */
  public static FeignConfigurationBuilderImpl builder() {
    return new FeignConfigurationBuilderImpl();
  }

  /**
   * Creates a new instance of the {@link Target} type in the configuration.  Implementations are
   * expected to be thread-safe.
   *
   * @param configuration containing the dependencies the resulting target should use.
   * @param <T> of the Target.
   * @return a new Target instance.
   */
  protected abstract <T> T create(FeignConfiguration configuration);

  /**
   * Default {@link FeignConfigurationBuilder}.  Provides access to the core Feign components.
   */
  public static class FeignConfigurationBuilderImpl extends
      AbstractFeignConfigurationBuilder<FeignConfigurationBuilderImpl, FeignConfiguration> {

    /**
     * Creates a new Builder, defining the default components.
     */
    FeignConfigurationBuilderImpl() {
      super(FeignConfigurationBuilderImpl.class);

      /* use the Feign Contract annotations. */
      this.contract = new FeignContract();

      /* assume all request and response items are strings */
      this.encoder = new StringEncoder();
      this.decoder = new StringDecoder();

      /* use the java.net client */
      this.client = new UrlConnectionClient();

      /* don't handle exceptions, throw them */
      this.exceptionHandler = new RethrowExceptionHandler();

      /* execute on the same thread */
      this.executor = Runnable::run;

      /* default log configuration */
      this.logger = SimpleLogger.builder().build();

      /* default retry */
      this.retry = new NoRetry();
    }

    /**
     * Build the Feign Configuration.
     *
     * @return a new {@link FeignConfiguration} instance.
     */
    @Override
    public FeignConfiguration build() {
      return new BaseFeignConfiguration(
          this.target, this.contract, this.encoder, this.interceptors, this.client, this.decoder,
          this.exceptionHandler, this.executor, this.logger, this.retry, this.uri);
    }

    /**
     * Creates a new JDK Proxy backed Target instance.
     *
     * @param targetType containing the Target definition.
     * @param uri for all requests in the target to be sent to.  Must be absolute.
     * @param <T> type of the Target instance.
     * @return a new Target instance.
     */
    public <T> T target(Class<T> targetType, String uri) {
      this.target(new UriTarget<>(targetType, uri));

      /* create a new ProxyFeign instance */
      Feign feign = new ProxyFeign();
      return feign.create(this.build());
    }
  }
}
