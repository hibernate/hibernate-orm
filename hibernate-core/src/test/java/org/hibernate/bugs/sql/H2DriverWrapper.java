package org.hibernate.bugs.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class H2DriverWrapper implements Driver {

	private final Driver wrapped;

	public H2DriverWrapper() {
		this.wrapped = new org.h2.Driver();
	}

	@Override
	public Connection connect(String url, Properties info) throws SQLException {
		return new ConnectionWrapper(wrapped.connect(url, info));
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return wrapped.acceptsURL(url);
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return wrapped.getPropertyInfo(url, info);
	}

	@Override
	public int getMajorVersion() {
		return wrapped.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return wrapped.getMinorVersion();
	}

	@Override
	public boolean jdbcCompliant() {
		return wrapped.jdbcCompliant();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return wrapped.getParentLogger();
	}
}
