/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2.util;

import org.jboss.cache.Cache;
import org.jboss.cache.config.Configuration;

/**
 * Helper for dealing with JBossCache {@link Configuration.CacheMode}.
 *
 * @author Steve Ebersole
 */
public class CacheModeHelper {
	/**
	 * Disallow external instantiation of CacheModeHelper.
	 */
	private CacheModeHelper() {
	}

	/**
	 * Is this cache participating in a cluster with invalidation?
	 *
	 * @param cache The cache to check.
	 * @return True if the cache is configured for synchronous/asynchronous invalidation; false
	 * otherwise.
	 */
	public static boolean isClusteredInvalidation(Cache cache) {
		return isClusteredInvalidation( cache.getConfiguration().getCacheMode() );
	}

	/**
	 * Does this cache mode indicate clustered invalidation?
	 *
	 * @param cacheMode The cache to check
	 * @return True if the cache mode is confiogured for synchronous/asynchronous invalidation; false
	 * otherwise.
	 */
	public static boolean isClusteredInvalidation(Configuration.CacheMode cacheMode) {
		return cacheMode == Configuration.CacheMode.REPL_ASYNC || cacheMode == Configuration.CacheMode.REPL_SYNC;
	}
}
