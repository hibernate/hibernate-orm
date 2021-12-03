/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

/**
 * Details about the underlying database, as understood by a Dialect.
 *
 * Also used in conjunction with {@link org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo}
 * to help resolve the Dialect to use.
 */
public interface DatabaseVersion {
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

	default boolean isAfter(DatabaseVersion other) {
		return isAfter( other.getDatabaseMajorVersion(), other.getDatabaseMinorVersion() );
	}

	default boolean isAfter(int major, int minor) {
		return getDatabaseMajorVersion() > major
				|| ( getDatabaseMajorVersion() == major && getDatabaseMinorVersion() > minor );
	}

	default boolean isAfter(Integer major, Integer minor) {
		return isAfter( (int) major, minor == null ? NO_VERSION : minor );
	}
}
