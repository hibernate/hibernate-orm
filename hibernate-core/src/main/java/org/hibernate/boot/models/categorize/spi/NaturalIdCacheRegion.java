/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.internal.util.StringHelper;

/**
 * Details about caching related to the natural-id of an entity
 *
 * @see CacheRegion
 *
 * @author Steve Ebersole
 */
public class NaturalIdCacheRegion {
	private final String regionName;

	public NaturalIdCacheRegion(NaturalIdCache cacheAnnotation, CacheRegion cacheRegion) {
		this.regionName = determineRegionName( cacheAnnotation, cacheRegion );
	}

	private static String determineRegionName(NaturalIdCache cacheAnnotation, CacheRegion cacheRegion) {
		if ( cacheAnnotation != null ) {
			final String explicitRegionName = cacheAnnotation.region();
			if ( StringHelper.isNotEmpty( explicitRegionName ) ) {
				return explicitRegionName;
			}
		}

		// use the default value
		return cacheRegion.getRegionName() + "##NaturalId";
	}

	public String getRegionName() {
		return regionName;
	}
}
