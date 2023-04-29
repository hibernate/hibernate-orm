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
public enum SybaseDriverKind {
	JCONNECT,
	JTDS,
	OTHER;

	public static SybaseDriverKind determineKind(DialectResolutionInfo dialectResolutionInfo) {
		final String driverName = dialectResolutionInfo.getDriverName();
		// By default, we assume OTHER
		if ( driverName == null ) {
			return OTHER;
		}
		switch ( driverName ) {
			case "jConnect (TM) for JDBC (TM)":
				return JCONNECT;
			case "jTDS Type 4 JDBC Driver for MS SQL Server and Sybase":
				return JTDS;
			default:
				return OTHER;
		}
	}
}
