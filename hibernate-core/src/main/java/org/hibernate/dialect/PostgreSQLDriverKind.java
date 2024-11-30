/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 *
 * @author Christian Beikov
 */
public enum PostgreSQLDriverKind {
	PG_JDBC,
	VERT_X,
	OTHER;

	public static PostgreSQLDriverKind determineKind(DialectResolutionInfo dialectResolutionInfo) {
		final String driverName = dialectResolutionInfo.getDriverName();
		// By default we assume PgJDBC
		if ( driverName == null ) {
			return PG_JDBC;
		}
		switch ( driverName ) {
			case "PostgreSQL JDBC Driver":
				return PG_JDBC;
		}
		return OTHER;
	}
}
