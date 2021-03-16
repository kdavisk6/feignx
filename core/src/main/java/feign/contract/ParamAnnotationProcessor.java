package feign.contract;

import feign.TargetMethodDefinition.Builder;
import feign.template.ExpanderRegistry;
import feign.template.ExpressionExpander;
import feign.template.SimpleTemplateParameter;
import feign.template.expander.DefaultExpander;

public class ParamAnnotationProcessor implements ParameterAnnotationProcessor<Param> {

  private final ExpanderRegistry expanderRegistry;

  public ParamAnnotationProcessor(ExpanderRegistry expanderRegistry) {
    this.expanderRegistry = expanderRegistry;
  }


  @Override
  public void process(Param annotation, Integer index, String type, Builder builder) {
    String name = annotation.value();
    Class<? extends ExpressionExpander> expanderClass = annotation.expander();

    /* inspect the type annotated */
    ExpressionExpander expander = null;
    if (this.isCustomExpander(expanderClass)) {
      /* retrieve an instance of the custom expander */
      expander = this.expanderRegistry.getExpander(expanderClass);
    } else {
      /* retrieve the expander from the registry by the parameter type */
      expander = this.expanderRegistry.getExpanderByTypeName(type);
    }

    builder.templateParameter(
        index, new SimpleTemplateParameter(name, type, expander));
  }

  private boolean isCustomExpander(Class<? extends ExpressionExpander> expanderClass) {
    return DefaultExpander.class != expanderClass;
  }
}
