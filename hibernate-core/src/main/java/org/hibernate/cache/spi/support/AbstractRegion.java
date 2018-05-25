/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRegion implements Region {
	private final String name;
	private final RegionFactory regionFactory;

	/**
	 * Constructs an {@link AbstractRegion}.
	 *
	 * @param name - the unqualified region name
	 * @param regionFactory - the region factory
	 */
	public AbstractRegion(String name, RegionFactory regionFactory) {
		this.name = name;
		this.regionFactory = regionFactory;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

}
