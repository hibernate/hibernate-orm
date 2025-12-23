/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import org.hibernate.models.spi.MemberDetails;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author Gavin King
 */
public final class GenericsHelper {

	/**
	 * The type of the member inherited by the subclass from the supertype,
	 * as viewed from within the subclass.
	 * @param memberDetails The member, represented in the subclass
	 * @return The type of the member as it would be seen in the subclass
	 */
	public static Type actualMemberType(MemberDetails memberDetails) {
		return actualInheritedMemberType(
				memberDetails.getDeclaringType().toJavaClass(),
				memberDetails.toJavaMember()
		);
	}

	/**
	 * The type of the member inherited by the subclass from the supertype,
	 * as viewed from within the subclass.
	 * @param subclass The inheriting subclass
	 * @param superMember The member declared in the supertype
	 * @return The type of the member as it would be seen in the subclass
	 */
	public static Type actualInheritedMemberType(Class<?> subclass, Member superMember) {
		return substituteTypeVariables( getMemberType( superMember ),
				collectTypeArguments( subclass, superMember ) );
	}

	private static Map<TypeVariable<?>, Type> collectTypeArguments(
			Class<?> subclass, Member superMember) {
		final var superclass = superMember.getDeclaringClass();
		final var typeArguments = typeArguments( superclass, subclass );
		final var typeParameters = superclass.getTypeParameters();
		final Map<TypeVariable<?>, Type> typeMap =
				new HashMap<>( typeParameters.length );
		for ( int i = 0; i < typeParameters.length; i++ ) {
			typeMap.put( typeParameters[i], typeArguments[i] );
		}
		return typeMap;
	}

	private static Type getMemberType(Member superMember) {
		if ( superMember instanceof Field field ) {
			return field.getGenericType();
		}
		else if ( superMember instanceof Method method ) {
			return method.getGenericReturnType();
		}
		else {
			throw new IllegalArgumentException( "Unsupported member: " + superMember );
		}
	}

	private static Type substituteTypeVariables(Type type, Map<TypeVariable<?>, Type> typeMap) {

		if ( type instanceof TypeVariable<?> typeVariable ) {
			final var substituted = typeMap.get( typeVariable );
			return substituted == null
					? Object.class
					: substituteTypeVariables( substituted, typeMap );
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			final var args = parameterizedType.getActualTypeArguments();
			final var resolved = new Type[args.length];
			for ( int i = 0; i < args.length; i++ ) {
				resolved[i] = substituteTypeVariables( args[i], typeMap );
			}
			return new SimpleParameterizedType(
					(Class<?>) parameterizedType.getRawType(),
					resolved,
					parameterizedType.getOwnerType()
			);
		}
		else if ( type instanceof GenericArrayType genericArrayType ) {
			final var elementType =
					substituteTypeVariables( genericArrayType.getGenericComponentType(), typeMap );
			return new GenericArrayType() {
				@Override
				public Type getGenericComponentType() {
					return elementType;
				}

				@Override
				public String toString() {
					return elementType.getTypeName() + "[]";
				}
			};
		}
		else {
			return type;
		}
	}

	/**
	 * The erased type of the given type.
	 * @param type A type, possibly with type arguments
	 * @return The erased type
	 */
	public static Class<?> erasedType(Type type) {
		if ( type instanceof Class<?> clazz ) {
			return clazz;
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			return erasedType( parameterizedType.getRawType() );
		}
		else if ( type instanceof TypeVariable<?> typeVariable ) {
			return erasedType( typeVariable.getBounds()[0] );
		}
		else if ( type instanceof GenericArrayType genericArrayType ) {
			return genericArrayType.getGenericComponentType() instanceof Class<?> elementClass
					? elementClass.arrayType()
					: Object[].class;
		}
		else {
			throw new IllegalArgumentException( "Cannot erase type: " + type );
		}
	}

