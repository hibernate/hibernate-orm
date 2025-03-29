/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.jpa.MyNamingStrategy;
import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gail Badner
 */
@BaseUnitTest
public class NamingStrategyConfigurationTest {

	@Test
	public void testNamingStrategyFromProperty() {

		// configure NamingStrategy
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			Map<String, Object> settings = ServiceRegistryUtil.createBaseSettings();
			settings.put( AvailableSettings.PHYSICAL_NAMING_STRATEGY, MyNamingStrategy.class.getName() );
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					settings
			);
			final EntityManagerFactory emf = builder.build();
			try {
				assertEquals(
						MyNamingStrategy.class.getName(),
						builder.getConfigurationValues().get( AvailableSettings.PHYSICAL_NAMING_STRATEGY )
				);

				assertTyping(
						MyNamingStrategy.class,
						builder.getMetadata().getMetadataBuildingOptions().getPhysicalNamingStrategy()
				);
			}
			finally {
				if ( emf != null ) {
					emf.close();
				}
			}
		}
	}
}
