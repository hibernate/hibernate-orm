/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
