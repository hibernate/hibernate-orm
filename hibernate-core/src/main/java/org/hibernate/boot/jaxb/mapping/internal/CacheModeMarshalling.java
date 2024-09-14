/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;


import org.hibernate.CacheMode;

/**
 * JAXB marshalling for Hibernate's {@link CacheMode}
 *
 * @author Steve Ebersole
 */
public class CacheModeMarshalling {
	public static CacheMode fromXml(String name) {
		return name == null ? CacheMode.NORMAL : CacheMode.valueOf( name );
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode == null ? null : cacheMode.name();
	}
}
