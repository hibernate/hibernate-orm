/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.service;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ServiceContributorTest extends BaseUnitTestCase {
	@Test
	public void overrideInitiator() {
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();

		class MyRegionFactoryInitiator extends RegionFactoryInitiator {
			private boolean called = false;

			@Override
			public RegionFactory initiateService(
					Map configurationValues,
					ServiceRegistryImplementor registry) {
				called = true;
				return super.initiateService( configurationValues, registry );
			}
		}

		final MyRegionFactoryInitiator initiator = new MyRegionFactoryInitiator();

		ssrb.addInitiator( initiator );

		final ServiceRegistryImplementor registry = (ServiceRegistryImplementor) ssrb.build();
		try {
			registry.getService( RegionFactory.class );
			assertTrue( initiator.called );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}
}
