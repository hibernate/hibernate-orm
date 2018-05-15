/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Defines the caching settings for an entity/collection.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Caching {
	private final Boolean requested;
	private final String region;
	private final AccessType accessType;
	private final boolean cacheLazyProperties;

	public Caching(String region, AccessType accessType, boolean cacheLazyProperties, Boolean requested) {
		this.requested = requested;
		this.region = region;
		this.accessType = accessType;
		this.cacheLazyProperties = cacheLazyProperties;
	}

	public Boolean isRequested() {
		return requested;
	}

	public String getRegion() {
		return region;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public boolean isCacheLazyProperties() {
		return cacheLazyProperties;
	}

	@Override
	public String toString() {
		return "Caching{region='" + region + '\''
				+ ", accessType=" + accessType
				+ ", cacheLazyProperties=" + cacheLazyProperties
				+ ", requested=" + requested + '}';
	}

}
