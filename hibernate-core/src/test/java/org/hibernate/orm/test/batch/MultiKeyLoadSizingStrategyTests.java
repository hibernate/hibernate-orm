/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static java.util.Locale.ROOT;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class MultiKeyLoadSizingStrategyTests {
	@Test
	public void testSizes(ServiceRegistryScope scope) {
		scope.withService( JdbcServices.class, (jdbcServices) -> {
			final MultiKeyLoadSizingStrategy sizingStrategy = jdbcServices.getDialect().getBatchLoadSizingStrategy();

			check( 1, 1000, true, sizingStrategy );
			check( 1, 1000, false, sizingStrategy );
		} );
	}

	private void check(int columns, int size, boolean pad, MultiKeyLoadSizingStrategy sizingStrategy) {
		final int value = sizingStrategy.determineOptimalBatchLoadSize( columns, size, pad );
		System.out.printf( ROOT, "(%s, %s, %s) - %s%n", columns, size, pad, value );
	}
}
