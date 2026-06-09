/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.internal.util.StringHelper;

/// Categorized cache-region options for an entity natural id.
///
/// Natural-id cache settings can name their own region.  When no explicit region
/// is supplied, Hibernate derives the natural-id region from the entity cache
/// region.
///
/// @see CacheRegion
///
/// @author Steve Ebersole
public class NaturalIdCacheRegion {
	private final String regionName;

	/// Create a natural-id cache-region descriptor from an optional annotation
	/// and the entity cache-region defaults.
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

	/// The cache region name to use for the natural-id cache.
	public String getRegionName() {
		return regionName;
	}
}
