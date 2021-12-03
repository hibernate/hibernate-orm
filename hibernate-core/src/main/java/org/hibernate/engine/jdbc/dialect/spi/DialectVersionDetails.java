/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * @author Steve Ebersole
 */
public interface DialectVersionDetails {
	/**
	 * Constant used to indicate that no version is defined
	 */
	int NO_VERSION = -9999;

	/**
	 * Obtain access to the database major version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()} for the target database.
	 *
	 * @return The database major version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
	 */
	int getDatabaseMajorVersion();

	/**
	 * Obtain access to the database minor version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()} for the target database.
	 *
	 * @return The database minor version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
	 */
	int getDatabaseMinorVersion();

	/**
	 * Obtain access to the major version of the JDBC driver, as returned from
	 * {@link java.sql.DatabaseMetaData#getDriverMajorVersion()} ()} for the target database.
	 *
	 * @return The JDBC driver major version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDriverMajorVersion()
	 */
	int getDriverMajorVersion();

	/**
	 * Obtain access to the minor version of the JDBC driver, as returned from
	 * {@link java.sql.DatabaseMetaData#getDriverMinorVersion()} for the target database.
	 *
	 * @return The JDBC driver minor version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDriverMinorVersion()
	 */
	int getDriverMinorVersion();

	default boolean isAfterDatabaseVersion(DialectVersionDetails other) {
		return isAfterDatabaseVersion( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	default boolean isAfterDatabaseVersion(int major, int minor) {
		return getDatabaseMajorVersion() > major
				|| ( getDatabaseMajorVersion() == major && getDatabaseMinorVersion() > minor );
	}

	default boolean isAfterDriverVersion(DialectVersionDetails other) {
		return isAfterDriverVersion( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	default boolean isAfterDriverVersion(int major, int minor) {
		return getDriverMajorVersion() > major
				|| ( getDriverMajorVersion() == major && getDriverMinorVersion() > minor );
	}
}
