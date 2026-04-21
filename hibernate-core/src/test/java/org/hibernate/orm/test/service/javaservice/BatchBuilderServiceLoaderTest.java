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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@link BatchBuilder} supplied via the Java service loader
 * mechanism is picked up by {@code BatchBuilderInitiator} when no explicit
 * {@value org.hibernate.cfg.BatchSettings#BUILDER} setting is configured.
 */
@JiraKey("HHH-18938")
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = BatchBuilder.class,
				impl = BatchBuilderServiceLoaderTest.CustomBatchBuilder.class
		)
)
@ServiceRegistry
public class BatchBuilderServiceLoaderTest {

	@Test
	void testServiceLoaderDiscovery(ServiceRegistryScope scope) {
		final BatchBuilder batchBuilder = scope.getRegistry().requireService( BatchBuilder.class );
		assertThat( batchBuilder ).isInstanceOf( CustomBatchBuilder.class );
	}

	public static class CustomBatchBuilder implements BatchBuilder {
		@Override
		public Batch buildBatch(
				BatchKey key,
				Integer batchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			throw new UnsupportedOperationException( "Not needed for bootstrap test" );
		}
	}
}
