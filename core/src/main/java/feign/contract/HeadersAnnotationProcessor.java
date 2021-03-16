package feign.contract;

import feign.TargetMethodDefinition.Builder;

public class HeadersAnnotationProcessor implements AnnotationProcessor<Headers> {

  private final HeaderAnnotationProcessor headerAnnotationProcessor =
      new HeaderAnnotationProcessor();

  @Override
  public void process(Headers annotation, Builder builder) {
    if (annotation.value().length != 0) {
      Header[] header = annotation.value();
      for (Header value : header) {
        headerAnnotationProcessor.process(value, builder);
      }
    }
  }
}
