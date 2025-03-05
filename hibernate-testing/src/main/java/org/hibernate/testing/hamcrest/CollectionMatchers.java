/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.hamcrest;

import java.util.Collection;

import org.hamcrest.Matcher;

/**
 * @author Steve Ebersole
 */
public class CollectionMatchers {
	private static final CollectionEmptinessMatcher IS_EMPTY = new CollectionEmptinessMatcher( true );
	private static final CollectionEmptinessMatcher IS_NOT_EMPTY = new CollectionEmptinessMatcher( false );

	public static Matcher<Collection> isEmpty() {
		return IS_EMPTY;
	}

	public static Matcher<Collection> isNotEmpty() {
		return IS_NOT_EMPTY;
	}

	public static Matcher<Collection<?>> hasSize(int size) {
		return org.hamcrest.Matchers.hasSize( size );
	}

	public static <X extends Collection<?>> HasNullElementsMatcher<X> hasNullElements() {
		//noinspection unchecked
		return HasNullElementsMatcher.HAS_NULL_ELEMENTS_MATCHER;
	}

	public static <X extends Collection<?>> HasNullElementsMatcher<X> hasNoNullElements() {
		//noinspection unchecked
		return HasNullElementsMatcher.HAS_NO_NULL_ELEMENTS_MATCHER;
	}

	public static <C extends Collection<?>> Matcher<C> isInitialized() {
		return InitializationCheckMatcher.isInitialized();
	}

	public static <C extends Collection<?>> Matcher<C> isNotInitialized() {
		return InitializationCheckMatcher.isNotInitialized();
	}
}
