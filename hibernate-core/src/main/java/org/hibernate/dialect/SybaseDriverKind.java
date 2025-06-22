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
