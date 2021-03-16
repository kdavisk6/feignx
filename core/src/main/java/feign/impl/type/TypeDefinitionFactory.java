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

package feign.impl.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

/**
 * Factory responsible for inspecting a given Generic {@link Type} and parsing the type variables
 * into their actual class references, resulting in a new {@link TypeDefinition}.
 */
public class TypeDefinitionFactory {

  private static final TypeDefinitionFactory instance = new TypeDefinitionFactory();

  public static TypeDefinitionFactory getInstance() {
    return instance;
  }

  /**
   * Creates a {@link TypeDefinition} for the {@link Type} provided.  If the Type is not a {@link
   * Class} reference, such as a {@link java.lang.reflect.ParameterizedType}, or {@link
   * java.lang.reflect.GenericArrayType}, the concrete definition will be resolved first.
   *
   * @param type to define.
   * @param context to use to limit the scope of any type definitions, can be {@literal null}
   * @return a new {@link TypeDefinition} instance.
   */
  public TypeDefinition create(Type type, Class<?> context) {
    if (type instanceof Class<?>) {
      return new ClassTypeDefinition((Class<?>) type);
    } else if (type instanceof ParameterizedType) {
      return this.define((ParameterizedType) type, context);
    } else if (type instanceof GenericArrayType) {
      return this.define((GenericArrayType) type, context);
    } else if (type instanceof WildcardType) {
      return this.define((WildcardType) type, context);
    } else if (type instanceof TypeVariable) {
      return this.define((TypeVariable) type, context);
    }
    throw new IllegalArgumentException("Type " + type.getTypeName() + " is not supported.");
  }

  public TypeDefinition create(TypeMirror typeMirror) {
    throw new IllegalArgumentException("Type " + typeMirror.getClass() + " is not supported.");
  }

  /**
   * Defines a {@link ParameterizedType}, attempting to define any {@link TypeVariable}s used as
   * Type arguments.
   *
   * @param parameterizedType to define.
   * @param context for this definition.
   * @return a new Type Definition instance for the type.
   */
  private TypeDefinition define(ParameterizedType parameterizedType, Class<?> context) {
    /* the owner of this type may also contain the definition for this type, we will need to
     * define the owner as well.
     */
    Type owner = parameterizedType.getOwnerType();
    if (owner != null) {
      owner = this.create(parameterizedType.getOwnerType(), context);
    }

    /* define the type arguments */
    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    List<TypeDefinition> typeDefinitions = new ArrayList<>();
    for (Type argument : typeArguments) {
      TypeDefinition argumentDefinition = this.create(argument, context);
      if (argumentDefinition != null) {
        typeDefinitions.add(argumentDefinition);
      }
    }
    return new ParameterizedTypeDefinition(
        owner, parameterizedType.getRawType(),
        typeDefinitions.toArray(new TypeDefinition[]{}));
  }

  /**
   * Defines a {@link GenericArrayType}, attempting to define any {@link TypeVariable}s used.
   *
   * @param genericArrayType to define.
   * @param context for this definition.
   * @return a new Type Definition instance.
   */
  private TypeDefinition define(GenericArrayType genericArrayType, Class<?> context) {
    /* like parameterized types, the parameter values here may not be defined at this level
     * so we need to walk the hierarchy before continuing.
     */
    Type componentType = genericArrayType.getGenericComponentType();
    return new GenericArrayTypeDefinition(this.create(componentType, context));
  }

  /**
   * Defines a {@link WildcardType}, attempting to resolve any Wildcard Expressions.
   *
   * @param wildcardType to define.
   * @param context for this definition.
   * @return a new Type Definition instance.
   */
  private TypeDefinition define(WildcardType wildcardType, Class<?> context) {
    WildCardTypeDefinition typeDefinition = new WildCardTypeDefinition();

    /* wild card types can define an 'upper' and 'lower' bounds, we will need to define each
     * bound.
     */
    Type[] upperBounds = wildcardType.getUpperBounds();
    Type[] lowerBounds = wildcardType.getLowerBounds();
    for (Type type : upperBounds) {
      TypeDefinition definition = this.create(type, context);
      if (definition != null) {
        typeDefinition.addUpperBound(definition);
      }
    }
    for (Type type : lowerBounds) {
      TypeDefinition definition = this.create(type, context);
      if (definition != null) {
        typeDefinition.addLowerBound(definition);
      }
    }

    return typeDefinition;
  }

  /**
   * Defines a {@link TypeVariable}, attempting to resolve the concrete class from the provided
   * context.
   *
   * @param typeVariable to resolve.
   * @return the TypeDefinition for the Type Variable.
   */
  private TypeDefinition define(TypeVariable typeVariable, Class<?> context) {
    /* determine where this type definition has been declared */
    Class<?> declared =
        (typeVariable.getGenericDeclaration() instanceof Class)
            ? (Class<?>) typeVariable.getGenericDeclaration() : null;
    if (declared == null) {
      return null;
    }

    /* walk the type hierarchy to locate the super type where the type variable has been defined,
     * if any.
     */
    Type superType = this.getGenericSuperType(declared, context);
    if (superType instanceof ParameterizedType) {
      /* the super type is a parameterized type, determine which one of the
       * type arguments this type variable is for. */
      ParameterizedType parameterizedSuperType = (ParameterizedType) superType;
      for (int i = 0; i < declared.getTypeParameters().length; i++) {
        TypeVariable typeParameter = declared.getTypeParameters()[i];
        if (typeParameter.equals(typeVariable)) {
          /* resolve this type */
          return this.create(parameterizedSuperType.getActualTypeArguments()[i], context);
        }
      }
    }

    /* the type is a direct reference, define it. */
    return this.create(declared, context);
  }

  /**
   * Determines the Super Type for the provided type, within the current context.
   *
   * @param type to inspect.
   * @param context defining the current Class this search should be scoped to.
   * @return the Generic Super Type of the provided type.
   */
  private Type getGenericSuperType(Class<?> type, Class<?> context) {
    /* type already matches, no need to go any further */
    if (type == context) {
      return type;
    }

    if (type.isInterface()) {
      /* walk the interface hierarchy to get to the root interface */
      Class<?>[] interfaces = context.getInterfaces();
      for (int i = 0; i < interfaces.length; i++) {
        Class<?> interfaceType = interfaces[i];
        if (interfaceType == type) {
          /* we've reached the top of the hierarchy */
          return context.getGenericInterfaces()[i];
        } else if (type.isAssignableFrom(interfaceType)) {
          /* still have more levels to go */
          return this.getGenericSuperType(type, interfaceType);
        }
      }
    }

    if (!context.isInterface()) {
      /* concrete class, watch the class hierarchy */
      while (context != Object.class) {
        /* walk the class hierarchy to get the root definition */
        Class<?> superClass = context.getSuperclass();
        if (superClass == type) {
          /* we've reached the level where the type is defined */
          return context.getGenericSuperclass();
        } else if (type.isAssignableFrom(superClass)) {
          /* keep going */
          return this.getGenericSuperType(type, superClass);
        }
        context = superClass;
      }
    }

    /* we cannot go any further */
    return type;
  }

}
