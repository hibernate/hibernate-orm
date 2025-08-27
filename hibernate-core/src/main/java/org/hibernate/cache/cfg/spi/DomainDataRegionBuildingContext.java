/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * The {@link CacheKeysFactory} explicitly specified as part of
	 * the bootstrap by the user, by some "container", etc.
	 *
	 * If this method returns a non-null value, it is expected that
	 * {@link RegionFactory} implementors will use to be its
	 * {@link CacheKeysFactory} and return it when asked later.
	 */
	CacheKeysFactory getEnforcedCacheKeysFactory();

	/**
	 * Access to the SessionFactory for which a Region is
	 * being built.
	 */
	SessionFactoryImplementor getSessionFactory();
}
