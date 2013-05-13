/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.management.impl;

import java.awt.Color;

/**
 * CacheRegionUtils
 *
 * @author gkeim
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class CacheRegionUtils {
	/**
	 * HIT_COLOR
	 */
	public static final Color HIT_COLOR = Color.green;

	/**
	 * MISS_COLOR
	 */
	public static final Color MISS_COLOR = Color.red;

	/**
	 * PUT_COLOR
	 */
	public static final Color PUT_COLOR = Color.blue;

	/**
	 * HIT_FILL_COLOR
	 */
	public static final Color HIT_FILL_COLOR = CacheRegionUtils.HIT_COLOR.brighter().brighter().brighter();

	/**
	 * MISS_FILL_COLOR
	 */
	public static final Color MISS_FILL_COLOR = CacheRegionUtils.MISS_COLOR.brighter().brighter().brighter();

	/**
	 * PUT_FILL_COLOR
	 */
	public static final Color PUT_FILL_COLOR = CacheRegionUtils.PUT_COLOR.brighter().brighter().brighter();

	/**
	 * HIT_DRAW_COLOR
	 */
	public static final Color HIT_DRAW_COLOR = CacheRegionUtils.HIT_COLOR.darker();

	/**
	 * MISS_DRAW_COLOR
	 */
	public static final Color MISS_DRAW_COLOR = CacheRegionUtils.MISS_COLOR.darker();

	/**
	 * PUT_DRAW_COLOR
	 */
	public static final Color PUT_DRAW_COLOR = CacheRegionUtils.PUT_COLOR.darker();


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
