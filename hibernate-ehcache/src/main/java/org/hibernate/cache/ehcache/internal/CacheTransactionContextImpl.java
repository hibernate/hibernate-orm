/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import org.hibernate.cache.spi.AbstractCacheTransactionContext;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public class CacheTransactionContextImpl extends AbstractCacheTransactionContext {
	public CacheTransactionContextImpl(RegionFactory regionFactory) {
		super( regionFactory );
	}

	// for now, nothing else to do.
	// todo (6.0) : get with Ehcache guys and see if there is anything here they'd like to take advantage of
}
