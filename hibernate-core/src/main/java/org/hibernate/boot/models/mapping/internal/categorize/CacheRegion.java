/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;

/// Categorized cache-region options for an entity, natural-id, or collection.
///
/// The region combines explicit annotation/XML values with implicit boot defaults.
/// Binding may still overlay settings from external cache-region definitions before
/// applying the final cache configuration.
///
/// @since 9.0
/// @author Steve Ebersole
/// @author Hardy Ferentschik
public class CacheRegion {
	private String regionName;
	private AccessType accessType;
	private boolean cacheLazyProperties;

	/// Create a cache-region descriptor from an optional {@link Cache} annotation
	/// and implicit cache settings.
	public CacheRegion(
			Cache cacheAnnotation,
			AccessType implicitCacheAccessType,
			String implicitRegionName) {
		if ( cacheAnnotation == null ) {
			regionName = implicitRegionName;
			accessType = implicitCacheAccessType;
			cacheLazyProperties = true;
		}
		else {
			final String explicitRegionName = cacheAnnotation.region();
			regionName = StringHelper.isEmpty( explicitRegionName ) ? implicitRegionName : explicitRegionName;

			accessType = interpretAccessStrategy( cacheAnnotation.usage() );

			cacheLazyProperties = cacheAnnotation.includeLazy();
		}
	}

	private AccessType interpretAccessStrategy(CacheConcurrencyStrategy usage) {
		if ( usage == null ) {
			return null;
		}
		return switch ( usage ) {
			case NONE -> null;
			case READ_ONLY -> AccessType.READ_ONLY;
			case READ_WRITE -> AccessType.READ_WRITE;
			case NONSTRICT_READ_WRITE -> AccessType.NONSTRICT_READ_WRITE;
			case TRANSACTIONAL -> AccessType.TRANSACTIONAL;
		};
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public boolean isCacheLazyProperties() {
		return cacheLazyProperties;
	}

	public void setCacheLazyProperties(boolean cacheLazyProperties) {
		this.cacheLazyProperties = cacheLazyProperties;
	}

	/// Overlay settings from an externally supplied cache-region definition.
	public void overlay(CacheRegionDefinition overrides) {
		if ( overrides == null ) {
			return;
		}

		accessType = AccessType.fromExternalName( overrides.usage() );
		if ( StringHelper.isNotEmpty( overrides.region() ) ) {
			regionName = overrides.region();
		}
		// ugh, primitive boolean
		cacheLazyProperties = overrides.cacheLazy();
	}

	/// Overlay settings from another categorized cache-region descriptor.
	public void overlay(CacheRegion overrides) {
		if ( overrides == null ) {
			return;
		}

		this.accessType = overrides.accessType;
		this.regionName = overrides.regionName;
		this.cacheLazyProperties = overrides.cacheLazyProperties;
	}

	@Override
	public String toString() {
		return "Caching{" +
				"region='" + regionName + '\''
				+ ", accessType=" + accessType
				+ ", cacheLazyProperties=" + cacheLazyProperties + '}';
	}

}
