/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import net.sf.ehcache.Cache;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.support.DirectAccessRegionTemplate;
import org.hibernate.cache.spi.support.StorageAccess;

/**
 * Access to a JCache Cache used to store "update timestamps".
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class TimestampsRegionImpl extends DirectAccessRegionTemplate implements TimestampsRegion {
	private final StorageAccessImpl cacheAccess;

	public TimestampsRegionImpl(
			String regionName,
			RegionFactory regionFactory,
			Cache cache) {
		super( regionName, regionFactory );
		this.cacheAccess = new StorageAccessImpl( cache );
	}

	@Override
	public StorageAccess getStorageAccess() {
		return cacheAccess;
	}
}
