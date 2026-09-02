/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

/**
 * @author Gavin King
 */
public class MariaDBTemporalTableSupport extends MySQLTemporalTableSupport {

	public MariaDBTemporalTableSupport(MariaDBDialect dialect) {
		super( dialect );
	}

	@Override
	public boolean supportsNativeTemporalTables() {
		return true;
	}

	@Override
	public boolean supportsTemporalTablePartitioning() {
		return false;
	}

	@Override
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
		return request.strategy() == TemporalTableStrategy.NATIVE
				? "with system versioning"
				: null;
	}

	@Override
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		return request.strategy() == TemporalTableStrategy.NATIVE
				? "period for system_time (" + request.rowStartColumnName() + ", "
						+ request.rowEndColumnName() + ")"
				: null;
	}

	@Override
	public int getTemporalColumnType() {
		return SqlTypes.TIMESTAMP_WITH_TIMEZONE;
	}

	@Override
	public String getTemporalExclusionColumnOption() {
		return "without system versioning";
	}

	@Override
	public TemporalTableStrategy getDefaultTemporalTableStrategy() {
		return TemporalTableStrategy.NATIVE;
	}

}
