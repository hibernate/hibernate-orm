/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.models.spi.MemberDetails;

import java.lang.reflect.*;
import java.util.*;

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
				extractTypeArguments( subclass, superMember.getDeclaringClass() ) );
	}

	private static @NonNull Type getMemberType(Member superMember) {
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

	private static Map<TypeVariable<?>, Type> extractTypeArguments(
			Class<?> subclass,
			Class<?> targetSuperclass) {
		final Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
		extractTypeArguments( subclass, targetSuperclass, typeMap );
		return typeMap;
	}

	private static boolean extractTypeArguments(
			Class<?> current,
			Class<?> target,
			Map<TypeVariable<?>, Type> typeMap) {

		if ( current == null || current == Object.class ) {
			return false;
		}

		final var genericSuperclass = current.getGenericSuperclass();
		if ( genericSuperclass != null ) {
			final var raw = current.getSuperclass();
			if ( raw != null ) {
				if ( raw == target ) {
					collectTypeArguments( genericSuperclass, raw, typeMap );
					return true;
				}
				if ( extractTypeArguments( raw, target, typeMap ) ) {
					collectTypeArguments( genericSuperclass, raw, typeMap );
					return true;
				}
			}
		}

		for ( var type : current.getGenericInterfaces() ) {
			final var raw = erasedType( type );
			if ( raw != null ) {
				if ( raw == target ) {
					collectTypeArguments( type, raw, typeMap );
					return true;
				}
				if ( extractTypeArguments( raw, target, typeMap ) ) {
					collectTypeArguments( type, raw, typeMap );
					return true;
				}
			}
		}

		return false;
	}

	private static void collectTypeArguments(
			Type type,
			Class<?> raw,
			Map<TypeVariable<?>, Type> typeMap) {
		if ( type instanceof ParameterizedType pt ) {
			final var vars = raw.getTypeParameters();
			final var args = pt.getActualTypeArguments();
			for ( int i = 0; i < vars.length; i++ ) {
				typeMap.put( vars[i], args[i] );
			}
		}
	}

	private static Type substituteTypeVariables(
			Type type,
			Map<TypeVariable<?>, Type> typeMap) {

		if ( type instanceof TypeVariable<?> tv ) {
			final var substituted = typeMap.get( tv );
			return substituted == null
					? Object.class
					: substituteTypeVariables( substituted, typeMap );
		}
		else if ( type instanceof ParameterizedType pt ) {
			final var args = pt.getActualTypeArguments();
			final var resolved = new Type[args.length];
			for ( int i = 0; i < args.length; i++ ) {
				resolved[i] = substituteTypeVariables( args[i], typeMap );
			}
			return new SimpleParameterizedType(
					(Class<?>) pt.getRawType(),
					resolved,
					pt.getOwnerType()
			);
		}
		else if ( type instanceof GenericArrayType gat ) {
			final var elementType =
					substituteTypeVariables( gat.getGenericComponentType(), typeMap );
			return new GenericArrayType() {
				@Override
				@NonNull
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
		if ( type instanceof Class<?> c ) {
			return c;
		}
		else if ( type instanceof ParameterizedType pt ) {
			return erasedType( pt.getRawType() );
		}
		else if ( type instanceof TypeVariable<?> tv ) {
			return erasedType( tv.getBounds()[0] );
		}
		else if ( type instanceof GenericArrayType gat ) {
			return gat.getGenericComponentType() instanceof Class<?> elementClass
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

		final Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
		if ( !collectSupertypeTypeArguments( implementingType, genericType, typeMap ) ) {
			throw new IllegalArgumentException(
					implementingType + " does not implement " + genericType );
		}

		final var vars = genericType.getTypeParameters();
		final var result = new Type[vars.length];
		for ( int i = 0; i < vars.length; i++ ) {
			result[i] = substituteTypeVariables( vars[i], typeMap );
		}
		return result;
	}

	private static boolean collectSupertypeTypeArguments(
			Type current,
			Class<?> targetInterface,
			Map<TypeVariable<?>, Type> typeMap) {

		if ( current == null ) {
			return false;
		}

		final var raw = erasedType( current );
		if ( raw == null || raw == Object.class ) {
			return false;
		}

		// If current is parameterized, record its bindings
		collectTypeArguments( current, raw, typeMap );

		if ( raw == targetInterface ) {
			return true;
		}

		// Check directly implemented interfaces
		for ( var iface : raw.getGenericInterfaces() ) {
			final var ifaceRaw = erasedType( iface );
			if ( ifaceRaw != null ) {
				if ( ifaceRaw == targetInterface ) {
					collectTypeArguments( iface, ifaceRaw, typeMap );
					return true;
				}

				if ( targetInterface.isAssignableFrom( ifaceRaw ) ) {
					if ( collectSupertypeTypeArguments( iface, targetInterface, typeMap ) ) {
						collectTypeArguments( iface, ifaceRaw, typeMap );
						return true;
					}
				}
			}

		}

		// Check superclass
		final var superclass = raw.getGenericSuperclass();
		if ( superclass != null ) {
			final var classRaw = erasedType( superclass );
			if ( classRaw != null ) {
				if ( classRaw == targetInterface ) {
					collectTypeArguments( superclass, classRaw, typeMap );
					return true;
				}

				if ( collectSupertypeTypeArguments( superclass, targetInterface, typeMap ) ) {
					collectTypeArguments( superclass, erasedType( superclass ), typeMap );
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * A supertype of the given type which is an instantiation of the
	 * given generic type constructor.
	 * @param genericType A generic type constructor
	 * @param base A type which is assignable to some instantiation of
	 *             the given generic type constructor
	 * @return An instantiation of the generic type constructor which
	 *         is a supertype of the given type, or null if none exists
	 */
	// currently unused, but let's not delete this one
	// this might be more efficient than typeArguments(),
	// assuming that it's actually correct
	public static ParameterizedType supertypeInstantiation(Class<?> genericType, Type base) {
		if ( base == null ) {
			return null;
		}

		final var clazz = erasedType( base );
		if ( clazz == null ) {
			return null;
		}

		final var superclass = clazz.getGenericSuperclass();
		if ( superclass != null ) {
			var type = superclass;
			type = resolveType( type, base );
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

		for ( var type : clazz.getGenericInterfaces() ) {
			type = resolveType( type, base );
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

	private static Type resolveTypeVariable(TypeVariable<?> typeVariable, ParameterizedType context) {
		final var clazz = erasedType( context.getRawType() );
		if ( clazz == null ) {
			return null;
		}

		final var typeArguments = context.getActualTypeArguments();
		final var typeParameters = clazz.getTypeParameters();
		for ( int idx = 0; idx < typeParameters.length; idx++ ) {
			if ( typeVariable.getName().equals( typeParameters[idx].getName() ) ) {
				return resolveType( typeArguments[idx], context );
			}
		}

		return typeVariable;
	}

	private static Type resolveType(Type target, Type context) {
		if ( target instanceof ParameterizedType parameterizedType ) {
			return resolveParameterizedType( parameterizedType, context );
		}
		else if ( target instanceof TypeVariable<?> typeVariable ) {
			return resolveTypeVariable( typeVariable, (ParameterizedType) context );
		}
		else {
			return target;
		}
	}

	private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type context) {
		final var typeArguments = parameterizedType.getActualTypeArguments();
		final var resolvedTypeArguments = new Type[typeArguments.length];
		for ( int idx = 0; idx < typeArguments.length; idx++ ) {
			resolvedTypeArguments[idx] = resolveType( typeArguments[idx], context );
		}
		return new SimpleParameterizedType(
				erasedType( parameterizedType ),
				resolvedTypeArguments,
				parameterizedType.getOwnerType()
		);
	}

	private record SimpleParameterizedType
			(@NonNull Class<?> raw, @NonNull Type[] args, @NonNull Type owner)
			implements ParameterizedType {
		@Override
		@NonNull
		public Type[] getActualTypeArguments() {
			return args;
		}

		@Override
		@NonNull
		public Type getRawType() {
			return raw;
		}

		@Override
		@NonNull
		public Type getOwnerType() {
			return owner;
		}

		@Override
		@NonNull
		public String toString() {
			final var joiner = new StringJoiner( ", ", "<", ">" );
			for ( var type : args ) {
				joiner.add( type.getTypeName() );
			}
			return raw.getName() + joiner;
		}
	}
}
