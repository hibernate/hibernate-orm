/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.DatabaseVersion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies the provider Dialect construction, identity, rendering, and
/// strategy-supply contracts at the assembled-artifact boundary.
///
/// @author Steve Ebersole
public class ExampleDialectConstructionTest {
	@Test
	void capturesTheExplicitVersionAndRetainsStableDiagnostics() {
		final DatabaseVersion version = DatabaseVersion.make( 3, 2, 1 );
		final ExampleDialect dialect = new ExampleDialect( version );

		assertSame( version, dialect.getVersion() );
		assertEquals( ExampleDialect.class.getName() + ", version: 3.2.1", dialect.toString() );
	}

	@Test
	void usesTheProviderMinimumAndFocusedRenderingContracts() {
		final ExampleDialect dialect = new ExampleDialect();

		assertEquals( 1, dialect.getVersion().getMajor() );
		assertEquals( '[', dialect.openQuote() );
		assertEquals( ']', dialect.closeQuote() );
	}

	@Test
	void suppliesTheProviderMultiKeySizingStrategy() {
		final ExampleDialect dialect = new ExampleDialect();

		assertSame( ExampleMultiKeyLoadSizingStrategy.INSTANCE, dialect.getMultiKeyLoadSizingStrategy() );
		assertEquals(
				250,
				dialect.getMultiKeyLoadSizingStrategy().determineOptimalBatchLoadSize( 4, 600, false )
		);
		assertSame( ExampleMultiKeyLoadSizingStrategy.INSTANCE, dialect.getBatchLoadSizingStrategy() );
	}
}
