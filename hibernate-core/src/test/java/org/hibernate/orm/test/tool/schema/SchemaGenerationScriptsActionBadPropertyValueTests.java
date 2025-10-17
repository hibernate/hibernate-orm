/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.fail;


/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13621")
@ServiceRegistryFunctionalTesting
@SessionFactory
public class SchemaGenerationScriptsActionBadPropertyValueTests implements ServiceRegistryProducer {

	@Test
	public void testValueEndingWithSpaceDoesNotCauseExceptionDuringBootstrap(ServiceRegistryScope registryScope) {
		registryScope.getRegistry();
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		try {
			var dropOutput = File.createTempFile( "drop_script", ".sql" );
			var createOutput = File.createTempFile( "create_script", ".sql" );
			dropOutput.deleteOnExit();
			createOutput.deleteOnExit();

			builder.applySetting(
					"javax.persistence.schema-generation.scripts.action",
					// note the space...
					"drop-and-create "
			);
			builder.applySetting(
					"javax.persistence.schema-generation.scripts.create-target",
					createOutput.getAbsolutePath()
			);
			builder.applySetting(
					"javax.persistence.schema-generation.scripts.drop-target",
					dropOutput.getAbsolutePath()
			);
		}
		catch (IOException e) {
			fail( "unable to create temp file" + e );
		}
		return builder.build();
	}
}
