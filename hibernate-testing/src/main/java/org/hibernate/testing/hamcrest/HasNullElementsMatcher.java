/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.hamcrest;

import java.util.Collection;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class HasNullElementsMatcher<C extends Collection<?>> extends BaseMatcher<C> {
	public static final HasNullElementsMatcher HAS_NULL_ELEMENTS_MATCHER = new HasNullElementsMatcher( false );
	public static final HasNullElementsMatcher HAS_NO_NULL_ELEMENTS_MATCHER = new HasNullElementsMatcher( true );

	private final boolean negated;

	public HasNullElementsMatcher(boolean negated) {
		this.negated = negated;
	}

	@Override
	public boolean matches(Object item) {
		assertThat( item, instanceOf( Collection.class ) );

		//noinspection unchecked
		C collection = (C) item;

		if ( negated ) {
			// check no-null-elements - if any is null, this check fails
			collection.forEach( e -> assertThat( e, notNullValue() ) );
			return true;
		}

		boolean foundOne = false;
		for ( Object e : collection ) {
			if ( e == null ) {
				foundOne = true;
				break;
			}
		}
		return foundOne;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "had null elements" );
	}
}
