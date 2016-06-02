/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.TransactionalDataRegion;

/**
 * @author Strong Liu
 */
class BaseTransactionalDataRegion extends BaseGeneralDataRegion implements TransactionalDataRegion {
	private final CacheDataDescription metadata;

	BaseTransactionalDataRegion(CachingRegionFactory cachingRegionFactory, String name, CacheDataDescription metadata) {
		super( cachingRegionFactory, name );
		this.metadata = metadata;
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

	@Override
	public boolean isTransactionAware() {
		return false;
	}

}
