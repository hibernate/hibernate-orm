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
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Exposes information about the database and JDBC driver that can be used in resolving the appropriate Dialect
 * to use.
 * <p/>
 * The information here mimics part of the JDBC {@link java.sql.DatabaseMetaData} contract, specifically the portions
 * about database and driver names and versions.
 *
 * @author Steve Ebersole
 */
public interface DialectResolutionInfo {
	/**
	 * Constant used to indicate that no version is defined
	 */
	public static final int NO_VERSION = -9999;

	/**
	 * Obtain access to the database name, as returned from {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
	 * for the target database
	 *
	 * @return The database name
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseProductName()
	 */
	public String getDatabaseName();

	/**
	 * Obtain access to the database major version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()} for the target database.
	 *
	 * @return The database major version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
	 */
	public int getDatabaseMajorVersion();

	/**
	 * Obtain access to the database minor version, as returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()} for the target database.
	 *
	 * @return The database minor version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
	 */
	public int getDatabaseMinorVersion();

	/**
	 * Obtain access to the name of the JDBC driver, as returned from {@link java.sql.DatabaseMetaData#getDriverName()}
	 * for the target database
	 *
	 * @return The JDBC driver name
	 *
	 * @see java.sql.DatabaseMetaData#getDriverName()
	 */
	public String getDriverName();

	/**
	 * Obtain access to the major version of the JDBC driver, as returned from
	 * {@link java.sql.DatabaseMetaData#getDriverMajorVersion()} ()} for the target database.
	 *
	 * @return The JDBC driver major version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDriverMajorVersion()
	 */
	public int getDriverMajorVersion();

	/**
	 * Obtain access to the minor version of the JDBC driver, as returned from
	 * {@link java.sql.DatabaseMetaData#getDriverMinorVersion()} for the target database.
	 *
	 * @return The JDBC driver minor version, or {@value #NO_VERSION} to indicate "no version information"
	 *
	 * @see java.sql.DatabaseMetaData#getDriverMinorVersion()
	 */
	public int getDriverMinorVersion();


}
