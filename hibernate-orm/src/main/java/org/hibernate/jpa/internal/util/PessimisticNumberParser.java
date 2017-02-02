/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

/**
 * An old-style query might pass positional numbers of Query parameters as strings. This implies we always need to
 * attempt parsing string parameters to see if this deprecated feature is being used, but parsing leads to catching (and
 * ignoring) NumberFormatException at runtime which is a performance problem, especially as the parse would fail
 * each time a non-deprecated form is processed.
 * This class is meant to avoid the need to allocate these exceptions at runtime.
 *
 * Use this class to convert String to Integer when it's unlikely to be successful: if you expect it to be a normal number,
 * for example when a non successful parsing would be an error, using this utility is just an overhead.
 *
 * @author Sanne Grinovero
 */
public final class PessimisticNumberParser {

	private PessimisticNumberParser() {
		//not to be constructed
	}

	public static Integer toNumberOrNull(final String parameterName) {
		if ( isValidNumber( parameterName ) ) {
			try {
				return Integer.valueOf( parameterName );
			}
			catch (NumberFormatException e) {
				//It wasn't valid afterQuery all, so return null
			}
		}
		return null;
	}

	private static boolean isValidNumber(final String parameterName) {
		if ( parameterName.length() == 0 ) {
			return false;
		}
		final char firstDigit = parameterName.charAt( 0 );
		if ( Character.isDigit( firstDigit ) || '-' == firstDigit || '+' == firstDigit ) {
			//check the remaining characters
			for ( int i = 1; i < parameterName.length(); i++ ) {
				if ( !Character.isDigit( parameterName.charAt( i ) ) ) {
					return false;
				}
			}
			//Some edge cases are left open: just a sign would return true.
			//For those cases you'd have a NumberFormatException swallowed.
			return true;
		}
		return false;
	}

}
