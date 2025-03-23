/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;

/**
 * @author Steve Ebersole
 */
public class TimestampsRegionTemplate extends DirectAccessRegionTemplate implements TimestampsRegion {
	/**
	 * Constructs a {@link TimestampsRegionTemplate}.
	 *
	 * @param name - the unqualified region name
	 * @param regionFactory - the region factory
	 * @param storageAccess - the cache storage access strategy
	 */
	public TimestampsRegionTemplate(
			String name,
			RegionFactory regionFactory,
			StorageAccess storageAccess) {
		super( name, regionFactory, storageAccess );
	}
}
