/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.javaservice;

import java.util.Map;

import org.hibernate.tool.schema.spi.ExtractionTool;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.SchemaValidator;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@link SchemaManagementTool} supplied via the Java service
 * loader mechanism is picked up by {@code SchemaManagementToolInitiator} when
 * no explicit {@value org.hibernate.cfg.SchemaToolingSettings#SCHEMA_MANAGEMENT_TOOL}
 * setting is configured.
 */
@JiraKey("HHH-18938")
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = SchemaManagementTool.class,
				impl = SchemaManagementToolServiceLoaderTest.CustomSchemaManagementTool.class
		)
)
@ServiceRegistry
public class SchemaManagementToolServiceLoaderTest {

	@Test
	void testServiceLoaderDiscovery(ServiceRegistryScope scope) {
		final SchemaManagementTool tool = scope.getRegistry().requireService( SchemaManagementTool.class );
		assertThat( tool ).isInstanceOf( CustomSchemaManagementTool.class );
	}

	public static class CustomSchemaManagementTool implements SchemaManagementTool {
		@Override
		public SchemaCreator getSchemaCreator(Map<String, Object> options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SchemaDropper getSchemaDropper(Map<String, Object> options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SchemaMigrator getSchemaMigrator(Map<String, Object> options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SchemaValidator getSchemaValidator(Map<String, Object> options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget) {
		}

		@Override
		public ExtractionTool getExtractionTool() {
			return null;
		}
	}
}
