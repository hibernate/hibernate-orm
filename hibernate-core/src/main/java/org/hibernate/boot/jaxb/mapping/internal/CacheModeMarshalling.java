/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.CacheMode;

/**
 * JAXB marshalling for Hibernate's {@link CacheMode}
 *
 * @author Steve Ebersole
 */
public class CacheModeMarshalling {
	public static CacheMode fromXml(String name) {
		for ( CacheMode mode : CacheMode.values() ) {
			if ( mode.name().equalsIgnoreCase( name ) ) {
				return mode;
			}
		}
		return CacheMode.NORMAL;
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode == null ? null : cacheMode.name().toLowerCase( Locale.ENGLISH );
	}
}
