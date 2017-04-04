/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;

/**
 * Models the caching options for an entity, natural-id or collection.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Caching {
	// NOTE : TruthValue for now because I need to look at how JPA's SharedCacheMode concept is handled
	private TruthValue requested = TruthValue.UNKNOWN;
	private String region;
	private AccessType accessType;
	private boolean cacheLazyProperties;

	public Caching(TruthValue requested) {
		this.requested = requested;
	}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties) {
		this( region, accessType, cacheLazyProperties, TruthValue.UNKNOWN );
	}

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties, TruthValue requested) {
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

	public TruthValue getRequested() {
		return requested;
	}

	public void setRequested(TruthValue requested) {
		this.requested = requested;
	}

	public void overlay(CacheRegionDefinition overrides) {
		if ( overrides == null ) {
			return;
		}

		requested = TruthValue.TRUE;
		accessType = AccessType.fromExternalName( overrides.getUsage() );
		if ( StringHelper.isEmpty( overrides.getRegion() ) ) {
			region = overrides.getRegion();
		}
		// ugh, primitive boolean
		cacheLazyProperties = overrides.isCacheLazy();
	}

	public void overlay(Caching overrides) {
		if ( overrides == null ) {
			return;
		}

		this.requested = overrides.requested;
		this.accessType = overrides.accessType;
		this.region = overrides.region;
		this.cacheLazyProperties = overrides.cacheLazyProperties;
	}

	@Override
	public String toString() {
		return "Caching{region='" + region + '\''
				+ ", accessType=" + accessType
				+ ", cacheLazyProperties=" + cacheLazyProperties
				+ ", requested=" + requested + '}';
	}

}
