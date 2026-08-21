/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temporal;

import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.temporal.MySQLTemporalTableSupport;
import org.hibernate.temporal.TemporalTableStrategy;

/**
 * @author Daniël van Eeden
 */
public class TiDBTemporalTableSupport extends MySQLTemporalTableSupport {

	public TiDBTemporalTableSupport(TiDBDialect dialect) {
		super( dialect );
	}

	@Override
	public String getExtraTemporalTableDeclarations(
			TemporalTableStrategy strategy,
			String rowStartColumn, String rowEndColumn,
			boolean partitioned) {
		// TiDB does not support the INVISIBLE keyword on generated columns used by MySQLTemporalTableSupport
		// See https://github.com/pingcap/tidb/issues/59233
		return partitioned
				? rowEndColumn + "_null tinyint as (" + rowEndColumn + " is null) virtual"
				: null;
	}
}
