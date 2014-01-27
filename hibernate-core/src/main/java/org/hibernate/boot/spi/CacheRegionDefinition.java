/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.spi;

/**
 * Models the definition of caching settings for a particular region.  Generally as found in either:<ul>
 *     <li>{@code cfg.xml}</li>
 *     <li>{@code hbm.xml}</li>
 *     <li>annotation</li>
 * </ul>
 * Though certainly other custom sources are acceptable too.
 *
 * @author Steve Ebersole
 */
public class CacheRegionDefinition {
	public static enum CacheRegionType {
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
