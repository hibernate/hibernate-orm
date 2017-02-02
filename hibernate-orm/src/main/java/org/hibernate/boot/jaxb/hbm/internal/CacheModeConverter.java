/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.CacheMode;

/**
 * @author Steve Ebersole
 */
public class CacheModeConverter {
	public static CacheMode fromXml(String name) {
		for ( CacheMode mode : CacheMode.values() ) {
			if ( mode.name().equalsIgnoreCase( name ) ) {
				return mode;
			}
		}
		return CacheMode.NORMAL;
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode.name().toLowerCase( Locale.ENGLISH );
	}
}
