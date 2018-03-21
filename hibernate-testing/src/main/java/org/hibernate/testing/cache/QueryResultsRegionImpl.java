/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.DirectAccessRegionTemplate;
import org.hibernate.cache.spi.support.StorageAccess;

/**
 * @author Steve Ebersole
 */
public class QueryResultsRegionImpl extends DirectAccessRegionTemplate implements QueryResultsRegion {
	private final StorageAccessImpl storageAccess = new StorageAccessImpl();

	public QueryResultsRegionImpl(
			String name,
			RegionFactory regionFactory) {
		super( name, regionFactory );
	}

	@Override
	public StorageAccess getStorageAccess() {
		return storageAccess;
	}
}
