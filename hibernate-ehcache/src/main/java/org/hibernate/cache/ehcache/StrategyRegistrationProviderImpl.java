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
package org.hibernate.cache.ehcache;

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
public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final List<StrategyRegistration> strategyRegistrations = new ArrayList<StrategyRegistration>();

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl(
						RegionFactory.class,
						EhCacheRegionFactory.class,
						"ehcache",
						EhCacheRegionFactory.class.getName(),
						EhCacheRegionFactory.class.getSimpleName(),
						// legacy impl class name
						"org.hibernate.cache.EhCacheRegionFactory"
				)
		);

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl(
						RegionFactory.class,
						SingletonEhCacheRegionFactory.class,
						"ehcache-singleton",
						SingletonEhCacheRegionFactory.class.getName(),
						SingletonEhCacheRegionFactory.class.getSimpleName(),
						// legacy impl class name
						"org.hibernate.cache.SingletonEhCacheRegionFactory"
				)
		);

		return strategyRegistrations;
	}
}
