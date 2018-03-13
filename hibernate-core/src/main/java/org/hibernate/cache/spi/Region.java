/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.CacheException;

/**
 * Defines a contract for accessing a particular named region within the 
 * underlying cache implementation.
 *
 * @author Steve Ebersole
 */
public interface Region {
	/**
	 * Retrieve the name of this region.
	 *
	 * @return The region name
	 */
	String getName();

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
