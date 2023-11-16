/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect.resolver;

import java.util.Collections;
import java.util.Map;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * DialectResolutionInfo implementation used in testing
*/
public class TestingDialectResolutionInfo implements DialectResolutionInfo {
	private final String databaseName;
	private final int databaseMajorVersion;
	private final int databaseMinorVersion;

	private final String driverName;
	private final int driverMajorVersion;
	private final int driverMinorVersion;

	private final Map<String, Object> configurationValues;

	TestingDialectResolutionInfo(
			String databaseName,
			int databaseMajorVersion,
			int databaseMinorVersion,
			String driverName,
			int driverMajorVersion,
			int driverMinorVersion,
			Map<String, Object> configurationValues) {
		this.databaseName = databaseName;
		this.databaseMajorVersion = databaseMajorVersion;
		this.databaseMinorVersion = databaseMinorVersion;
		this.driverName = driverName;
		this.driverMajorVersion = driverMajorVersion;
		this.driverMinorVersion = driverMinorVersion;
		this.configurationValues = configurationValues;
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name) {
		return forDatabaseInfo( name, NO_VERSION );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name, int majorVersion) {
		return forDatabaseInfo( name, majorVersion, NO_VERSION );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String name, int majorVersion, int minorVersion) {
		return forDatabaseInfo( name, null, majorVersion, minorVersion );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(String databaseName, String driverName, int majorVersion, int minorVersion) {
		return forDatabaseInfo( databaseName, driverName, majorVersion, minorVersion, Collections.emptyMap() );
	}

	public static TestingDialectResolutionInfo forDatabaseInfo(
			String databaseName,
			String driverName,
			int majorVersion,
			int minorVersion,
			Map<String, Object> configurationValues) {
		return new TestingDialectResolutionInfo(
				databaseName,
				majorVersion,
				minorVersion,
				driverName,
				NO_VERSION,
				NO_VERSION,
				configurationValues
		);
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public String getDatabaseVersion() {
		if ( databaseMajorVersion == NO_VERSION ) {
			if ( databaseMinorVersion == NO_VERSION ) {
				return null;
			}
			return Integer.toString( databaseMinorVersion );
		}
		else {
			if ( databaseMinorVersion == NO_VERSION ) {
				return Integer.toString( databaseMajorVersion );
			}
			return databaseMajorVersion + "." + databaseMinorVersion;
		}
	}

	@Override
	public int getDatabaseMajorVersion() {
		return databaseMajorVersion == NO_VERSION ? 0 : databaseMajorVersion;
	}

	@Override
	public int getDatabaseMinorVersion() {
		return databaseMinorVersion == NO_VERSION ? 0 : databaseMinorVersion;
	}

	@Override
	public String getDriverName() {
		return driverName;
	}

	@Override
	public int getDriverMajorVersion() {
		return driverMajorVersion == NO_VERSION ? 0 : driverMajorVersion;
	}

	@Override
	public int getDriverMinorVersion() {
		return driverMinorVersion == NO_VERSION ? 0 : driverMinorVersion;
	}

	@Override
	public String getSQLKeywords() {
		return "";
	}

	@Override
	public String toString() {
		return getMajor() + "." + getMinor();
	}

	@Override
	public Map<String, Object> getConfigurationValues() {
		return configurationValues;
	}
}
