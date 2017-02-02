/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan;

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
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final List<StrategyRegistration> strategyRegistrations = new ArrayList<StrategyRegistration>();

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl<RegionFactory>(
						RegionFactory.class,
						InfinispanRegionFactory.class,
						"infinispan",
						InfinispanRegionFactory.class.getName(),
						InfinispanRegionFactory.class.getSimpleName()
				)
		);

		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl<RegionFactory>(
						RegionFactory.class,
						JndiInfinispanRegionFactory.class,
						"infinispan-jndi",
						JndiInfinispanRegionFactory.class.getName(),
						JndiInfinispanRegionFactory.class.getSimpleName()
				)
		);

		return strategyRegistrations;
	}
}
