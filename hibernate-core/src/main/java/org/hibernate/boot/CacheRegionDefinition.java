/*
 * SPDX-License-Identifier: Apache-2.0
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
public record CacheRegionDefinition(CacheRegionType regionType,
									String role, String usage, String region,
									boolean cacheLazy) {

	public enum CacheRegionType {
		ENTITY,
		COLLECTION,
		QUERY
	}

	@Deprecated(since = "7")
	public CacheRegionType getRegionType() {
		return regionType;
	}

	@Deprecated(since = "7")
	public String getRole() {
		return role;
	}

	@Deprecated(since = "7")
	public String getUsage() {
		return usage;
	}

	@Deprecated(since = "7")
	public String getRegion() {
		return region;
	}

	@Deprecated(since = "7")
	public boolean isCacheLazy() {
		return cacheLazy;
	}
}
