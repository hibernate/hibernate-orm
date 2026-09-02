/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that TiDB composes the public MySQL temporal-table profile and
/// overrides only its generated-column declaration.
///
/// @author Steve Ebersole
public class TemporalTableSupportTest {
	@Test
	void tiDBPreservesTheStockProfileExceptForInvisibleGeneratedColumns() {
		final var dialect = new TiDBDialect();
		final var support = dialect.getTemporalTableSupport();
		final var request = new TemporalTableDdlRequest(
				TemporalTableStrategy.HISTORY_TABLE,
				"orders",
				"valid_from",
				"valid_to",
				true,
				"orders_current",
				"orders_history"
		);

		assertThat( support.getTemporalColumnType() ).isEqualTo( SqlTypes.TIMESTAMP_UTC );
		assertThat( support.getTemporalColumnPrecision() )
				.isEqualTo( dialect.getTypeSizingProfile().defaultTimestampPrecision() );
		assertThat( support.supportsTemporalTablePartitioning() ).isTrue();
		assertThat( support.getTemporalTableOptions( request ) ).isEqualTo(
				"partition by list (valid_to_null) (partition orders_history values in (0),"
						+ " partition orders_current values in (1))"
		);
		assertThat( support.getExtraTemporalTableDeclarations( request ) )
				.isEqualTo( "valid_to_null tinyint as (valid_to is null) virtual" );
		assertThat( support.getTemporalTableAuxiliaryObjects( request ) ).isEmpty();
	}
}
