/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.hamcrest;

import java.util.Locale;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * @author Steve Ebersole
 */
public class CaseInsensitiveStartsWithMatcher extends TypeSafeMatcher<String> {
	private final String match;

	public CaseInsensitiveStartsWithMatcher(String match) {
		this.match = match.toLowerCase( Locale.ROOT );
	}

	public static Matcher<String> startsWith(String expected) {
		return new CaseInsensitiveStartsWithMatcher( expected );
	}

	@Override
	protected boolean matchesSafely(String string) {
		final String normalized = string.toLowerCase( Locale.ROOT ).trim();
		return normalized.startsWith( match );
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "starts with (case insensitive)" ).appendValue( match );
	}
}
