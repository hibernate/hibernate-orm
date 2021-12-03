/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Exposes information about the database and JDBC driver that can be used in resolving the appropriate Dialect
 * to use.
 * <p/>
 * The information here mimics part of the JDBC {@link java.sql.DatabaseMetaData} contract, specifically the portions
 * about database and driver names and versions.
 */
public interface DialectResolutionInfo extends DialectVersionDetails {

	/**
	 * Obtain access to the database name, as returned from {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
	 * for the target database
	 *
	 * @return The database name
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	String getDatabaseName();

	/**
	 * Obtain access to the database version, as returned from {@link java.sql.DatabaseMetaData#getDatabaseProductVersion()}
	 * for the target database
	 *
	 * @return The database version
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseProductVersion()
	 */
	String getDatabaseVersion();

	/**
	 * Obtain access to the name of the JDBC driver, as returned from {@link java.sql.DatabaseMetaData#getDriverName()}
	 * for the target database
	 *
	 * @return The JDBC driver name
	 *
	 * @see java.sql.DatabaseMetaData#getDriverName()
	 */
	String getDriverName();

	/**
	 * Obtain access to the SQL keywords of the JDBC driver, as returned from
	 * {@link java.sql.DatabaseMetaData#getSQLKeywords()} for the target database.
	 *
	 * @return The JDBC driver keywords
	 *
	 * @see java.sql.DatabaseMetaData#getSQLKeywords()
	 */
	String getSQLKeywords();

	/**
	 * Obtain access to the underlying object of the given type.
	 *
	 * Return <code>null</code> if the underlying object is not of the given type.
	 *
	 * @return The unwrapped object or <code>null</code>
	 */
	default <T> T unwrap(Class<T> clazz) {
		return null;
	}
}