	/**
	 * Get the type argument of the instantiation of the given generic
	 * type constructor which is a supertype of the given type expression.
	 * @param genericType A generic type constructor
	 * @param implementingType A type expression
	 * @return The type arguments assigned to parameters of the generic type constructor
	 */
	public static Type[] typeArguments(Class<?> genericType, Type implementingType) {
		if ( genericType.getTypeParameters().length == 0 ) {
			return EMPTY_TYPE_ARRAY;
		}
		else {
			final var instantiation =
					supertypeInstantiation( genericType, implementingType );
			if ( instantiation == null ) {
				throw new IllegalArgumentException(
						implementingType.getTypeName()
						+ " is is not a subtype of "
						+ genericType.getName() );
			}
			return instantiation.getActualTypeArguments();
		}
	}

	private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

	/**
	 * A supertype of the given type which is an instantiation of the
	 * given generic type constructor.
	 * @param genericType A generic type constructor
	 * @param base A type which is assignable to some instantiation of
	 *             the given generic type constructor
	 * @return An instantiation of the generic type constructor which
	 *         is a supertype of the given type, or null if none exists
	 */
	public static ParameterizedType supertypeInstantiation(Class<?> genericType, Type base) {
		if ( base == null ) {
			return null;
		}

		final var clazz = erasedType( base );
		if ( clazz == null ) {
			return null;
		}

		if ( clazz == genericType
				&& base instanceof ParameterizedType result ) {
			return result;
		}

		final var superclass = clazz.getGenericSuperclass();
		if ( superclass != null ) {
			final var type = substituteTypeArguments( superclass, base );
			if ( type instanceof ParameterizedType parameterizedType
					&& genericType.equals( parameterizedType.getRawType() ) ) {
				return parameterizedType;
			}

			final var parameterizedType =
					supertypeInstantiation( genericType, type );
			if ( parameterizedType != null ) {
				return parameterizedType;
			}
		}

		for ( var iface : clazz.getGenericInterfaces() ) {
			final var type = substituteTypeArguments( iface, base );
			if ( type instanceof ParameterizedType parameterizedType
					&& genericType.equals( parameterizedType.getRawType() ) ) {
				return parameterizedType;
			}

			final var parameterizedType =
					supertypeInstantiation( genericType, type );
			if ( parameterizedType != null ) {
				return parameterizedType;
			}
		}

		return null;
	}

	private static Type replaceTypeVariableWithArgument(
			TypeVariable<?> typeVariable, ParameterizedType context) {
		final var clazz = erasedType( context.getRawType() );
		if ( clazz == null ) {
			return null;
		}

		final var typeArguments = context.getActualTypeArguments();
		final var typeParameters = clazz.getTypeParameters();
		for ( int idx = 0; idx < typeParameters.length; idx++ ) {
			if ( typeVariable.getName().equals( typeParameters[idx].getName() ) ) {
				return substituteTypeArguments( typeArguments[idx], context );
			}
		}

		return typeVariable;
	}

	private static Type substituteTypeArguments(Type target, Type context) {
		if ( target instanceof ParameterizedType parameterizedType ) {
			return replaceTypeVariablesWithArguments( parameterizedType, context );
		}
		else if ( target instanceof TypeVariable<?> typeVariable
					&& context instanceof ParameterizedType parameterizedContext ) {
			return replaceTypeVariableWithArgument( typeVariable, parameterizedContext );
		}
		else {
			return target;
		}
	}

	private static ParameterizedType replaceTypeVariablesWithArguments(
			ParameterizedType parameterizedType, Type context) {
		final var typeArguments = parameterizedType.getActualTypeArguments();
		final var resolvedTypeArguments = new Type[typeArguments.length];
		for ( int idx = 0; idx < typeArguments.length; idx++ ) {
			resolvedTypeArguments[idx] = substituteTypeArguments( typeArguments[idx], context );
		}
		return new SimpleParameterizedType(
				erasedType( parameterizedType ),
				resolvedTypeArguments,
				parameterizedType.getOwnerType()
		);
	}

	private record SimpleParameterizedType(Class<?> raw, Type[] args, Type owner)
			implements ParameterizedType {
		@Override
		public Type[] getActualTypeArguments() {
			return args;
		}

		@Override
		public Type getRawType() {
			return raw;
		}

		@Override
		public Type getOwnerType() {
			return owner;
		}

		@Override
		public String toString() {
			final var joiner = new StringJoiner( ", ", "<", ">" );
			for ( var type : args ) {
				joiner.add( type.getTypeName() );
			}
			return raw.getName() + joiner;
		}
	}
}
