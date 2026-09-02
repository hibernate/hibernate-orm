/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies provider composition of the public temporal-table SPI.
///
/// @author Steve Ebersole
public class ExampleTemporalTableSupportTest {
	@Test
	void suppliesTheProviderOwnedStrategy() {
		assertSame( ExampleTemporalTableSupport.INSTANCE, new ExampleDialect().getTemporalTableSupport() );
	}

	@Test
	void composesTheStandardProfileAndAddsOneTableScopedAuxiliaryObject() {
		final var support = ExampleTemporalTableSupport.INSTANCE;
		final var request = new TemporalTableDdlRequest(
				TemporalTableStrategy.HISTORY_TABLE,
				"fixture_orders",
				"valid_from",
				"valid_to",
				false,
				"ignored_current",
				"ignored_history"
		);

		assertEquals( SqlTypes.TIMESTAMP, support.getTemporalColumnType() );
		assertEquals( 6, support.getTemporalColumnPrecision() );
		assertTrue( support.createTemporalTableCheckConstraint( TemporalTableStrategy.HISTORY_TABLE ) );
		assertEquals( 1, support.getTemporalTableAuxiliaryObjects( request ).size() );
		final var auxiliary = support.getTemporalTableAuxiliaryObjects( request ).get( 0 );
		assertEquals( "fixture-temporal-audit", auxiliary.exportIdentifier() );
		assertSame( TABLE, auxiliary.scope() );
		assertEquals(
				"create fixture temporal audit for fixture_orders",
				auxiliary.createCommands().get( 0 )
		);
		assertEquals(
				"drop fixture temporal audit for fixture_orders",
				auxiliary.dropCommands().get( 0 )
		);
	}
}
