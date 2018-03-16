/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * @author Steve Ebersole
 */
public class RegionNameQualifier {
	/**
	 * Singleton access
	 */
	public static final RegionNameQualifier INSTANCE = new RegionNameQualifier();

	public String qualify(String regionName, SessionFactoryOptions options) {
		final String prefix = options.getCacheRegionPrefix();
		if ( prefix == null ) {
			return regionName;
		}
		else {
			if ( regionName.startsWith( prefix ) ) {
				return regionName.substring( prefix.length() );
			}

			return prefix + regionName;
		}
	}

	private RegionNameQualifier() {
	}
}
