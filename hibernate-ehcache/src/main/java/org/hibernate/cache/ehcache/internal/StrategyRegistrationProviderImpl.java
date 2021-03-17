/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cache.spi.RegionFactory;

/**
 * Makes the 2 contained region factory implementations available to the Hibernate
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service.
 *
 * @author Steve Ebersole
 */
public final class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final List<StrategyRegistration> strategyRegistrations = new ArrayList<StrategyRegistration>( 2 );

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl(
						RegionFactory.class,
						EhcacheRegionFactory.class,
						"ehcache",
						EhcacheRegionFactory.class.getName(),
						EhcacheRegionFactory.class.getSimpleName(),
						// legacy impl class name
						"org.hibernate.cache.EhCacheRegionFactory",
						"org.hibernate.cache.ehcache.EhCacheRegionFactory"
				)
		);

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl(
						RegionFactory.class,
						SingletonEhcacheRegionFactory.class,
						"ehcache-singleton",
						SingletonEhcacheRegionFactory.class.getName(),
						SingletonEhcacheRegionFactory.class.getSimpleName(),
						// legacy impl class name
						"org.hibernate.cache.SingletonEhCacheRegionFactory",
						"org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory"
				)
		);

		return strategyRegistrations;
	}
}
