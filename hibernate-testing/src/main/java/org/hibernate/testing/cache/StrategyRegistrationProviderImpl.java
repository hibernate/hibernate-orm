/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.cache;

import java.util.Collections;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cache.spi.RegionFactory;

/**
 * Makes the JCache RegionFactory available to the Hibernate
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service
 * under a number of keys.  Prefer to use
 *
 * @author Steve Ebersole
 */
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return Collections.singletonList(
				new SimpleStrategyRegistrationImpl(
						RegionFactory.class,
						CachingRegionFactory.class,
						"testing",
						CachingRegionFactory.class.getName()
				)
		);
	}
}
