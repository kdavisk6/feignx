package feign.contract;

import feign.TargetMethodDefinition.Builder;
import feign.http.HttpHeader;

public class HeaderAnnotationProcessor implements AnnotationProcessor<Header> {

  @Override
  public void process(Header annotation, Builder builder) {
    HttpHeader httpHeader = new HttpHeader(annotation.name());
    httpHeader.value(annotation.value());
    builder.header(httpHeader);
  }
}
