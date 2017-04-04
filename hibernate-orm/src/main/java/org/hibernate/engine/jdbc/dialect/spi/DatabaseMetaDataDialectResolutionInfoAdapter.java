/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
