/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.List;

import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.DATABASE;
import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;

/// Verifies how declarative temporal auxiliary descriptors are keyed when
/// adapted to the boot model.
///
/// @author Steve Ebersole
public class TemporalHelperTest {
	@Test
	void databaseScopeDeduplicatesWhileTableScopeRemainsIsolated() {
		final var firstTable = request( "orders" );
		final var secondTable = request( "shipments" );
		final var databaseObject = descriptor( DATABASE );
		final var tableObject = descriptor( TABLE );

		assertThat( TemporalHelper.temporalAuxiliaryExportIdentifier( firstTable, databaseObject ) )
				.isEqualTo( TemporalHelper.temporalAuxiliaryExportIdentifier( secondTable, databaseObject ) )
				.isEqualTo( "shared-temporal-object" );
		assertThat( TemporalHelper.temporalAuxiliaryExportIdentifier( firstTable, tableObject ) )
				.isEqualTo( "orders:shared-temporal-object" )
				.isNotEqualTo( TemporalHelper.temporalAuxiliaryExportIdentifier( secondTable, tableObject ) );
	}

	private static TemporalTableDdlRequest request(String tableName) {
		return new TemporalTableDdlRequest(
				TemporalTableStrategy.HISTORY_TABLE,
				tableName,
				"valid_from",
				"valid_to",
				false,
				null,
				null
		);
	}

	private static TemporalTableAuxiliaryObject descriptor(TemporalTableAuxiliaryObject.Scope scope) {
		return new TemporalTableAuxiliaryObject(
				"shared-temporal-object",
				scope,
				false,
				List.of( "create shared temporal object" ),
				List.of( "drop shared temporal object" )
		);
	}
}
