/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.models.spi.MemberDetails;

import java.lang.reflect.*;
import java.util.*;

public final class GenericTypeResolver {

	public static Type inheritedMemberType(Class<?> subclass, Member superMember) {
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
			final var raw = rawClass( type );
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

	public static Class<?> erasedType(Type type) {
		if ( type instanceof Class<?> c ) {
			return c;
		}
		else if ( type instanceof ParameterizedType pt ) {
			return (Class<?>) pt.getRawType();
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

	private static Class<?> rawClass(Type type) {
		if ( type instanceof Class<?> c ) {
			return c;
		}
		else if ( type instanceof ParameterizedType pt ) {
			return (Class<?>) pt.getRawType();
		}
		else {
			return null;
		}
	}

	public static Type resolveMemberType(MemberDetails memberDetails) {
		return inheritedMemberType(
				memberDetails.getDeclaringType().toJavaClass(),
				memberDetails.toJavaMember()
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

	public static Type[] resolveInterfaceTypeArguments(
			Class<?> genericInterface, Type implementingType) {
		final Map<TypeVariable<?>, Type> typeMap = new HashMap<>();
		if ( !resolveInterfaceRecursive( implementingType, genericInterface, typeMap ) ) {
			throw new IllegalArgumentException(
					implementingType + " does not implement " + genericInterface );
		}

		final var vars = genericInterface.getTypeParameters();
		final var result = new Type[vars.length];
		for ( int i = 0; i < vars.length; i++ ) {
			result[i] = substituteTypeVariables( vars[i], typeMap );
		}
		return result;
	}

	private static boolean resolveInterfaceRecursive(
			Type current,
			Class<?> targetInterface,
			Map<TypeVariable<?>, Type> typeMap) {

		if ( current == null ) {
			return false;
		}

		final var raw = rawClass( current );
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
			final var ifaceRaw = rawClass( iface );
			if ( ifaceRaw != null ) {
				if ( ifaceRaw == targetInterface ) {
					collectTypeArguments( iface, ifaceRaw, typeMap );
					return true;
				}

				if ( targetInterface.isAssignableFrom( ifaceRaw ) ) {
					if ( resolveInterfaceRecursive( iface, targetInterface, typeMap ) ) {
						collectTypeArguments( iface, ifaceRaw, typeMap );
						return true;
					}
				}
			}

		}

		// Check superclass
		final var superclass = raw.getGenericSuperclass();
		if ( superclass != null ) {
			final var classRaw = rawClass( superclass );
			if ( classRaw != null ) {
				if ( classRaw == targetInterface ) {
					collectTypeArguments( superclass, classRaw, typeMap );
					return true;
				}

				if ( resolveInterfaceRecursive( superclass, targetInterface, typeMap ) ) {
					collectTypeArguments( superclass, rawClass( superclass ), typeMap );
					return true;
				}
			}
		}

		return false;
	}
}
