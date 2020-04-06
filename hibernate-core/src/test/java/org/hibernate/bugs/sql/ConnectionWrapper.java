package org.hibernate.bugs.sql;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publicly tracks number of open wrapped connections at
 * {@link #openConnections}.
 */
public class ConnectionWrapper implements Connection {

	/** Tracks the number of currently open connections. */
	public static final AtomicInteger currentOpenConnections = new AtomicInteger(0);

	/** Tracks the number of connections ever opened. */
	public static final AtomicInteger openedConnections = new AtomicInteger(0);

	private final Connection wrapped;

	public ConnectionWrapper(final Connection wrapped) {
		super();
		this.wrapped = wrapped;
		currentOpenConnections.incrementAndGet();
		openedConnections.incrementAndGet();
	}

	@Override
	public <T> T unwrap(final Class<T> iface) throws SQLException {
		return wrapped.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return wrapped.isWrapperFor(iface);
	}

	@Override
	public Statement createStatement() throws SQLException {
		return wrapped.createStatement();
	}

	@Override
	public PreparedStatement prepareStatement(final String sql) throws SQLException {
		return wrapped.prepareStatement(sql);
	}

	@Override
	public CallableStatement prepareCall(final String sql) throws SQLException {
		return wrapped.prepareCall(sql);
	}

	@Override
	public String nativeSQL(final String sql) throws SQLException {
		return wrapped.nativeSQL(sql);
	}

	@Override
	public void setAutoCommit(final boolean autoCommit) throws SQLException {
		wrapped.setAutoCommit(autoCommit);
	}

	@Override
	public boolean getAutoCommit() throws SQLException {
		return wrapped.getAutoCommit();
	}

	@Override
	public void commit() throws SQLException {
		wrapped.commit();
	}

	@Override
	public void rollback() throws SQLException {
		wrapped.rollback();
	}

	@Override
	public void close() throws SQLException {
		if (!isClosed())
			currentOpenConnections.decrementAndGet();
		wrapped.close();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return wrapped.isClosed();
	}

	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return wrapped.getMetaData();
	}

	@Override
	public void setReadOnly(final boolean readOnly) throws SQLException {
		wrapped.setReadOnly(readOnly);
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return wrapped.isReadOnly();
	}

	@Override
	public void setCatalog(final String catalog) throws SQLException {
		wrapped.setCatalog(catalog);
	}

	@Override
	public String getCatalog() throws SQLException {
		return wrapped.getCatalog();
	}

	@Override
	public void setTransactionIsolation(final int level) throws SQLException {
		wrapped.setTransactionIsolation(level);
	}

	@Override
	public int getTransactionIsolation() throws SQLException {
		return wrapped.getTransactionIsolation();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return wrapped.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		wrapped.clearWarnings();
	}

	@Override
	public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
		return wrapped.createStatement(resultSetType, resultSetConcurrency);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
			throws SQLException {
		return wrapped.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
			throws SQLException {
		return wrapped.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return wrapped.getTypeMap();
	}

	@Override
	public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
		wrapped.setTypeMap(map);
	}

	@Override
	public void setHoldability(final int holdability) throws SQLException {
		wrapped.setHoldability(holdability);
	}

	@Override
	public int getHoldability() throws SQLException {
		return wrapped.getHoldability();
	}

	@Override
	public Savepoint setSavepoint() throws SQLException {
		return wrapped.setSavepoint();
	}

	@Override
	public Savepoint setSavepoint(final String name) throws SQLException {
		return wrapped.setSavepoint(name);
	}

	@Override
	public void rollback(final Savepoint savepoint) throws SQLException {
		wrapped.rollback(savepoint);
	}

	@Override
	public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
		wrapped.releaseSavepoint(savepoint);
	}

	@Override
	public Statement createStatement(final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		return wrapped.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		return wrapped.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
			final int resultSetHoldability) throws SQLException {
		return wrapped.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
		return wrapped.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
		return wrapped.prepareStatement(sql, columnIndexes);
	}

	@Override
	public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
		return wrapped.prepareStatement(sql, columnNames);
	}

	@Override
	public Clob createClob() throws SQLException {
		return wrapped.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return wrapped.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return wrapped.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return wrapped.createSQLXML();
	}

	@Override
	public boolean isValid(final int timeout) throws SQLException {
		return wrapped.isValid(timeout);
	}

	@Override
	public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
		wrapped.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(final Properties properties) throws SQLClientInfoException {
		wrapped.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(final String name) throws SQLException {
		return wrapped.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return wrapped.getClientInfo();
	}

	@Override
	public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
		return wrapped.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
		return wrapped.createStruct(typeName, attributes);
	}

	@Override
	public void setSchema(final String schema) throws SQLException {
		wrapped.setSchema(schema);
	}

	@Override
	public String getSchema() throws SQLException {
		return wrapped.getSchema();
	}

	@Override
	public void abort(final Executor executor) throws SQLException {
		wrapped.abort(executor);
	}

	@Override
	public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
		wrapped.setNetworkTimeout(executor, milliseconds);
	}

	@Override
	public int getNetworkTimeout() throws SQLException {
		return wrapped.getNetworkTimeout();
	}

}
