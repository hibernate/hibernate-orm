package org.hibernate.annotations.common.reflection.java.generics;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;

/**
 * @author Paolo Perrotta
 */
public class TypeUtils {

	public static boolean isResolved(Type t) {
		return new TypeSwitch<Boolean>() {
			@Override
			public Boolean caseClass(Class classType) {
				return true;
			}

			@Override
			public Boolean caseGenericArrayType(GenericArrayType genericArrayType) {
				return isResolved( genericArrayType.getGenericComponentType() );
			}

			@Override
			public Boolean caseParameterizedType(ParameterizedType parameterizedType) {
				Type[] typeArgs = parameterizedType.getActualTypeArguments();
				for ( Type arg : typeArgs ) {
					if ( !isResolved( arg ) ) {
						return false;
					}
				}
				return isResolved( parameterizedType.getRawType() );
			}

			@Override
			public Boolean caseTypeVariable(TypeVariable typeVariable) {
				return false;
			}

			@Override
			public Boolean caseWildcardType(WildcardType wildcardType) {
				return areResolved( wildcardType.getUpperBounds() ) && areResolved( wildcardType.getLowerBounds() );
			}
		}.doSwitch( t );
	}

	private static Boolean areResolved(Type[] types) {
		for ( Type t : types ) {
			if ( !isResolved( t ) ) {
				return false;
			}
		}
		return true;
	}

	public static Class<? extends Collection> getCollectionClass(Type type) {
		return new TypeSwitch<Class<? extends Collection>>() {
			@Override
			public Class<? extends Collection> caseClass(Class clazz) {
				return isCollectionClass( clazz ) ? (Class<? extends Collection>) clazz : null;
			}

			@Override
			public Class<? extends Collection> caseParameterizedType(ParameterizedType parameterizedType) {
				return getCollectionClass( parameterizedType.getRawType() );
			}

			@Override
			public Class<? extends Collection> caseWildcardType(WildcardType wildcardType) {
				Type[] upperBounds = wildcardType.getUpperBounds();
				if ( upperBounds.length == 0 ) {
					return null;
				}
				return getCollectionClass( upperBounds[0] );
			}

			@Override
			public Class<? extends Collection> defaultCase(Type t) {
				return null;
			}
		}.doSwitch( type );
	}

	private static boolean isCollectionClass(Class<?> clazz) {
		return clazz == Collection.class
				|| clazz == java.util.List.class
				|| clazz == java.util.Set.class
				|| clazz == java.util.Map.class
				|| clazz == java.util.SortedSet.class // extension to the specs
				|| clazz == java.util.SortedMap.class; // extension to the specs
	}

	public static boolean isSimple(Type type) {
		return new TypeSwitch<Boolean>() {
			@Override
			public Boolean caseClass(Class clazz) {
				return !clazz.isArray() && !isCollectionClass( clazz ); // probably not fully accurate
			}

			@Override
			public Boolean caseParameterizedType(ParameterizedType parameterizedType) {
				return isSimple( parameterizedType.getRawType() );
			}

			@Override
			public Boolean caseWildcardType(WildcardType wildcardType) {
				return areSimple( wildcardType.getUpperBounds() ) && areSimple( wildcardType.getLowerBounds() );
			}

			@Override
			public Boolean defaultCase(Type t) {
				return false;
			}
		}.doSwitch( type );
	}

	private static Boolean areSimple(Type[] types) {
		for ( Type t : types ) {
			if ( !isSimple( t ) ) {
				return false;
			}
		}
		return true;
	}

	public static boolean isVoid(Type type) {
		return void.class.equals( type );
	}

	public static boolean isArray(Type t) {
		return new TypeSwitch<Boolean>() {
			@Override
			public Boolean caseClass(Class clazz) {
				return clazz.isArray();
			}

			@Override
			public Boolean caseGenericArrayType(GenericArrayType genericArrayType) {
				return isSimple( genericArrayType.getGenericComponentType() );
			}

			@Override
			public Boolean defaultCase(Type type) {
				return false;
			}
		}.doSwitch( t );
	}

	public static boolean isCollection(Type t) {
		return getCollectionClass( t ) != null;
	}
}
