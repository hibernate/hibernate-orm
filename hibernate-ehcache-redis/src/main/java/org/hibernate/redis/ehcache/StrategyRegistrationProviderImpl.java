package org.hibernate.redis.ehcache;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cache.spi.RegionFactory;

public class StrategyRegistrationProviderImpl implements StrategyRegistrationProvider {

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final List<StrategyRegistration> strategyRegistrations = new ArrayList<>();
		strategyRegistrations.add(
				new SimpleStrategyRegistrationImpl(RegionFactory.class, NeutrinoSingletonEhCacheRegionFactory.class,
						"hibernate-ehcache-redis-singleton", NeutrinoSingletonEhCacheRegionFactory.class.getName(),
						NeutrinoSingletonEhCacheRegionFactory.class.getSimpleName()));

		return strategyRegistrations;
	}

}
