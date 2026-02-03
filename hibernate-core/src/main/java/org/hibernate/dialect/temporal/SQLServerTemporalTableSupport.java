/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.mapping.Table;

import static java.util.Collections.emptySet;

/**
 * @author Gavin King
 */
public class SQLServerTemporalTableSupport extends DefaultTemporalTableSupport {

	public SQLServerTemporalTableSupport(SQLServerDialect dialect) {
		super( dialect );
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return true;
	}

	@Override
	public String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned) {
		return strategy == TemporalTableStrategy.NATIVE
				? "transaction_start_id bigint generated always as transaction_id start hidden not null"
				+ ", period for system_time (" + rowStartColumn + ", " + rowEndColumn + ")"
				: null;
	}

	@Override
	public String getTemporalTableOptions(
			TemporalTableStrategy strategy,
			String rowEndColumnName,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		return strategy == TemporalTableStrategy.NATIVE
				? "with (system_versioning = on)"
				: null;
	}

	@Override
	public void addTemporalTableAuxiliaryObjects(
			TemporalTableStrategy strategy,
			Table table,
			Database database,
			boolean partitioned,
			String currentPartitionName,
			String historyPartitionName) {
		if ( strategy == TemporalTableStrategy.NATIVE ) {
			database.addAuxiliaryDatabaseObject( new SimpleAuxiliaryDatabaseObject(
					database.getDefaultNamespace(),
					new String[0],
					new String[] { "alter table " + table.getQuotedName(dialect) + " set (system_versioning = off)" },
					emptySet(),
					false
			) );
		}
	}

}
