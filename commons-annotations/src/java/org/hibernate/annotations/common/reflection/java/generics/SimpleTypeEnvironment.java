package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;

/**
 * Binds formal type arguments (typically T, E, etc.) to actual types.
 * 
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
class SimpleTypeEnvironment extends HashMap<Type, Type> implements TypeEnvironment {

	private static final long serialVersionUID = 1L;
    
    private final TypeSwitch<Type> substitute = new TypeSwitch<Type>() {
		@Override
		public Type caseClass(Class classType) {
			return classType;
		}

		@Override
		public Type caseGenericArrayType(GenericArrayType genericArrayType) {
			Type originalComponentType = genericArrayType.getGenericComponentType();
			Type boundComponentType = bind( originalComponentType );
			// try to keep the original type if possible
			if ( originalComponentType == boundComponentType ) {
				return genericArrayType;
			}
			return TypeFactory.createArrayType( boundComponentType );
		}

		@Override
		public Type caseParameterizedType(ParameterizedType parameterizedType) {
			Type[] originalArguments = parameterizedType.getActualTypeArguments();
			Type[] boundArguments = substitute( originalArguments );
			// try to keep the original type if possible
			if ( areSame( originalArguments, boundArguments ) ) {
				return parameterizedType;
			}
			return TypeFactory.createParameterizedType(
					parameterizedType.getRawType(), boundArguments, parameterizedType.getOwnerType()
			);
		}

		private boolean areSame(Object[] array1, Object[] array2) {
			if ( array1.length != array2.length ) {
				return false;
			}
			for ( int i = 0; i < array1.length ; i++ ) {
				if ( array1[i] != array2[i] ) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Type caseTypeVariable(TypeVariable typeVariable) {
            if ( !containsKey( typeVariable )) {
            	return typeVariable;
            }
            return get( typeVariable );
		}

		@Override
		public Type caseWildcardType(WildcardType wildcardType) {
			return wildcardType;
		}
	};

	public SimpleTypeEnvironment(Type[] formalTypeArgs, Type[] actualTypeArgs) {
        for (int i = 0; i < formalTypeArgs.length; i++) {
            put( formalTypeArgs[i], actualTypeArgs[i] );
        }
	}

	public Type bind(Type type) {
		return substitute.doSwitch( type );
	}

	private Type[] substitute(Type[] types) {
		Type[] substTypes = new Type[types.length];
		for ( int i = 0; i < substTypes.length ; i++ ) {
			substTypes[i] = bind( types[i] );
		}
		return substTypes;
	}
}
