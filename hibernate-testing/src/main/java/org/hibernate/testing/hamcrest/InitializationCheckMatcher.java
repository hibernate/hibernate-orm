/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.hamcrest;

import org.hibernate.Hibernate;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * @author Steve Ebersole
 */
public class InitializationCheckMatcher<T> extends BaseMatcher<T> {
	public static final InitializationCheckMatcher INITIALIZED_MATCHER = new InitializationCheckMatcher();
	public static final InitializationCheckMatcher UNINITIALIZED_MATCHER = new InitializationCheckMatcher( false );

	public static <T> InitializationCheckMatcher<T> isInitialized() {
		//noinspection unchecked
		return INITIALIZED_MATCHER;
	}

	public static <T> Matcher<T> isNotInitialized() {
		//noinspection unchecked
		return UNINITIALIZED_MATCHER;
	}

	private final boolean assertInitialized;

	public InitializationCheckMatcher() {
		this( true );
	}

	public InitializationCheckMatcher(boolean assertInitialized) {
		this.assertInitialized = assertInitialized;
	}

	@Override
	public boolean matches(Object item) {
		return assertInitialized == Hibernate.isInitialized( item );
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue( "Hibernate#isInitialized() returns " + assertInitialized );
	}

}
