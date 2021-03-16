package feign.contract;

import feign.TargetMethodDefinition;
import feign.http.HttpMethod;
import feign.support.StringUtils;

public class RequestAnnotationProcessor implements AnnotationProcessor<Request> {

  @Override
  public void process(Request annotation, TargetMethodDefinition.Builder builder) {
    String uri = (StringUtils.isNotEmpty(annotation.uri())) ? annotation.uri() : annotation.value();
    HttpMethod httpMethod = annotation.method();
    boolean followRedirects = annotation.followRedirects();

    builder.uri(uri)
        .method(httpMethod)
        .followRedirects(followRedirects)
        .connectTimeout(annotation.connectTimeout())
        .readTimeout(annotation.readTimeout());
  }

}
