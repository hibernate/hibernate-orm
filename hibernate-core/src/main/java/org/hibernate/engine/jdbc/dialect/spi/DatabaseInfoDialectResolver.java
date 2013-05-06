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

import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;

/**
 * A contract for resolving database name, major version and minor version to Dialect
 *
 * @author Steve Ebersole
 */
public interface DatabaseInfoDialectResolver extends Service {
	/**
	 * Determine the {@link Dialect} to use based on the given information.  Implementations are
	 * expected to return the {@link Dialect} instance to use, or {@code null} if the they did not locate a match.
	 *
	 * @param databaseInfo Access to the needed database information
	 *
	 * @return The dialect to use, or null.
	 */
	public Dialect resolve(DatabaseInfo databaseInfo);

	/**
	 * Essentially a "parameter object" for {@link DatabaseInfoDialectResolver#resolve}
	 */
	public static interface DatabaseInfo {
		/**
		 * Constant used to indicate that no version is defined
		 */
		public static final int NO_VERSION = -9999;

		/**
		 * Obtain access to the database name, as returned from {@link java.sql.DatabaseMetaData#getDatabaseProductName()}
		 * for the target database
		 *
		 * @return The database name
		 */
		public String getDatabaseName();

		/**
		 * Obtain access to the database major version, as returned from
		 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()} for the target database; {@value #NO_VERSION}
		 * indicates no version information was supplied
		 *
		 * @return The major version
		 *
		 * @see #NO_VERSION
		 */
		public int getDatabaseMajorVersion();

		/**
		 * Obtain access to the database minor version, as returned from
		 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()} for the target database; {@value #NO_VERSION}
		 * indicates no version information was supplied
		 *
		 * @return The minor version
		 *
		 * @see #NO_VERSION
		 */
		public int getDatabaseMinorVersion();
	}
}
