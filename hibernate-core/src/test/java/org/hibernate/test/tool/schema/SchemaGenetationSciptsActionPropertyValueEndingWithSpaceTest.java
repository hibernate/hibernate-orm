/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.tool.schema;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13621")
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

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder(
				bootRegistry,
				cfgRegistryBuilder.getAggregatedCfgXml()
		).applySettings( properties );

		prepareBasicRegistryBuilder( registryBuilder );
		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	@Test
	public void testValueEndingWithSpaceDoesNotCauseExceptionDuringBootstrap() {
	}
}
