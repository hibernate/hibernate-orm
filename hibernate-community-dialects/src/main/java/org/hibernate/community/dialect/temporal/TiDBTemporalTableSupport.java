/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.temporal;

import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.temporal.spi.DelegatingTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableSupports;

/**
 * @author Daniël van Eeden
 */
public class TiDBTemporalTableSupport extends DelegatingTemporalTableSupport {

	public TiDBTemporalTableSupport(TiDBDialect dialect) {
		super( TemporalTableSupports.mysql(
				dialect.getTypeSizingProfile().defaultTimestampPrecision(),
				dialect.getCheckConstraintSupport().supports( org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE )
		) );
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		// TiDB does not support the INVISIBLE keyword on generated columns used by MySQLTemporalTableSupport
		// See https://github.com/pingcap/tidb/issues/59233
		return request.partitioned()
				? request.rowEndColumnName() + "_null tinyint as ("
						+ request.rowEndColumnName() + " is null) virtual"
				: null;
	}
}
