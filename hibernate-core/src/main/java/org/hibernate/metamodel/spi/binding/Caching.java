/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.binding;

import org.hibernate.TruthValue;
import org.hibernate.boot.spi.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;

/**
 * Defines the caching settings for an entity.
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
