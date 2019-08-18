/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.connection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * @author Emmanuel Bernard
 */
public class FakeDataSource implements DataSource {
	@Override
	public Connection getConnection() throws SQLException {
		throw new FakeDataSourceException( "connection" );
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		throw new FakeDataSourceException( "connection with password" );
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw new FakeDataSourceException( "getLogWriter" );
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new FakeDataSourceException( "setLogWriter" );
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new FakeDataSourceException( "setLoginTimeout" );
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new FakeDataSourceException( "getLoginTimeout" );
	}

	@Override
	public <T> T unwrap(Class<T> tClass) throws SQLException {
		throw new UnsupportedOperationException("not yet supported");
	}

	@Override
	public boolean isWrapperFor(Class<?> aClass) throws SQLException {
		return false;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}
}
