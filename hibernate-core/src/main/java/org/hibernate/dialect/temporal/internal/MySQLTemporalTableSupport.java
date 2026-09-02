/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.type.SqlTypes;

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

	@Override
	public int getTemporalColumnType() {
		return SqlTypes.TIMESTAMP_UTC;
	}
}
