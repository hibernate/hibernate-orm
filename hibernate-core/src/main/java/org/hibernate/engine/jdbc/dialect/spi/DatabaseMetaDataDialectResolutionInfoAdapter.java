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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * An implementation of DialectResolutionInfo that delegates calls to a wrapped {@link DatabaseMetaData}.
 * <p/>
 * All {@link SQLException}s resulting from calls on the DatabaseMetaData are converted to the Hibernate
 * {@link org.hibernate.JDBCException} hierarchy.
 *
 * @author Steve Ebersole
 */
public class DatabaseMetaDataDialectResolutionInfoAdapter implements DialectResolutionInfo {
	private final DatabaseMetaData databaseMetaData;

	public DatabaseMetaDataDialectResolutionInfoAdapter(DatabaseMetaData databaseMetaData) {
		this.databaseMetaData = databaseMetaData;
	}

	@Override
	public String getDatabaseName() {
		try {
			return databaseMetaData.getDatabaseProductName();
		}
		catch (SQLException e) {
			throw BasicSQLExceptionConverter.INSTANCE.convert( e );
		}
	}

	@Override
	public int getDatabaseMajorVersion() {
		try {
			return interpretVersion( databaseMetaData.getDatabaseMajorVersion() );
		}
		catch (SQLException e) {
			throw BasicSQLExceptionConverter.INSTANCE.convert( e );
		}
	}

	private static int interpretVersion(int result) {
		return result < 0 ? NO_VERSION : result;
	}

	@Override
	public int getDatabaseMinorVersion() {
		try {
			return interpretVersion( databaseMetaData.getDatabaseMinorVersion() );
		}
		catch (SQLException e) {
			throw BasicSQLExceptionConverter.INSTANCE.convert( e );
		}
	}

	@Override
	public String getDriverName() {
		try {
			return databaseMetaData.getDriverName();
		}
		catch (SQLException e) {
			throw BasicSQLExceptionConverter.INSTANCE.convert( e );
		}
	}

	@Override
	public int getDriverMajorVersion() {
		return interpretVersion( databaseMetaData.getDriverMajorVersion() );
	}

	@Override
	public int getDriverMinorVersion() {
		return interpretVersion( databaseMetaData.getDriverMinorVersion() );
	}
}
