/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporal.internal;

import java.util.List;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.type.SqlTypes;

import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;

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
	public String getTemporalTableOptions(TemporalTableDdlRequest request) {
		return request.partitioned()
				? "partition by list (" + request.rowEndColumnName() + ")"
				: null;
	}

	@Override
	public List<TemporalTableAuxiliaryObject> getTemporalTableAuxiliaryObjects(TemporalTableDdlRequest request) {
		if ( request.partitioned() ) {
			final String tableName = request.tableName();
			return List.of(
					new TemporalTableAuxiliaryObject(
							request.currentPartitionName(), TABLE, false,
							List.of( "create table " + request.currentPartitionName()
									+ " partition of " + tableName + " for values in (null)" ),
							List.of( "drop table if exists " + request.currentPartitionName() + " cascade" )
					),
					new TemporalTableAuxiliaryObject(
							request.historyPartitionName(), TABLE, false,
							List.of( "create table " + request.historyPartitionName()
									+ " partition of " + tableName + " default" ),
							List.of( "drop table if exists " + request.historyPartitionName() + " cascade" )
					)
			);
		}
		return List.of();
	}

	@Override
	public int getTemporalColumnType() {
		return SqlTypes.TIMESTAMP_UTC;
	}
}
