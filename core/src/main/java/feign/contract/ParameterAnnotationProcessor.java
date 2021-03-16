package feign.contract;

import feign.TargetMethodDefinition;
import java.lang.annotation.Annotation;

public interface ParameterAnnotationProcessor<T extends Annotation> {

  void process(T annotation, Integer index, String type, TargetMethodDefinition.Builder builder);

}
