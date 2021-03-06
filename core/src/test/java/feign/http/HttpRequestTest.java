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

package feign.http;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestEntity;
import feign.encoder.StringRequestEntity;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HttpRequestTest {

  @Test
  void create_withDefaultOptions() {
    HttpRequest request = new HttpRequest(
        URI.create("https://example.com"),
        HttpMethod.GET,
        Collections.emptyList(),
        null,
        null);
    assertThat(request.options()).isNotNull();
  }

  @Test
  void contentLength_isZero_ifNoBody() {
    HttpRequest request = new HttpRequest(
        URI.create("https://example.com"),
        HttpMethod.GET,
        Collections.emptyList(),
        null,
        null);
    assertThat(request.contentLength()).isZero();
    assertThat(request.content()).isNullOrEmpty();

    /* ensure that the content related headers are absent */
    assertThat(request.headers()).filteredOn(
        header -> "Content-Type".equalsIgnoreCase(header.name()))
        .isEmpty();

    assertThat(request.headers()).filteredOn(
        header -> "Content-Length".equalsIgnoreCase(header.name()))
        .isEmpty();
  }

  @Test
  void request_toString() {
    HttpRequest request = new HttpRequest(
        URI.create("https://example.com"),
        HttpMethod.GET,
        Collections.emptyList(),
        null,
        null);
    assertThat(request.toString()).isNotEmpty();
  }

  @Test
  void contentType_isSet_WhenEntityPresent() {
    HttpRequest request = new HttpRequest(
        URI.create("https://api.example.com"),
        HttpMethod.POST,
        Collections.emptyList(),
        null,
        new StringRequestEntity("content"));
    assertThat(request.content()).isNotNull();
    assertThat(request.contentLength()).isNotZero();
    assertThat(request.contentType()).isEqualToIgnoringCase("text/plain");
    assertThat(request.headers()).filteredOn(
        header -> "Content-Type".equalsIgnoreCase(header.name()))
        .filteredOnAssertions(header -> assertThat(header.values())
            .asString()
            .contains("charset"))
        .isNotEmpty();

    assertThat(request.headers()).filteredOn(
        header -> "Content-Length".equalsIgnoreCase(header.name()))
        .isNotEmpty();
  }

  @Test
  void charset_isIgnored_whenNotProvided() {
    HttpRequest request = new HttpRequest(
        URI.create("https://api.example.com"),
        HttpMethod.POST,
        Collections.emptyList(),
        null,
        new RequestEntity() {
          @Override
          public Optional<Charset> getCharset() {
            return Optional.empty();
          }

          @Override
          public int getContentLength() {
            return "content".getBytes().length;
          }

          @Override
          public String getContentType() {
            return "text/plain";
          }

          @Override
          public byte[] getData() {
            return "content".getBytes();
          }
        });
    assertThat(request.headers())
        .filteredOnAssertions(header -> assertThat(header.values())
            .asString()
            .contains("charset"))
        .isEmpty();
  }
}