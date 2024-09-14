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
public class CaseInsensitiveContainsMatcher extends TypeSafeMatcher<String> {
	private final String match;

	public CaseInsensitiveContainsMatcher(String match) {
		this.match = match.toLowerCase( Locale.ROOT );
	}

	public static Matcher<String> contains(String expected) {
		expected = expected.toLowerCase( Locale.ROOT );
		return new CaseInsensitiveContainsMatcher( expected );
	}

	@Override
	protected boolean matchesSafely(String string) {
		final String normalized = string.toLowerCase( Locale.ROOT ).trim();
		return normalized.contains( match );
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "contains (case insensitive)" ).appendValue( match );
	}
}
