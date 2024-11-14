/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.cache.spi.access.AccessType;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Models the caching options for an entity, natural id, or collection.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Caching {
	// NOTE : TruthValue for now because I need to look at how JPA's SharedCacheMode concept is handled
	private TruthValue requested;
	private String region;
	private AccessType accessType;
	private boolean cacheLazyProperties;

	public Caching() {
		this.requested = TruthValue.UNKNOWN;
	}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties) {
		this.requested = TruthValue.UNKNOWN;
		this.region = region;
		this.accessType = accessType;
		this.cacheLazyProperties = cacheLazyProperties;
	}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties, boolean requested) {
		this.requested = TruthValue.of( requested );
		this.region = region;
		this.accessType = accessType;
		this.cacheLazyProperties = cacheLazyProperties;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
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

	public boolean isRequested() {
		return requested == TruthValue.TRUE;
	}

	public boolean isRequested(boolean defaultValue) {
		return requested == TruthValue.UNKNOWN ? defaultValue : isRequested();
	}

	public void setRequested(boolean requested) {
		this.requested = TruthValue.of(requested);
	}

	public void overlay(CacheRegionDefinition overrides) {
		if ( overrides != null ) {
			requested = TruthValue.TRUE;
			accessType = AccessType.fromExternalName( overrides.getUsage() );
			if ( isEmpty( overrides.getRegion() ) ) {
				region = overrides.getRegion();
			}
			// ugh, primitive boolean
			cacheLazyProperties = overrides.isCacheLazy();
		}
	}

	public void overlay(Caching overrides) {
		if ( overrides != null ) {
			this.requested = overrides.requested;
			this.accessType = overrides.accessType;
			this.region = overrides.region;
			this.cacheLazyProperties = overrides.cacheLazyProperties;
		}
	}

	@Override
	public String toString() {
		return "Caching{region='" + region + '\''
				+ ", accessType=" + accessType
				+ ", cacheLazyProperties=" + cacheLazyProperties
				+ ", requested=" + requested + '}';
	}

}
