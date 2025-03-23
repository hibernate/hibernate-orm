/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * @author Steve Ebersole
 */
public class CacheUtils {
	public static boolean isUnqualified(String regionName, SessionFactoryOptions options) {
		final String prefix = options.getCacheRegionPrefix();
		if ( prefix == null ) {
			return true;
		}
		else {
			return !regionName.startsWith( prefix );
		}
	}
}
