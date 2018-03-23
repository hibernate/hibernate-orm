/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import net.sf.ehcache.Cache;

import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.support.DirectAccessRegionTemplate;
import org.hibernate.cache.spi.support.StorageAccess;

/**
 * @author Chris Dennis
 * @author Alex Snaps
 * @author Steve Ebersole
 */
public class QueryResultsRegionImpl extends DirectAccessRegionTemplate implements QueryResultsRegion {
	private final StorageAccessImpl cacheAccess;

	public QueryResultsRegionImpl(
			String name,
			RegionFactory regionFactory,
			Cache cache) {
		super( name, regionFactory );
		this.cacheAccess = new StorageAccessImpl( cache );
	}

	@Override
	public StorageAccess getStorageAccess() {
		return cacheAccess;
	}
}
