package org.hibernate.bugs.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class DataSourceWrapper implements DataSource {

	private final DataSource wrapped;

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return wrapped.getLogWriter();
	}

	@Override
	public <T> T unwrap(final Class<T> iface) throws SQLException {
		return wrapped.unwrap(iface);
	}

	@Override
	public void setLogWriter(final PrintWriter out) throws SQLException {
		wrapped.setLogWriter(out);
	}

	@Override
	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return wrapped.isWrapperFor(iface);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(wrapped.getConnection());
	}

	@Override
	public void setLoginTimeout(final int seconds) throws SQLException {
		wrapped.setLoginTimeout(seconds);
	}

	@Override
	public Connection getConnection(final String username, final String password) throws SQLException {
		return new ConnectionWrapper(wrapped.getConnection(username, password));
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return wrapped.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return wrapped.getParentLogger();
	}

	public DataSourceWrapper(final DataSource dataSource) {
		this.wrapped = dataSource;
	}

}
