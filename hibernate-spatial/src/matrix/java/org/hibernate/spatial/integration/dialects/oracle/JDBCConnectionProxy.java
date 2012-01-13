package org.hibernate.spatial.integration.dialects.oracle;

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

/**
 * This class hides the actual Connection implementation class.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jun 30, 2010
 */
public class JDBCConnectionProxy implements Connection {

	private final Connection delegate;

	public JDBCConnectionProxy(Connection conn) {
		this.delegate = conn;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return delegate.unwrap( iface );
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return delegate.isWrapperFor( iface );
	}

	public Statement createStatement() throws SQLException {
		return delegate.createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return delegate.prepareStatement( sql );
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return delegate.prepareCall( sql );
	}

	public String nativeSQL(String sql) throws SQLException {
		return delegate.nativeSQL( sql );
	}

	public void setAutoCommit(boolean autoCommit) throws SQLException {
		delegate.setAutoCommit( autoCommit );
	}

	public boolean getAutoCommit() throws SQLException {
		return delegate.getAutoCommit();
	}

	public void commit() throws SQLException {
		delegate.commit();
	}

	public void rollback() throws SQLException {
		delegate.rollback();
	}

	public void close() throws SQLException {
		delegate.close();
	}

	public boolean isClosed() throws SQLException {
		return delegate.isClosed();
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return delegate.getMetaData();
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		delegate.setReadOnly( readOnly );
	}

	public boolean isReadOnly() throws SQLException {
		return delegate.isReadOnly();
	}

	public void setCatalog(String catalog) throws SQLException {
		delegate.setCatalog( catalog );
	}

	public String getCatalog() throws SQLException {
		return delegate.getCatalog();
	}

	public void setTransactionIsolation(int level) throws SQLException {
		delegate.setTransactionIsolation( level );
	}

	public int getTransactionIsolation() throws SQLException {
		return delegate.getTransactionIsolation();
	}

	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return delegate.createStatement( resultSetType, resultSetConcurrency );
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return delegate.prepareStatement( sql, resultSetType, resultSetConcurrency );
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return delegate.prepareCall( sql, resultSetType, resultSetConcurrency );
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return delegate.getTypeMap();
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		delegate.setTypeMap( map );
	}

	public void setHoldability(int holdability) throws SQLException {
		delegate.setHoldability( holdability );
	}

	public int getHoldability() throws SQLException {
		return delegate.getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException {
		return delegate.setSavepoint();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		return delegate.setSavepoint( name );
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		delegate.rollback( savepoint );
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		delegate.releaseSavepoint( savepoint );
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return delegate.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability );
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return delegate.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return delegate.prepareCall( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return delegate.prepareStatement( sql, autoGeneratedKeys );
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return delegate.prepareStatement( sql, columnIndexes );
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return delegate.prepareStatement( sql, columnNames );
	}

	public Clob createClob() throws SQLException {
		return delegate.createClob();
	}

	public Blob createBlob() throws SQLException {
		return delegate.createBlob();
	}

	public NClob createNClob() throws SQLException {
		return delegate.createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return delegate.createSQLXML();
	}

	public boolean isValid(int timeout) throws SQLException {
		return delegate.isValid( timeout );
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		delegate.setClientInfo( name, value );
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		delegate.setClientInfo( properties );
	}

	public String getClientInfo(String name) throws SQLException {
		return delegate.getClientInfo( name );
	}

	public Properties getClientInfo() throws SQLException {
		return delegate.getClientInfo();
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return delegate.createArrayOf( typeName, elements );
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return delegate.createStruct( typeName, attributes );
	}
}
