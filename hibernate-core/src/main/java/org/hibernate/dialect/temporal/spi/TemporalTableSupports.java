/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.spi;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.SPI;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

import static org.hibernate.SPI.Role.USE;
import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;

/// Constructs immutable stock temporal-table profiles without exposing a
/// Hibernate vendor implementation.
///
/// Select [#standard(int, int, boolean)] for ordinary history-table behavior.
/// Select [#mysql(int, boolean)] only when the complete MySQL partition grammar
/// matches, and compose a provider difference with
/// [DelegatingTemporalTableSupport].
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class TemporalTableSupports {
	private TemporalTableSupports() {
	}

	/// Create the standard non-native history-table profile.
	public static TemporalTableSupport standard(
			int temporalColumnType,
			int temporalColumnPrecision,
			boolean tableCheckConstraints) {
		return new StandardTemporalTableSupport(
				temporalColumnType,
				temporalColumnPrecision,
				tableCheckConstraints
		);
	}

	/// Create the MySQL history-table and partitioning profile.
	public static TemporalTableSupport mysql(int temporalColumnPrecision, boolean tableCheckConstraints) {
		return new MySqlTemporalTableSupport( temporalColumnPrecision, tableCheckConstraints );
	}

	private static class StandardTemporalTableSupport implements TemporalTableSupport {
		private final int temporalColumnType;
		private final int temporalColumnPrecision;
		private final boolean tableCheckConstraints;

		private StandardTemporalTableSupport(
				int temporalColumnType,
				int temporalColumnPrecision,
				boolean tableCheckConstraints) {
			this.temporalColumnType = temporalColumnType;
			this.temporalColumnPrecision = temporalColumnPrecision;
			this.tableCheckConstraints = tableCheckConstraints;
		}

		@Override
		public boolean supportsNativeTemporalTables() {
			return false;
		}

		@Override
		public int getTemporalColumnType() {
			return temporalColumnType;
		}

		@Override
		public int getTemporalColumnPrecision() {
			return temporalColumnPrecision;
		}

		@Override
		public String getTemporalTableOptions(TemporalTableDdlRequest request) {
			return null;
		}

		@Override
		public boolean suppressesTemporalTablePrimaryKeys(boolean partitioned) {
			return partitioned && supportsTemporalTablePartitioning();
		}

		@Override
		public boolean supportsTemporalTablePartitioning() {
			return false;
		}

		@Override
		public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(
				TemporalTableDdlRequest request) {
			return List.of();
		}

		@Override
		public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
			return null;
		}

		@Override
		public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
			return strategy != TemporalTableStrategy.NATIVE && tableCheckConstraints;
		}

		@Override
		public String getAsOfOperator(TemporalTableStrategy strategy) {
			return "for system_time as of";
		}

		@Override
		public boolean useAsOfOperator(TemporalTableStrategy strategy) {
			return strategy == TemporalTableStrategy.NATIVE;
		}

		@Override
		public boolean useAsOfOperatorForCurrent(TemporalTableStrategy strategy) {
			return false;
		}

		@Override
		public boolean useTemporalRestriction(TemporalRestrictionRequest request) {
			return switch ( request.strategy() ) {
				case HISTORY_TABLE -> request.temporalIdentifierPresent();
				case NATIVE -> false;
				default -> true;
			};
		}

		@Override
		public String getTemporalExclusionColumnOption() {
			throw new MappingException( "Native temporal exclusion column option is not supported by this dialect" );
		}

		@Override
		public TemporalTableStrategy getDefaultTemporalTableStrategy() {
			return HISTORY_TABLE;
		}
	}

	private static final class MySqlTemporalTableSupport extends StandardTemporalTableSupport {
		private MySqlTemporalTableSupport(int temporalColumnPrecision, boolean tableCheckConstraints) {
			super( SqlTypes.TIMESTAMP_UTC, temporalColumnPrecision, tableCheckConstraints );
		}

		@Override
		public boolean supportsTemporalTablePartitioning() {
			return true;
		}

		@Override
		public String getTemporalTableOptions(TemporalTableDdlRequest request) {
			return request.partitioned()
					? "partition by list (" + request.rowEndColumnName() + "_null)"
							+ " (partition " + request.historyPartitionName() + " values in (0),"
							+ " partition " + request.currentPartitionName() + " values in (1))"
					: null;
		}

		@Override
		public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
			return request.partitioned()
					? request.rowEndColumnName() + "_null tinyint as ("
							+ request.rowEndColumnName() + " is null) virtual invisible"
					: null;
		}
	}
}
