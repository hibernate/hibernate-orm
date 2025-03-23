/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public class QueryResultsRegionTemplate extends DirectAccessRegionTemplate implements QueryResultsRegion {
	/**
	 * Constructs a {@link QueryResultsRegionTemplate}.
	 *
	 * @param name - the unqualified region name
	 * @param regionFactory - the region factory
	 * @param storageAccess - the cache storage access strategy
	 */
	public QueryResultsRegionTemplate(
			String name,
			RegionFactory regionFactory,
			StorageAccess storageAccess) {
		super( name, regionFactory, storageAccess );
	}
}
