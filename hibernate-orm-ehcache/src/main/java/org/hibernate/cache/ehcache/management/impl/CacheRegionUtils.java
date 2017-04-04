/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;

/**
 * CacheRegionUtils
 *
 * @author gkeim
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class CacheRegionUtils {
	/**
	 * Determine a short name from the full name
	 *
	 * @param fullName The full name
	 *
	 * @return The short name
	 */
	public static String determineShortName(String fullName) {
		String result = fullName;

		if ( fullName != null ) {
			final String[] comps = fullName.split( "\\." );
			if ( comps.length == 1 ) {
				return fullName;
			}
			boolean truncate = true;
			for ( int i = 0; i < comps.length; i++ ) {
				final String comp = comps[i];
				final char c = comp.charAt( 0 );
				if ( truncate && Character.isUpperCase( c ) ) {
					truncate = false;
				}
				if ( truncate ) {
					comps[i] = Character.toString( c );
				}
			}
			result = join( comps, '.' );
		}

		return result;
	}

	/**
	 * Same as Hibernate internal {@link org.hibernate.internal.util.StringHelper#join} methods
	 *
	 * @param elements The things to join
	 * @param c The separator between elements
	 *
	 * @return The joined string
	 */
	private static String join(String[] elements, char c) {
		if ( elements == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		for ( String s : elements ) {
			sb.append( s ).append( c );
		}
		return sb.length() > 0 ? sb.substring( 0, sb.length() - 1 ) : "";
	}
}
