/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import java.util.List;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.temporal.spi.TemporalRestrictionRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.DATABASE;
import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;
import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;

/**
 * @author Gavin King
 */
public class OracleTemporalTableSupport extends DefaultTemporalTableSupport {

	public OracleTemporalTableSupport(OracleDialect dialect) {
		super( dialect );
	}

	/**
	 * Return {@code false} because we use {@code period for system_time}
	 * to implement the constraint on Oracle.
	 */
	@Override
	public boolean createTemporalTableCheckConstraint(TemporalTableStrategy strategy) {
		return false;
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		return "period for system_time (" + request.rowStartColumnName() + ", "
				+ request.rowEndColumnName() + ")";
	}

	@Override
	public String getAsOfOperator(TemporalTableStrategy strategy) {
		return strategy == TemporalTableStrategy.NATIVE
				? "as of timestamp"
				: "as of period for system_time";
	}

	@Override
	public boolean useAsOfOperator(TemporalTableStrategy strategy) {
		return strategy != HISTORY_TABLE;
	}

	@Override
	public boolean useAsOfOperatorForCurrent(TemporalTableStrategy strategy) {
		return strategy != HISTORY_TABLE;
	}

	@Override
	public boolean useTemporalRestriction(TemporalRestrictionRequest request) {
		return request.instantChangesetIdentifier()
				? request.strategy() == HISTORY_TABLE && request.temporalIdentifierPresent()
				: super.useTemporalRestriction( request );
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return true;
	}

	@Override
	public boolean suppressesTemporalTablePrimaryKeys(boolean partitioned) {
		return false;
	}

	@Override
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
		if ( request.strategy() == TemporalTableStrategy.NATIVE ) {
			return "flashback archive fba_history";
		}
		else if ( request.partitioned() ) {
			return "partition by list( " + request.rowEndColumnName() + ")"
				+ " (partition " + request.currentPartitionName() + " values (null),"
				+ " partition " + request.historyPartitionName() + " values (default))"
				+ " enable row movement";
		}
		else {
			return null;
		}
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return true;
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		if ( request.strategy() == TemporalTableStrategy.NATIVE ) {
			return List.of(
					new TemporalTableAuxiliaryObject(
							"disable-flashback-archive",
							TABLE,
							false,
							List.of(),
							List.of( "alter table " + request.tableName() + " no flashback archive" )
					),
					new TemporalTableAuxiliaryObject(
							"fba_history",
							DATABASE,
							true,
							List.of( "create flashback archive fba_history tablespace users quota 1M retention 1 month" ),
							List.of( "drop flashback archive fba_history" )
					)
			);
		}
		return List.of();
	}

}
