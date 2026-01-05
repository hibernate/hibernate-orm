/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * @author Gavin King
 * @since 7.3
 */
public class Constructors {

	public static <C> C construct(
			Class<? extends C> construction,
			TypedArgument<?>... arguments)
					throws InstantiationException, IllegalAccessException, InvocationTargetException {
		try {
			final var types = Arrays.stream( arguments ).map( TypedArgument::type ).toArray( Class<?>[]::new );
			final var values = Arrays.stream( arguments ).map( TypedArgument::argument ).toArray();
			final var constructor = construction.getDeclaredConstructor( types );
			constructor.setAccessible( true );
			return constructor.newInstance( values );
		}
		catch (NoSuchMethodException ignore) {
			return null;
		}
	}

	public static <C,U> C construct(
			Class<? extends C> construction,
			Class<U> type, U argument)
					throws InstantiationException, IllegalAccessException, InvocationTargetException {
		return construct( construction, TypedArgument.of( type, argument ) );
	}

	public static <C,U,V> C construct(
			Class<? extends C> construction,
			Class<U> type1, U argument1,
			Class<V> type2, V argument2)
					throws InstantiationException, IllegalAccessException, InvocationTargetException {
		return construct( construction,
				TypedArgument.of( type1, argument1 ),
				TypedArgument.of( type2, argument2 ) );
	}

	public static <C,U,V,W> C construct(
			Class<? extends C> construction,
			Class<U> type1, U argument1,
			Class<V> type2, V argument2,
			Class<W> type3, W argument3)
					throws InstantiationException, IllegalAccessException, InvocationTargetException {
		return construct( construction,
				TypedArgument.of( type1, argument1 ),
				TypedArgument.of( type2, argument2 ),
				TypedArgument.of( type3, argument3 ) );
	}

	public record TypedArgument<T>(Class<T> type, T argument) {
		public static <T> TypedArgument<T> of(Class<T> type, T argument) {
			return new TypedArgument<>(type, argument);
		}
	}
}
