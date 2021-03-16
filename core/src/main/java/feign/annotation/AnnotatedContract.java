package feign.annotation;

import java.lang.annotation.Annotation;
import java.util.Collection;

public interface AnnotatedContract {
  Collection<Class<? extends Annotation>> getSupportedClassAnnotations();

  Collection<Class<? extends Annotation>> getSupportedMethodAnnotations();

  Collection<Class<? extends Annotation>> getSupportedParameterAnnotations();
}
