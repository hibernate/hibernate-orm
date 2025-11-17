/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.type.SqlTypes;

/**
 * SQL Server specific local temporary table strategy.
 */
public class SQLServerLocalTemporaryTableStrategy extends TransactSQLLocalTemporaryTableStrategy {

	public static final SQLServerLocalTemporaryTableStrategy INSTANCE = new SQLServerLocalTemporaryTableStrategy();

	@Override
	public String getCreateTemporaryTableColumnAnnotation(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case SqlTypes.CHAR, SqlTypes.VARCHAR, SqlTypes.CLOB, SqlTypes.NCHAR, SqlTypes.NVARCHAR, SqlTypes.NCLOB ->
					"collate database_default";
			default -> "";
		};
	}

}
