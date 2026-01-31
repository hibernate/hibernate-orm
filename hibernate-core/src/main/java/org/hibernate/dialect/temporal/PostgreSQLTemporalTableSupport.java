/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;

import static java.util.Collections.emptySet;

/**
 * @author Gavin King
 */
public class PostgreSQLTemporalTableSupport extends DefaultTemporalTableSupport {

	public PostgreSQLTemporalTableSupport(PostgreSQLDialect dialect) {
		super( dialect );
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return true;
	}

	@Override
	public String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		return partitioned
				? "partition by list (" + rowEndColumnName + ")"
				: null;
	}

	@Override
	public void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table,
			Database database,
			boolean partitioned,
			String currentPartition,
			String historyPartition) {
		if ( partitioned ) {
			final String tableName = table.getQuotedName( dialect );
			database.addAuxiliaryDatabaseObject( new NamedAuxiliaryDatabaseObject(
					currentPartition,
					database.getDefaultNamespace(),
					"create table " + currentPartition + " partition of " + tableName + " for values in (null)",
					"drop table if exists " + currentPartition + " cascade",
					emptySet()
			) );
			database.addAuxiliaryDatabaseObject( new NamedAuxiliaryDatabaseObject(
					historyPartition,
					database.getDefaultNamespace(),
					"create table " + historyPartition + " partition of " + tableName + " default",
					"drop table if exists " + historyPartition + " cascade",
					emptySet()
			) );
		}
	}

	@Override
	public int getTemporalColumnType() {
		return SqlTypes.TIMESTAMP_UTC;
	}
}
