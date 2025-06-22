/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.DatabaseMetaData;

/**
 * Enum interpretation of the valid values from {@link DatabaseMetaData#getSQLStateType()}
 *
 * @author Steve Ebersole
 */
public enum SQLStateType {
	/**
	 * The reported codes follow the X/Open spec
	 */
	XOpen,
	/**
	 * The reported codes follow the SQL spec
	 */
	SQL99,
	/**
	 * It is unknown.  Might follow another spec completely, or be a mixture.
	 */
	UNKNOWN;


	public static SQLStateType interpretReportedSQLStateType(int sqlStateType) {
		switch ( sqlStateType ) {
			case DatabaseMetaData.sqlStateSQL99 : {
				return SQL99;
			}
			case DatabaseMetaData.sqlStateXOpen : {
				return XOpen;
			}
			default : {
				return UNKNOWN;
			}
		}
	}
}
