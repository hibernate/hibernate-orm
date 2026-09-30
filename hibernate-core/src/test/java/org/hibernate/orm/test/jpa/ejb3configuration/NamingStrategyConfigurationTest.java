/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import org.hibernate.boot.pipeline.internal.source.PersistenceUnitSources;
import java.util.Map;

import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
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
	/// Unconfigured boot and both JPA aliases must select the JPA implementation,
	/// rather than its superclass with Hibernate-specific naming conventions.
	@Test
	public void testJpaImplicitNamingIsDefault() {
		for ( String selection : new String[] { null, "default", "jpa" } ) {
			var builder = ServiceRegistryUtil.serviceRegistryBuilder();
			if ( selection != null ) {
				builder.applySetting( AvailableSettings.IMPLICIT_NAMING_STRATEGY, selection );
			}
			try ( var registry = builder.build() ) {
				var options = new MappingResolutionOptionsImpl( registry );
				assertEquals( ImplicitNamingStrategyJpaCompliantImpl.class, options.getImplicitNamingStrategy().getClass() );
				assertEquals( PhysicalNamingStrategyStandardImpl.class, options.getPhysicalNamingStrategy().getClass() );
			}
		}
	}

	@Test
	public void testNamingStrategyFromProperty() {

		// configure NamingStrategy
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			Map<String, Object> settings = ServiceRegistryUtil.createBaseSettings();
			settings.put( AvailableSettings.PHYSICAL_NAMING_STRATEGY, MyNamingStrategy.class.getName() );
			try (var metadataResolution = BootstrapPipeline.resolveMetadata(
					PersistenceUnitSources.container( new PersistenceUnitInfoDescriptor( adapter ) ),
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
