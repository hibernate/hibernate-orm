/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal;

import org.hibernate.cfg.TemporalTableStrategy;
import org.hibernate.dialect.MySQLDialect;

/**
 * @author Gavin King
 */
public class MySQLTemporalTableSupport extends DefaultTemporalTableSupport {

	public MySQLTemporalTableSupport(MySQLDialect dialect) {
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
			String currentPartition,
			String historyPartition) {
		return partitioned
				? "partition by list (" + rowEndColumnName + "_null)"
				+ " (partition " + historyPartition + " values in (0),"
				+ " partition " + currentPartition + " values in (1))"
				: null;
	}

	@Override
	public String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned) {
		return partitioned
				? rowEndColumn + "_null tinyint as (" + rowEndColumn + " is null) virtual invisible"
				: null;
	}


}
