/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.support.AbstractRegion;

/**
 * A timestamps region specific wrapper around an Ehcache instance.
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class TimestampsRegionImpl extends AbstractRegion implements TimestampsRegion {
	public TimestampsRegionImpl(
			String regionName,
			JCacheRegionFactory regionFactory,
			Cache cache) {
		super(
				regionName,
				regionFactory,
				new JCacheAccessImpl( cache )
		);
	}
}
