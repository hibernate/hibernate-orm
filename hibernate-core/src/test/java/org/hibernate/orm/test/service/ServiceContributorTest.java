/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class ServiceContributorTest {

	@Test
	public void overrideCachingInitiator() {
		var ssrb = ServiceRegistryUtil.serviceRegistryBuilder();
		ssrb.clearSettings();

		final var initiator = new MyRegionFactoryInitiator();
		ssrb.addInitiator( initiator );

		final var registry = (ServiceRegistryImplementor) ssrb.build();
		try {
			final RegionFactory regionFactory = registry.getService( RegionFactory.class );
			assertThat( initiator.called ).isTrue();
			assertThat( regionFactory ).isInstanceOf( MyRegionFactory.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	public void overrideCachingInitiatorExplicitSet() {
		var ssrb = ServiceRegistryUtil.serviceRegistryBuilder();

		final var initiator = new MyRegionFactoryInitiator();
		ssrb.addInitiator( initiator );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, new MyRegionFactory() );

		final var registry = (ServiceRegistryImplementor) ssrb.build();
		try {
			registry.getService( RegionFactory.class );
			assertThat( initiator.called ).isFalse();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	static class MyRegionFactoryInitiator extends RegionFactoryInitiator {
		private boolean called = false;

		@Override
		protected RegionFactory getFallback(
				Map<?, ?> configurationValues,
				ServiceRegistryImplementor registry) {
			called = true;
			return new MyRegionFactory();
		}

//			@Override
//			public RegionFactory initiateService(
//					Map configurationValues,
//					ServiceRegistryImplementor registry) {
//				called = true;
//				return super.initiateService( configurationValues, registry );
//			}
	}

	static class MyRegionFactory extends CachingRegionFactory {
	}

}
