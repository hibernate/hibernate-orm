/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

		return qualify( prefix, regionName );
	}

	public String qualify(String prefix, String regionName) {
		if ( regionName.startsWith( prefix + '.' ) ) {
			return regionName;
		}

		return prefix + '.' + regionName;
	}


	public boolean isQualified(String regionName, SessionFactoryOptions options) {
		return isQualified( options.getCacheRegionPrefix(), regionName );
	}

	public boolean isQualified(String prefix, String regionName) {
		return prefix != null && regionName.startsWith( prefix );
	}

	private RegionNameQualifier() {
	}
}
