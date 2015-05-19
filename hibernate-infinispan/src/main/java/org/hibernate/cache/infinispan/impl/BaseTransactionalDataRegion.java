/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.impl;

import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;

import org.infinispan.AdvancedCache;

/**
 * Support for Inifinispan {@link org.hibernate.cache.spi.TransactionalDataRegion} implementors.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseTransactionalDataRegion
		extends BaseRegion implements TransactionalDataRegion {

	private final CacheDataDescription metadata;

   /**
    * Base transactional region constructor
    *
    * @param cache instance to store transactional data
    * @param name of the transactional region
    * @param metadata for the transactional region
    * @param factory for the transactional region
    */
	public BaseTransactionalDataRegion(
			AdvancedCache cache, String name,
			CacheDataDescription metadata, RegionFactory factory) {
		super( cache, name, factory );
		this.metadata = metadata;
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

}
