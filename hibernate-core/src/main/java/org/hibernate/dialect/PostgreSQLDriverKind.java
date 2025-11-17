/*
 * SPDX-License-Identifier: Apache-2.0
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

	public static PostgreSQLDriverKind determineKind(DialectResolutionInfo info) {
		final String driverName = info.getDriverName();
		// By default we assume PgJDBC
		if ( driverName == null ) {
			return PG_JDBC;
		}
		else {
			return switch ( driverName ) {
				case "PostgreSQL JDBC Driver" -> PG_JDBC;
				default -> OTHER;
			};
		}
	}
}
