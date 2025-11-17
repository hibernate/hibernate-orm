/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache.internal;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.spi.RegionFactory;

import static java.util.Collections.singleton;

/**
 * Makes the JCache RegionFactory available to the Hibernate
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service
 * under a number of keys.  Prefer to use
 *
 * @author Steve Ebersole
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	public Iterable<StrategyRegistration<?>> getStrategyRegistrations() {

		return singleton( new SimpleStrategyRegistrationImpl<>(
				RegionFactory.class,
				JCacheRegionFactory.class,
				ConfigSettings.SIMPLE_FACTORY_NAME,
				JCacheRegionFactory.class.getName(),
				JCacheRegionFactory.class.getSimpleName(),
				// legacy impl class name
				"org.hibernate.cache.jcache.JCacheRegionFactory"
		) );
	}
}
