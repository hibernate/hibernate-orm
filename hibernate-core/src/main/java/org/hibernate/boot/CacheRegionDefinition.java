/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

/**
 * Models the definition of caching settings for a particular region.  Generally found in:<ul>
 *     <li>{@code cfg.xml}</li>
 *     <li>annotations</li>
 *     <li>{@code orm.xml}</li>
 *     <li>{@code hbm.xml}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class CacheRegionDefinition {
	public enum CacheRegionType {
		ENTITY,
		COLLECTION,
		QUERY
	}

	private final CacheRegionType regionType;
	private final String role;
	private final String usage;
	private final String region;
	private final boolean cacheLazy;

	public CacheRegionDefinition(
			CacheRegionType cacheType,
			String role,
			String usage,
			String region,
			boolean cacheLazy) {
		this.regionType = cacheType;
		this.role = role;
		this.usage = usage;
		this.region = region;
		this.cacheLazy = cacheLazy;
	}

	public CacheRegionType getRegionType() {
		return regionType;
	}

	public String getRole() {
		return role;
	}

	public String getUsage() {
		return usage;
	}

	public String getRegion() {
		return region;
	}

	public boolean isCacheLazy() {
		return cacheLazy;
	}
}
