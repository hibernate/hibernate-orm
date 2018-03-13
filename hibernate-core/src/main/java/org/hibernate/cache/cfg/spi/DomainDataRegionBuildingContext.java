/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * A "parameter object" for {@link RegionFactory#buildDomainDataRegion}
 * calls, giving it access to information it needs.
 *
 * @author Steve Ebersole
 */
public interface DomainDataRegionBuildingContext {
	/**
	 * The CacheKeyFactory explicitly specified as part of the
	 * bootstrap by the user, by some "container", etc.
	 *
	 * If this method returns a non-null value, it is expected
	 * that RegionFactory implementors will use to be its
	 * CacheKeyFactory and return it when asked later.
	 */
	CacheKeysFactory getEnforcedCacheKeysFactory();

	/**
	 * Access to the SessionFactory for which a Region is
	 * being built.
	 */
	SessionFactoryImplementor getSessionFactory();
}
