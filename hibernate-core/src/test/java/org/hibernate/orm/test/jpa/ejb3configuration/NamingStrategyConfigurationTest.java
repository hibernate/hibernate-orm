/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import java.util.Map;

import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
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
			try (var metadataResolution = BootstrapPipeline.resolveMetadata(
					new PersistenceUnitInfoDescriptor( adapter ),
					settings
			)) {
				assertEquals(
						MyNamingStrategy.class.getName(),
						metadataResolution.configurationValues().get( AvailableSettings.PHYSICAL_NAMING_STRATEGY )
				);

				assertTyping(
						MyNamingStrategy.class,
						metadataResolution.metadata().getMappingResolutionOptions().getPhysicalNamingStrategy()
				);
			}
		}
	}
}
