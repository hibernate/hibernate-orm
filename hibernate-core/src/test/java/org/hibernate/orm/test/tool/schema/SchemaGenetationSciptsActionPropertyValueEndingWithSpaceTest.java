/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13621")
public class SchemaGenetationSciptsActionPropertyValueEndingWithSpaceTest extends BaseCoreFunctionalTestCase {

	private File dropOutput;
	private File createOutput;

	@Override
	protected StandardServiceRegistryImpl buildServiceRegistry(
			BootstrapServiceRegistry bootRegistry,
			Configuration configuration) {
		try {
			dropOutput = File.createTempFile( "drop_script", ".sql" );
			createOutput = File.createTempFile( "create_script", ".sql" );
			dropOutput.deleteOnExit();
			createOutput.deleteOnExit();
		}
		catch (IOException e) {
			fail( "unable to create temp file" + e );
		}
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		// the value of the property ends with a space
		properties.setProperty( "javax.persistence.schema-generation.scripts.action", "drop-and-create " );
		properties.setProperty(
				"javax.persistence.schema-generation.scripts.create-target",
				createOutput.getAbsolutePath()
		);
		properties.setProperty(
				"javax.persistence.schema-generation.scripts.drop-target",
				dropOutput.getAbsolutePath()
		);
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder cfgRegistryBuilder = configuration.getStandardServiceRegistryBuilder();

		StandardServiceRegistryBuilder registryBuilder = ServiceRegistryUtil.applySettings(
				new StandardServiceRegistryBuilder(
						bootRegistry,
						cfgRegistryBuilder.getAggregatedCfgXml()
				)
		).applySettings( properties );

		prepareBasicRegistryBuilder( registryBuilder );
		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	@Test
	public void testValueEndingWithSpaceDoesNotCauseExceptionDuringBootstrap() {
	}
}
