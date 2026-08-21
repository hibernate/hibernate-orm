/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service.javaservice;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@link MutationExecutorService} supplied via the Java service
 * loader mechanism is picked up by {@code MutationExecutorServiceInitiator}
 * when no explicit {@code hibernate.jdbc.mutation.executor} setting is configured.
 */
@JiraKey("HHH-18938")
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = MutationExecutorService.class,
				impl = MutationExecutorServiceLoaderTest.CustomMutationExecutorService.class
		)
)
@ServiceRegistry
public class MutationExecutorServiceLoaderTest {

	@Test
	void testServiceLoaderDiscovery(ServiceRegistryScope scope) {
		final MutationExecutorService service =
				scope.getRegistry().requireService( MutationExecutorService.class );
		assertThat( service ).isInstanceOf( CustomMutationExecutorService.class );
	}

	public static class CustomMutationExecutorService implements MutationExecutorService {
		@Override
		public MutationExecutor createExecutor(
				BatchKeyAccess batchKeySupplier,
				MutationOperationGroup operationGroup,
				SharedSessionContractImplementor session) {
			throw new UnsupportedOperationException( "Not needed for bootstrap test" );
		}
	}
}
