/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
				//It wasn't valid after all, so return null
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
