/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.cfg.internal;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class SharedCacheModeMarshalling {
	public static SharedCacheMode fromXml(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return SharedCacheMode.UNSPECIFIED;
		}
		return SharedCacheMode.valueOf( name );
	}

	public static String toXml(SharedCacheMode sharedCacheMode) {
		if ( sharedCacheMode == null ) {
			return null;
		}
		return sharedCacheMode.name();
	}
}
