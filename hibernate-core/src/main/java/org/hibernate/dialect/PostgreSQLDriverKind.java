/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
