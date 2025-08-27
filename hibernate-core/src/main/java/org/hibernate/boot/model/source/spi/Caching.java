/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Models the caching options for an entity, natural id, or collection.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Caching {
	private Boolean requested;
	private String region;
	private AccessType accessType;
	private boolean cacheLazyProperties;

	public Caching() {}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties) {
		this.region = region;
		this.accessType = accessType;
		this.cacheLazyProperties = cacheLazyProperties;
	}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties, boolean requested) {
		this.requested = requested;
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
		return requested == Boolean.TRUE;
	}

	public boolean isRequested(boolean defaultValue) {
		return requested == null ? defaultValue : isRequested();
	}

	public void setRequested(boolean requested) {
		this.requested = requested;
	}

	public void overlay(CacheRegionDefinition overrides) {
		if ( overrides != null ) {
			requested = true;
			accessType = AccessType.fromExternalName( overrides.usage() );
			if ( !isEmpty( overrides.region() ) ) {
				region = overrides.region();
			}
			cacheLazyProperties = overrides.cacheLazy();
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
