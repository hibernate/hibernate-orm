/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public static TestingDialectResolutionInfo forDatabaseInfo(String databaseName, String driverName, int majorVersion, int minorVersion) {
		return new TestingDialectResolutionInfo( databaseName, majorVersion, minorVersion, driverName, NO_VERSION, NO_VERSION );
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
