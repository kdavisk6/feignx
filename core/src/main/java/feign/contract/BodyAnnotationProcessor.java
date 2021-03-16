package feign.contract;

import feign.TargetMethodDefinition.Builder;

public class BodyAnnotationProcessor implements ParameterAnnotationProcessor<Body> {

  @Override
  public void process(Body annotation, Integer index, String type, Builder builder) {
    builder.body(index);
  }
}
