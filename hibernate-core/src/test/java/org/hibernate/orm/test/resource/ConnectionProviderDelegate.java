/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class ConnectionProviderDelegate implements
		ConnectionProvider,
		Configurable,
		ServiceRegistryAwareService,
		Stoppable {

	private ServiceRegistryImplementor serviceRegistry;

	private ConnectionProvider connectionProvider;
	private boolean configured;
	private final boolean forceSupportsAggressiveRelease;

	public ConnectionProviderDelegate() {
		this( false );
	}

	public ConnectionProviderDelegate(boolean forceSupportsAggressiveRelease) {
		this.forceSupportsAggressiveRelease = forceSupportsAggressiveRelease;
	}

	public ConnectionProviderDelegate(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
		this.forceSupportsAggressiveRelease = false;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map<String, Object> configurationValues) {
		if ( !configured ) {
			if ( connectionProvider == null ) {
				Map<String, Object> settings = new HashMap<>( configurationValues );
				settings.remove( AvailableSettings.CONNECTION_PROVIDER );
				connectionProvider = ConnectionProviderInitiator.INSTANCE.initiateService(
						settings,
						serviceRegistry
				);
			}
			if ( connectionProvider instanceof ServiceRegistryAwareService ) {
				( (ServiceRegistryAwareService) connectionProvider ).injectServices( serviceRegistry );
			}
			if ( connectionProvider instanceof Configurable ) {
				Configurable configurableConnectionProvider = (Configurable) connectionProvider;
				configurableConnectionProvider.configure( configurationValues );
			}
			configured = true;
		}
	}

	HashMap<Connection, ConnectionWrapper> xref = new HashMap<>();

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = connectionProvider.getConnection();
		ConnectionWrapper connectionWrapper = xref.get( connection );
		if ( connectionWrapper == null ) {
			connectionWrapper = new ConnectionWrapper( connection );
			xref.put( connection, connectionWrapper );
		}
		return connectionWrapper;
	}

	@Override
	public void closeConnection(Connection connection) throws SQLException {
		connectionProvider.closeConnection( ((ConnectionWrapper) connection).delegate );
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return forceSupportsAggressiveRelease
			|| connectionProvider.supportsAggressiveRelease();
	}

	@Override
	public DatabaseConnectionInfo getDatabaseConnectionInfo(Dialect dialect) {
		return connectionProvider.getDatabaseConnectionInfo( dialect );
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return connectionProvider.isUnwrappableAs( unwrapType );
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return connectionProvider.unwrap( unwrapType );
	}

	@Override
	public void stop() {
		if ( connectionProvider instanceof Stoppable ) {
			( (Stoppable) connectionProvider ).stop();
		}
	}

	static class StatementWrapper implements Statement {

		protected final Statement statement;

		public StatementWrapper(Statement statement) {
			this.statement = statement;
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException {
			return statement.executeQuery( sql );
		}

		@Override
		public int executeUpdate(String sql) throws SQLException {
			return statement.executeUpdate( sql );
		}

		@Override
		public void close() throws SQLException {
			statement.close();
		}

		@Override
		public int getMaxFieldSize() throws SQLException {
			return statement.getMaxFieldSize();
		}

		@Override
		public void setMaxFieldSize(int max) throws SQLException {
			statement.setMaxFieldSize( max );
		}

		@Override
		public int getMaxRows() throws SQLException {
			return statement.getMaxRows();
		}

		@Override
		public void setMaxRows(int max) throws SQLException {
			statement.setMaxRows( max );
		}

		@Override
		public void setEscapeProcessing(boolean enable) throws SQLException {
			statement.setEscapeProcessing( enable );
		}

		@Override
		public int getQueryTimeout() throws SQLException {
			return statement.getQueryTimeout();
		}

		@Override
		public void setQueryTimeout(int seconds) throws SQLException {
			statement.setQueryTimeout( seconds );
		}

		@Override
		public void cancel() throws SQLException {
			statement.cancel();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return statement.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			statement.clearWarnings();
		}

		@Override
		public void setCursorName(String name) throws SQLException {
			statement.setCursorName( name );
		}

		@Override
		public boolean execute(String sql) throws SQLException {
			return statement.execute( sql );
		}

		@Override
		public ResultSet getResultSet() throws SQLException {
			return statement.getResultSet();
		}

		@Override
		public int getUpdateCount() throws SQLException {
			return statement.getUpdateCount();
		}

		@Override
		public boolean getMoreResults() throws SQLException {
			return statement.getMoreResults();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			statement.setFetchDirection( direction );
		}

		@Override
		public int getFetchDirection() throws SQLException {
			return statement.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			statement.setFetchSize( rows );
		}

		@Override
		public int getFetchSize() throws SQLException {
			return statement.getFetchSize();
		}

		@Override
		public int getResultSetConcurrency() throws SQLException {
			return statement.getResultSetConcurrency();
		}

		@Override
		public int getResultSetType() throws SQLException {
			return statement.getResultSetType();
		}

		@Override
		public void addBatch(String sql) throws SQLException {
			statement.addBatch( sql );
		}

		@Override
		public void clearBatch() throws SQLException {
			statement.clearBatch();
		}

		@Override
		public int[] executeBatch() throws SQLException {
			return statement.executeBatch();
		}

		@Override
		public Connection getConnection() throws SQLException {
			return statement.getConnection();
		}

		@Override
		public boolean getMoreResults(int current) throws SQLException {
			return statement.getMoreResults( current );
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException {
			return statement.getGeneratedKeys();
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			return statement.executeUpdate( sql, autoGeneratedKeys );
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
			return statement.executeUpdate( sql, columnIndexes );
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames) throws SQLException {
			return statement.executeUpdate( sql, columnNames );
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
			return statement.execute( sql, autoGeneratedKeys );
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes) throws SQLException {
			return statement.execute( sql, columnIndexes );
		}

		@Override
		public boolean execute(String sql, String[] columnNames) throws SQLException {
			return statement.execute( sql, columnNames );
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			return statement.getResultSetHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return statement.isClosed();
		}

		@Override
		public void setPoolable(boolean poolable) throws SQLException {
			statement.setPoolable( poolable );
		}

		@Override
		public boolean isPoolable() throws SQLException {
			return statement.isPoolable();
		}

		@Override
		public void closeOnCompletion() throws SQLException {
			statement.closeOnCompletion();
		}

		@Override
		public boolean isCloseOnCompletion() throws SQLException {
			return statement.isCloseOnCompletion();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return statement.unwrap( iface );
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return statement.isWrapperFor( iface );
		}
	}

	static class PrepareStatementWrapper extends StatementWrapper implements PreparedStatement {
		private final PreparedStatement delegate;

		public PrepareStatementWrapper(PreparedStatement statement) {
			super( statement );
			delegate = statement;
		}

		@Override
		public ResultSet executeQuery() throws SQLException {
			return delegate.executeQuery();
		}

		@Override
		public int executeUpdate() throws SQLException {
			return delegate.executeUpdate();
		}

		@Override
		public void setNull(int parameterIndex, int sqlType) throws SQLException {
			delegate.setNull( parameterIndex, sqlType );
		}

		@Override
		public void setBoolean(int parameterIndex, boolean x) throws SQLException {
			delegate.setBoolean( parameterIndex, x );
		}

		@Override
		public void setByte(int parameterIndex, byte x) throws SQLException {
			delegate.setByte( parameterIndex, x );
		}

		@Override
		public void setShort(int parameterIndex, short x) throws SQLException {
			delegate.setShort( parameterIndex, x );
		}

		@Override
		public void setInt(int parameterIndex, int x) throws SQLException {
			delegate.setInt( parameterIndex, x );
		}

		@Override
		public void setLong(int parameterIndex, long x) throws SQLException {
			delegate.setLong( parameterIndex, x );
		}

		@Override
		public void setFloat(int parameterIndex, float x) throws SQLException {
			delegate.setFloat( parameterIndex, x );
		}

		@Override
		public void setDouble(int parameterIndex, double x) throws SQLException {
			delegate.setDouble( parameterIndex, x );
		}

		@Override
		public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
			delegate.setBigDecimal( parameterIndex, x );
		}

		@Override
		public void setString(int parameterIndex, String x) throws SQLException {
			delegate.setString( parameterIndex, x );
		}

		@Override
		public void setBytes(int parameterIndex, byte[] x) throws SQLException {
			delegate.setBytes( parameterIndex, x );
		}

		@Override
		public void setDate(int parameterIndex, Date x) throws SQLException {
			delegate.setDate( parameterIndex, x );
		}

		@Override
		public void setTime(int parameterIndex, Time x) throws SQLException {
			delegate.setTime( parameterIndex, x );
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
			delegate.setTimestamp( parameterIndex, x );
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setAsciiStream( parameterIndex, x, length );
		}

		@Override
		public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setUnicodeStream( parameterIndex, x, length );
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setBinaryStream( parameterIndex, x, length );
		}

		@Override
		public void clearParameters() throws SQLException {
			delegate.clearParameters();
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
			delegate.setObject( parameterIndex, x, targetSqlType );
		}

		@Override
		public void setObject(int parameterIndex, Object x) throws SQLException {
			delegate.setObject( parameterIndex, x );
		}

		@Override
		public boolean execute() throws SQLException {
			return delegate.execute();
		}

		@Override
		public void addBatch() throws SQLException {
			delegate.addBatch();
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
			delegate.setCharacterStream( parameterIndex, reader, length );
		}

		@Override
		public void setRef(int parameterIndex, Ref x) throws SQLException {
			delegate.setRef( parameterIndex, x );
		}

		@Override
		public void setBlob(int parameterIndex, Blob x) throws SQLException {
			delegate.setBlob( parameterIndex, x );
		}

		@Override
		public void setClob(int parameterIndex, Clob x) throws SQLException {
			delegate.setClob( parameterIndex, x );
		}

		@Override
		public void setArray(int parameterIndex, Array x) throws SQLException {
			delegate.setArray( parameterIndex, x );
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			return delegate.getMetaData();
		}

		@Override
		public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
			delegate.setDate( parameterIndex, x, cal );
		}

		@Override
		public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
			delegate.setTime( parameterIndex, x, cal );
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
			delegate.setTimestamp( parameterIndex, x, cal );
		}

		@Override
		public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
			delegate.setNull( parameterIndex, sqlType, typeName );
		}

		@Override
		public void setURL(int parameterIndex, URL x) throws SQLException {
			delegate.setURL( parameterIndex, x );
		}

		@Override
		public ParameterMetaData getParameterMetaData() throws SQLException {
			return delegate.getParameterMetaData();
		}

		@Override
		public void setRowId(int parameterIndex, RowId x) throws SQLException {
			delegate.setRowId( parameterIndex, x );
		}

		@Override
		public void setNString(int parameterIndex, String value) throws SQLException {
			delegate.setNString( parameterIndex, value );
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
			delegate.setNCharacterStream( parameterIndex, value, length );
		}

		@Override
		public void setNClob(int parameterIndex, NClob value) throws SQLException {
			delegate.setNClob( parameterIndex, value );
		}

		@Override
		public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setClob( parameterIndex, reader, length );
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
			delegate.setBlob( parameterIndex, inputStream, length );
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setNClob( parameterIndex, reader, length );
		}

		@Override
		public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
			delegate.setSQLXML( parameterIndex, xmlObject );
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
			delegate.setObject( parameterIndex, x, targetSqlType, scaleOrLength );
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
			delegate.setAsciiStream( parameterIndex, x, length );
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
			delegate.setBinaryStream( parameterIndex, x, length );
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setCharacterStream( parameterIndex, reader, length );
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
			delegate.setAsciiStream( parameterIndex, x );
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
			delegate.setBinaryStream( parameterIndex, x );
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
			delegate.setCharacterStream( parameterIndex, reader );
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
			delegate.setNCharacterStream( parameterIndex, value );
		}

		@Override
		public void setClob(int parameterIndex, Reader reader) throws SQLException {
			delegate.setClob( parameterIndex, reader );
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
			delegate.setBlob( parameterIndex, inputStream );
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader) throws SQLException {
			delegate.setNClob( parameterIndex, reader );
		}
	}

	static class CallableStatementWrapper extends PrepareStatementWrapper implements CallableStatement {

		private final CallableStatement delegate;

		public CallableStatementWrapper(CallableStatement statement) {
			super( statement );
			delegate = statement;
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
			delegate.registerOutParameter( parameterIndex, sqlType );
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
			delegate.registerOutParameter( parameterIndex, sqlType, scale );
		}

		@Override
		public boolean wasNull() throws SQLException {
			return delegate.wasNull();
		}

		@Override
		public String getString(int parameterIndex) throws SQLException {
			return delegate.getString( parameterIndex );
		}

		@Override
		public boolean getBoolean(int parameterIndex) throws SQLException {
			return delegate.getBoolean( parameterIndex );
		}

		@Override
		public byte getByte(int parameterIndex) throws SQLException {
			return delegate.getByte( parameterIndex );
		}

		@Override
		public short getShort(int parameterIndex) throws SQLException {
			return delegate.getShort( parameterIndex );
		}

		@Override
		public int getInt(int parameterIndex) throws SQLException {
			return delegate.getInt( parameterIndex );
		}

		@Override
		public long getLong(int parameterIndex) throws SQLException {
			return delegate.getLong( parameterIndex );
		}

		@Override
		public float getFloat(int parameterIndex) throws SQLException {
			return delegate.getFloat( parameterIndex );
		}

		@Override
		public double getDouble(int parameterIndex) throws SQLException {
			return delegate.getDouble( parameterIndex );
		}

		@Override
		public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
			return delegate.getBigDecimal( parameterIndex, scale );
		}

		@Override
		public byte[] getBytes(int parameterIndex) throws SQLException {
			return delegate.getBytes( parameterIndex );
		}

		@Override
		public Date getDate(int parameterIndex) throws SQLException {
			return delegate.getDate( parameterIndex );
		}

		@Override
		public Time getTime(int parameterIndex) throws SQLException {
			return delegate.getTime( parameterIndex );
		}

		@Override
		public Timestamp getTimestamp(int parameterIndex) throws SQLException {
			return delegate.getTimestamp( parameterIndex );
		}

		@Override
		public Object getObject(int parameterIndex) throws SQLException {
			return delegate.getObject( parameterIndex );
		}

		@Override
		public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
			return delegate.getBigDecimal( parameterIndex );
		}

		@Override
		public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject( parameterIndex, map );
		}

		@Override
		public Ref getRef(int parameterIndex) throws SQLException {
			return delegate.getRef( parameterIndex );
		}

		@Override
		public Blob getBlob(int parameterIndex) throws SQLException {
			return delegate.getBlob( parameterIndex );
		}

		@Override
		public Clob getClob(int parameterIndex) throws SQLException {
			return delegate.getClob( parameterIndex );
		}

		@Override
		public Array getArray(int parameterIndex) throws SQLException {
			return delegate.getArray( parameterIndex );
		}

		@Override
		public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
			return delegate.getDate( parameterIndex, cal );
		}

		@Override
		public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
			return delegate.getTime( parameterIndex, cal );
		}

		@Override
		public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
			return delegate.getTimestamp( parameterIndex, cal );
		}

		@Override
		public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
			delegate.registerOutParameter( parameterIndex, sqlType, typeName );
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
			delegate.registerOutParameter( parameterName, sqlType );
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
			delegate.registerOutParameter( parameterName, sqlType, scale );
		}

		@Override
		public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
			delegate.registerOutParameter( parameterName, sqlType, typeName );
		}

		@Override
		public URL getURL(int parameterIndex) throws SQLException {
			return delegate.getURL( parameterIndex );
		}

		@Override
		public void setURL(String parameterName, URL val) throws SQLException {
			delegate.setURL( parameterName, val );
		}

		@Override
		public void setNull(String parameterName, int sqlType) throws SQLException {
			delegate.setNull( parameterName, sqlType );
		}

		@Override
		public void setBoolean(String parameterName, boolean x) throws SQLException {
			delegate.setBoolean( parameterName, x );
		}

		@Override
		public void setByte(String parameterName, byte x) throws SQLException {
			delegate.setByte( parameterName, x );
		}

		@Override
		public void setShort(String parameterName, short x) throws SQLException {
			delegate.setShort( parameterName, x );
		}

		@Override
		public void setInt(String parameterName, int x) throws SQLException {
			delegate.setInt( parameterName, x );
		}

		@Override
		public void setLong(String parameterName, long x) throws SQLException {
			delegate.setLong( parameterName, x );
		}

		@Override
		public void setFloat(String parameterName, float x) throws SQLException {
			delegate.setFloat( parameterName, x );
		}

		@Override
		public void setDouble(String parameterName, double x) throws SQLException {
			delegate.setDouble( parameterName, x );
		}

		@Override
		public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
			delegate.setBigDecimal( parameterName, x );
		}

		@Override
		public void setString(String parameterName, String x) throws SQLException {
			delegate.setString( parameterName, x );
		}

		@Override
		public void setBytes(String parameterName, byte[] x) throws SQLException {
			delegate.setBytes( parameterName, x );
		}

		@Override
		public void setDate(String parameterName, Date x) throws SQLException {
			delegate.setDate( parameterName, x );
		}

		@Override
		public void setTime(String parameterName, Time x) throws SQLException {
			delegate.setTime( parameterName, x );
		}

		@Override
		public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
			delegate.setTimestamp( parameterName, x );
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
			delegate.setAsciiStream( parameterName, x, length );
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
			delegate.setBinaryStream( parameterName, x, length );
		}

		@Override
		public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
			delegate.setObject( parameterName, x, targetSqlType, scale );
		}

		@Override
		public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
			delegate.setObject( parameterName, x, targetSqlType );
		}

		@Override
		public void setObject(String parameterName, Object x) throws SQLException {
			delegate.setObject( parameterName, x );
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
			delegate.setCharacterStream( parameterName, reader, length );
		}

		@Override
		public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
			delegate.setDate( parameterName, x, cal );
		}

		@Override
		public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
			delegate.setTime( parameterName, x, cal );
		}

		@Override
		public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
			delegate.setTimestamp( parameterName, x, cal );
		}

		@Override
		public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
			delegate.setNull( parameterName, sqlType, typeName );
		}

		@Override
		public String getString(String parameterName) throws SQLException {
			return delegate.getString( parameterName );
		}

		@Override
		public boolean getBoolean(String parameterName) throws SQLException {
			return delegate.getBoolean( parameterName );
		}

		@Override
		public byte getByte(String parameterName) throws SQLException {
			return delegate.getByte( parameterName );
		}

		@Override
		public short getShort(String parameterName) throws SQLException {
			return delegate.getShort( parameterName );
		}

		@Override
		public int getInt(String parameterName) throws SQLException {
			return delegate.getInt( parameterName );
		}

		@Override
		public long getLong(String parameterName) throws SQLException {
			return delegate.getLong( parameterName );
		}

		@Override
		public float getFloat(String parameterName) throws SQLException {
			return delegate.getFloat( parameterName );
		}

		@Override
		public double getDouble(String parameterName) throws SQLException {
			return delegate.getDouble( parameterName );
		}

		@Override
		public byte[] getBytes(String parameterName) throws SQLException {
			return delegate.getBytes( parameterName );
		}

		@Override
		public Date getDate(String parameterName) throws SQLException {
			return delegate.getDate( parameterName );
		}

		@Override
		public Time getTime(String parameterName) throws SQLException {
			return delegate.getTime( parameterName );
		}

		@Override
		public Timestamp getTimestamp(String parameterName) throws SQLException {
			return delegate.getTimestamp( parameterName );
		}

		@Override
		public Object getObject(String parameterName) throws SQLException {
			return delegate.getObject( parameterName );
		}

		@Override
		public BigDecimal getBigDecimal(String parameterName) throws SQLException {
			return delegate.getBigDecimal( parameterName );
		}

		@Override
		public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject( parameterName, map );
		}

		@Override
		public Ref getRef(String parameterName) throws SQLException {
			return delegate.getRef( parameterName );
		}

		@Override
		public Blob getBlob(String parameterName) throws SQLException {
			return delegate.getBlob( parameterName );
		}

		@Override
		public Clob getClob(String parameterName) throws SQLException {
			return delegate.getClob( parameterName );
		}

		@Override
		public Array getArray(String parameterName) throws SQLException {
			return delegate.getArray( parameterName );
		}

		@Override
		public Date getDate(String parameterName, Calendar cal) throws SQLException {
			return delegate.getDate( parameterName, cal );
		}

		@Override
		public Time getTime(String parameterName, Calendar cal) throws SQLException {
			return delegate.getTime( parameterName, cal );
		}

		@Override
		public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
			return delegate.getTimestamp( parameterName, cal );
		}

		@Override
		public URL getURL(String parameterName) throws SQLException {
			return delegate.getURL( parameterName );
		}

		@Override
		public RowId getRowId(int parameterIndex) throws SQLException {
			return delegate.getRowId( parameterIndex );
		}

		@Override
		public RowId getRowId(String parameterName) throws SQLException {
			return delegate.getRowId( parameterName );
		}

		@Override
		public void setRowId(String parameterName, RowId x) throws SQLException {
			delegate.setRowId( parameterName, x );
		}

		@Override
		public void setNString(String parameterName, String value) throws SQLException {
			delegate.setNString( parameterName, value );
		}

		@Override
		public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
			delegate.setNCharacterStream( parameterName, value, length );
		}

		@Override
		public void setNClob(String parameterName, NClob value) throws SQLException {
			delegate.setNClob( parameterName, value );
		}

		@Override
		public void setClob(String parameterName, Reader reader, long length) throws SQLException {
			delegate.setClob( parameterName, reader, length );
		}

		@Override
		public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
			delegate.setBlob( parameterName, inputStream, length );
		}

		@Override
		public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
			delegate.setNClob( parameterName, reader, length );
		}

		@Override
		public NClob getNClob(int parameterIndex) throws SQLException {
			return delegate.getNClob( parameterIndex );
		}

		@Override
		public NClob getNClob(String parameterName) throws SQLException {
			return delegate.getNClob( parameterName );
		}

		@Override
		public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
			delegate.setSQLXML( parameterName, xmlObject );
		}

		@Override
		public SQLXML getSQLXML(int parameterIndex) throws SQLException {
			return delegate.getSQLXML( parameterIndex );
		}

		@Override
		public SQLXML getSQLXML(String parameterName) throws SQLException {
			return delegate.getSQLXML( parameterName );
		}

		@Override
		public String getNString(int parameterIndex) throws SQLException {
			return delegate.getNString( parameterIndex );
		}

		@Override
		public String getNString(String parameterName) throws SQLException {
			return delegate.getNString( parameterName );
		}

		@Override
		public Reader getNCharacterStream(int parameterIndex) throws SQLException {
			return delegate.getNCharacterStream( parameterIndex );
		}

		@Override
		public Reader getNCharacterStream(String parameterName) throws SQLException {
			return delegate.getNCharacterStream( parameterName );
		}

		@Override
		public Reader getCharacterStream(int parameterIndex) throws SQLException {
			return delegate.getCharacterStream( parameterIndex );
		}

		@Override
		public Reader getCharacterStream(String parameterName) throws SQLException {
			return delegate.getCharacterStream( parameterName );
		}

		@Override
		public void setBlob(String parameterName, Blob x) throws SQLException {
			delegate.setBlob( parameterName, x );
		}

		@Override
		public void setClob(String parameterName, Clob x) throws SQLException {
			delegate.setClob( parameterName, x );
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
			delegate.setAsciiStream( parameterName, x, length );
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
			delegate.setBinaryStream( parameterName, x, length );
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
			delegate.setCharacterStream( parameterName, reader, length );
		}

		@Override
		public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
			delegate.setAsciiStream( parameterName, x );
		}

		@Override
		public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
			delegate.setBinaryStream( parameterName, x );
		}

		@Override
		public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
			delegate.setCharacterStream( parameterName, reader );
		}

		@Override
		public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
			delegate.setNCharacterStream( parameterName, value );
		}

		@Override
		public void setClob(String parameterName, Reader reader) throws SQLException {
			delegate.setClob( parameterName, reader );
		}

		@Override
		public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
			delegate.setBlob( parameterName, inputStream );
		}

		@Override
		public void setNClob(String parameterName, Reader reader) throws SQLException {
			delegate.setNClob( parameterName, reader );
		}

		@Override
		public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
			return delegate.getObject( parameterIndex, type );
		}

		@Override
		public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
			return delegate.getObject( parameterName, type );
		}
	}

	static class ConnectionWrapper implements Connection {

		private final Connection delegate;

		public ConnectionWrapper(Connection connection) {
			this.delegate = connection;
		}


		@Override
		public Statement createStatement() throws SQLException {
			Statement statement = delegate.createStatement();
			return new StatementWrapper( statement );
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return new PrepareStatementWrapper( delegate.prepareStatement( sql ) );
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return new CallableStatementWrapper( delegate.prepareCall( sql ) );
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return delegate.nativeSQL( sql );
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			delegate.setAutoCommit( autoCommit );
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return delegate.getAutoCommit();
		}

		@Override
		public void commit() throws SQLException {
			delegate.commit();
		}

		@Override
		public void rollback() throws SQLException {
			delegate.rollback();
		}

		@Override
		public void close() throws SQLException {
			delegate.close();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return delegate.getMetaData();
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			delegate.setReadOnly( readOnly );
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return delegate.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			delegate.setCatalog( catalog );
		}

		@Override
		public String getCatalog() throws SQLException {
			return delegate.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			delegate.setTransactionIsolation( level );
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return delegate.getTransactionIsolation();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return delegate.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			delegate.clearWarnings();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return delegate.createStatement( resultSetType, resultSetConcurrency );
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return delegate.prepareStatement( sql, resultSetType, resultSetConcurrency );
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return delegate.prepareCall( sql, resultSetType, resultSetConcurrency );
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return delegate.getTypeMap();
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			delegate.setTypeMap( map );
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			delegate.setHoldability( holdability );
		}

		@Override
		public int getHoldability() throws SQLException {
			return delegate.getHoldability();
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return delegate.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return delegate.setSavepoint( name );
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			delegate.rollback( savepoint );
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			delegate.releaseSavepoint( savepoint );
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return delegate.createStatement( resultSetType, resultSetConcurrency, resultSetHoldability );
		}

		@Override
		public PreparedStatement prepareStatement(
				String sql,
				int resultSetType,
				int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return delegate.prepareStatement( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
		}

		@Override
		public CallableStatement prepareCall(
				String sql,
				int resultSetType,
				int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return delegate.prepareCall( sql, resultSetType, resultSetConcurrency, resultSetHoldability );
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return new PrepareStatementWrapper( delegate.prepareStatement( sql, autoGeneratedKeys ) );
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return new PrepareStatementWrapper( delegate.prepareStatement( sql, columnIndexes ) );
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return new PrepareStatementWrapper( delegate.prepareStatement( sql, columnNames ) );
		}

		@Override
		public Clob createClob() throws SQLException {
			return delegate.createClob();
		}

		@Override
		public Blob createBlob() throws SQLException {
			return delegate.createBlob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			return delegate.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return delegate.createSQLXML();
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return delegate.isValid( timeout );
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			delegate.setClientInfo( name, value );
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			delegate.setClientInfo( properties );
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return delegate.getClientInfo( name );
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return delegate.getClientInfo();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return delegate.createArrayOf( typeName, elements );
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return delegate.createStruct( typeName, attributes );
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			delegate.setSchema( schema );
		}

		@Override
		public String getSchema() throws SQLException {
			return delegate.getSchema();
		}

		@Override
		public void abort(Executor executor) throws SQLException {
			delegate.abort( executor );
		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			delegate.setNetworkTimeout( executor, milliseconds );
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return delegate.getNetworkTimeout();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap( iface );
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor( iface );
		}
	}

}
