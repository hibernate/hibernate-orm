/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import java.util.List;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.temporal.TemporalTableStrategy;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;

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
	public String getExtraTemporalTableDeclarations(TemporalTableDdlRequest request) {
		return request.strategy() == TemporalTableStrategy.NATIVE
				// Transaction id support was only added in SQL Server 2022 (16.x)
				? (dialect.getVersion().isSameOrAfter( 16 )
				? "transaction_start_id bigint generated always as transaction_id start hidden not null, " : "")
				+ "period for system_time (" + request.rowStartColumnName() + ", "
				+ request.rowEndColumnName() + ")"
				: null;
	}

	@Override
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
		return request.strategy() == TemporalTableStrategy.NATIVE
				? "with (system_versioning = on)"
				: null;
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		if ( request.strategy() == TemporalTableStrategy.NATIVE ) {
			return List.of( new TemporalTableAuxiliaryObject(
					"disable-system-versioning",
					TABLE,
					false,
					List.of(),
					List.of( "alter table " + request.tableName() + " set (system_versioning = off)" )
			) );
		}
		return List.of();
	}

}
