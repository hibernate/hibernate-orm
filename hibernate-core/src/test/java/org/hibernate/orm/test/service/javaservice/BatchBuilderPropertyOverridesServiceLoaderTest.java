/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.javaservice;

import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that an explicitly configured {@value org.hibernate.cfg.BatchSettings#BUILDER}
 * takes precedence over a {@link BatchBuilder} registered via Java service loader.
 */
@JiraKey("HHH-18938")
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = BatchBuilder.class,
				impl = BatchBuilderPropertyOverridesServiceLoaderTest.ServiceLoaderBatchBuilder.class
		)
)
@ServiceRegistry(
		settings = @Setting(
				name = "hibernate.jdbc.batch.builder",
				value = "org.hibernate.orm.test.service.javaservice.BatchBuilderPropertyOverridesServiceLoaderTest$PropertyBatchBuilder"
		)
)
public class BatchBuilderPropertyOverridesServiceLoaderTest {

	@Test
	void testPropertyWinsOverServiceLoader(ServiceRegistryScope scope) {
		final BatchBuilder batchBuilder = scope.getRegistry().requireService( BatchBuilder.class );
		assertThat( batchBuilder ).isInstanceOf( PropertyBatchBuilder.class );
	}

	public static class PropertyBatchBuilder implements BatchBuilder {
		@Override
		public Batch buildBatch(
				BatchKey key,
				Integer batchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			throw new UnsupportedOperationException();
		}
	}

	public static class ServiceLoaderBatchBuilder implements BatchBuilder {
		@Override
		public Batch buildBatch(
				BatchKey key,
				Integer batchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			throw new UnsupportedOperationException();
		}
	}
}
