/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;

/**
 * @author Gavin King
 */
public class DB2TemporalTableSupport extends DefaultTemporalTableSupport {

	public DB2TemporalTableSupport(DB2Dialect dialect) {
		super( dialect );
	}

	@Override
	public int getTemporalColumnPrecision() {
		return 12; // required!
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return true;
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return true;
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		// no 'for' keyword
		if ( request.strategy() == TemporalTableStrategy.NATIVE ) {
			return "transaction_start_id timestamp(12) not null generated always as transaction start id implicitly hidden"
					+ ", period system_time (" + request.rowStartColumnName() + ", "
					+ request.rowEndColumnName() + ")";
		}
		else if ( request.partitioned() ) {
			return request.rowEndColumnName() + "_null smallint generated always as (case when "
					+ request.rowEndColumnName() + " is null then 1 else 0 end) implicitly hidden";
		}
		else {
			return null;
		}
	}

	@Override
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
		return request.partitioned()
				? "partition by range (" + request.rowEndColumnName() + "_null)"
						+ " (partition " + request.historyPartitionName() + " starting from (0) ending at (0),"
						+ " partition " + request.currentPartitionName() + " starting from (1) ending at (1))"
				: null;
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		if ( request.strategy() == TemporalTableStrategy.NATIVE ) {
			final String name = request.tableName();
			final String historyTableName = name + "_history";
			return List.of(
					new TemporalTableAuxiliaryObject(
							historyTableName,
							TABLE,
							false,
							List.of(
									"create table " + historyTableName + " like " + name,
									"alter table " + name + " add versioning use history table " + historyTableName
							),
							List.of( "drop table " + historyTableName )
					)
			);
		}
		return List.of();
	}

	@Override
	public TemporalTableStrategy getDefaultTemporalTableStrategy() {
		return TemporalTableStrategy.NATIVE;
	}

}
