/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.hamcrest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * @author Steve Ebersole
 */
public class CollectionElementMatcher<E,C extends Collection<E>> extends BaseMatcher<C> {
	public static <T> Matcher<Collection<T>> hasAllOf(Matcher<T>... elementMatchers) {
		return new CollectionElementMatcher<>( elementMatchers );
	}

	private final List<Matcher<E>> elementMatchers;

	public CollectionElementMatcher(Matcher<E>... elementMatchers) {
		this.elementMatchers = Arrays.asList( elementMatchers );
	}

	@Override
	public boolean matches(Object o) {
		assert o instanceof Collection;
		final Collection collection = (Collection) o;

		outer: for ( Matcher<E> valueMatcher : elementMatchers ) {
			for ( Object value : collection ) {
				if ( valueMatcher.matches( value ) ) {
					continue outer;
				}
			}

			return false;
		}

		return true;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "contained" );
	}
}
