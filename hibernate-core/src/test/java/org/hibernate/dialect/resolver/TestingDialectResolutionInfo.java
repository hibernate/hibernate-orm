/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.resolver;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
* @author Steve Ebersole
*/
public class TestingDialectResolutionInfo implements DialectResolutionInfo {
	private final String databaseName;
	private final int databaseMajorVersion;
	private final int databaseMinorVersion;

	private final String driverName;
	private final int driverMajorVersion;
	private final int driverMinorVersion;

	TestingDialectResolutionInfo(
			String databaseName,
			int databaseMajorVersion,
			int databaseMinorVersion,
			String driverName,
			int driverMajorVersion,
			int driverMinorVersion) {
		this.databaseName = databaseName;
		this.databaseMajorVersion = databaseMajorVersion;
		this.databaseMinorVersion = databaseMinorVersion;
		this.driverName = driverName;
		this.driverMajorVersion = driverMajorVersion;
		this.driverMinorVersion = driverMinorVersion;
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name) {
		return forDatabaseInfo( name, NO_VERSION );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name, int majorVersion) {
		return forDatabaseInfo( name, majorVersion, NO_VERSION );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name, int majorVersion, int minorVersion) {
		return new TestingDialectResolutionInfo( name, majorVersion, minorVersion, null, NO_VERSION, NO_VERSION );
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public int getDatabaseMajorVersion() {
		return databaseMajorVersion;
	}

	@Override
	public int getDatabaseMinorVersion() {
		return databaseMinorVersion;
	}

	@Override
	public String getDriverName() {
		return driverName;
	}

	@Override
	public int getDriverMajorVersion() {
		return driverMajorVersion;
	}

	@Override
	public int getDriverMinorVersion() {
		return driverMinorVersion;
	}
}
