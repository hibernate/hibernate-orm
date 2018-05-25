/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;

/**
 * Contract for a named "region".  The concept of a Region might not
 * necessarily correlate to a specific concept in the underlying caching
 * provider - it is just a thing that can be referenced by name later.
 * <p/>
 * A region's name is "unqualified"; i.e. it is not prefixed by
 * {@link SessionFactoryOptions#getCacheRegionPrefix()}.
 * <p/>
 * Region is the base contract defining some common characteristics
 * regardless of the type of data intended to be stored within this
 * Region.  The more specific sub-types are {@link DomainDataRegion}
 * (storing entity, collection and natural-id data) and
 * {@link DirectAccessRegion} (storing query result and timestamp
 * data).
 *
 * @author Steve Ebersole
 */
public interface Region {
	/**
	 * Retrieve the unqualified name of this region.
	 */
	String getName();

	/**
	 * The RegionFactory that generated this Region
	 */
	RegionFactory getRegionFactory();

	/**
	 * Clear all data cached in the region
	 */
	void clear();

	/**
	 * The "end state" contract of the region's lifecycle.  Called
	 * during {@link org.hibernate.SessionFactory#close()} to give
	 * the region a chance to cleanup.
	 *
	 * @throws org.hibernate.cache.CacheException Indicates problem shutting down
	 */
	void destroy() throws CacheException;
}
