package feign.contract;

import feign.TargetMethodDefinition;
import java.lang.annotation.Annotation;

public interface AnnotationProcessor<T extends Annotation> {

  void process(T annotation, TargetMethodDefinition.Builder builder);

}
