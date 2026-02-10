/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.mapping.Table;

import static java.util.Collections.emptySet;

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
	public String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned) {
		// no 'for' keyword
		if ( strategy == TemporalTableStrategy.NATIVE ) {
			return "transaction_start_id timestamp(12) not null generated always as transaction start id implicitly hidden"
					+ ", period system_time (" + rowStartColumn + ", " + rowEndColumn + ")";
		}
		else if ( partitioned ) {
			return rowEndColumn + "_null smallint generated always as (case when " + rowEndColumn + " is null then 1 else 0 end) implicitly hidden";
		}
		else {
			return null;
		}
	}

	@Override
	public String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartition,
			String historyPartition) {
		return partitioned
				? "partition by range (" + rowEndColumnName + "_null)"
						+ " (partition " + historyPartition + " starting from (0) ending at (0),"
						+ " partition " + currentPartition + " starting from (1) ending at (1))"
				: null;
	}

	@Override
	public void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table, Database database,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		if ( strategy == TemporalTableStrategy.NATIVE ) {
			final String name = table.getQuotedName( dialect );
			final String historyTableName = name + "_history";
			database.addAuxiliaryDatabaseObject(
					new NamedAuxiliaryDatabaseObject(
							historyTableName,
							database.getDefaultNamespace(),
							new String[] {
									"create table " + historyTableName + " like " + name,
									"alter table " + name + " add versioning use history table " + historyTableName,
							},
							new String[] {"drop table " + historyTableName},
							emptySet()
					)
			);
		}
	}

	@Override
	public TemporalTableStrategy getDefaultTemporalTableStrategy() {
		return TemporalTableStrategy.NATIVE;
	}

}
