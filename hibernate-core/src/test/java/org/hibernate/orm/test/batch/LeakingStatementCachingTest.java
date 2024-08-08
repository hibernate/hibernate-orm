/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.PrintWriter;
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
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.stream.LongStream;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Executes a ConnectionPool with a single connection to force re-usage of a ResourceRegistry, and leaks non-proxied
 * Statements from ResultSets to validate those Statements aren't used in trying to evict statements from the cache.
 *
 * @author Michael Clarke
 * @see ResultSetWrapper#getStatement()
 * @see StatementWrapper#equals(Object)
 */
@RequiresDialect(H2Dialect.class)
@SessionFactory
@DomainModel( annotatedClasses = LeakingStatementCachingTest.BaseEntity.class )
@ServiceRegistry( serviceContributors = LeakingStatementCachingTest.ConnectionProviderServiceContributor.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18325" )
class LeakingStatementCachingTest {

	@Test
	void testPersistMultipleEntities(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			LongStream.range(1, 10)
				.mapToObj(BaseEntity::new)
				.forEach(session::persist);

			assertThat(session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources()).isFalse();
		});
	}

	@Entity( name = "BaseEntity" )
	public static class BaseEntity {
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Id
		private Long id;

		private Long other;

		public BaseEntity() {
		}

		public BaseEntity(Long other) {
			this.other = other;
		}
	}

	public static class ConnectionProviderServiceContributor implements ServiceContributor {

		@Override
		public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
            try {
                serviceRegistryBuilder.applySetting(AvailableSettings.DATASOURCE, new SingleConnectionDataSource());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }

			serviceRegistryBuilder.getSettings().remove(AvailableSettings.USER);
			serviceRegistryBuilder.getSettings().remove(AvailableSettings.PASS);


			serviceRegistryBuilder.applySetting(AvailableSettings.CONNECTION_PROVIDER, new DatasourceConnectionProviderImpl());
			serviceRegistryBuilder.applySetting(AvailableSettings.SHOW_SQL, false);
			serviceRegistryBuilder.applySetting(AvailableSettings.LOG_JDBC_WARNINGS, false);
			serviceRegistryBuilder.getSettings().remove(AvailableSettings.URL);
		}
	}

	private static class SingleConnectionDataSource implements javax.sql.DataSource {

		private final BlockingQueue<Connection> connectionQueue = new ArrayBlockingQueue<>(1);

		public SingleConnectionDataSource() throws SQLException {
			DriverManagerConnectionProviderImpl connectionProvider = new DriverManagerConnectionProviderImpl();
			connectionProvider.configure( PropertiesHelper.map( ConnectionProviderBuilder.getConnectionProviderProperties() ) );
			connectionQueue.add(new ConnectionWrapper(connectionProvider.getConnection(), this));
		}

		@Override
		public Connection getConnection() throws SQLException {
            try {
                return connectionQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			throw new UnsupportedOperationException();
		}

		@Override
		public PrintWriter getLogWriter() throws SQLException {
			return null;
		}

		@Override
		public void setLogWriter(PrintWriter out) throws SQLException {

		}

		@Override
		public void setLoginTimeout(int seconds) throws SQLException {

		}

		@Override
		public int getLoginTimeout() throws SQLException {
			return 0;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return null;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

		private void returnConnection(Connection connection) {
			connectionQueue.add(connection);
		}
	}

	private static class ConnectionWrapper implements Connection {

		private final SingleConnectionDataSource owner;
		private final Connection delegate;

        private ConnectionWrapper(Connection delegate, SingleConnectionDataSource owner) {
            this.delegate = delegate;
			this.owner = owner;
        }

		@Override
		public Statement createStatement() throws SQLException {
			return new StatementWrapper(delegate.createStatement());
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql));
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return delegate.prepareCall(sql);
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return delegate.nativeSQL(sql);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			delegate.setAutoCommit(autoCommit);
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
			owner.returnConnection(this);
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
			delegate.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return delegate.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			delegate.setCatalog(catalog);
		}

		@Override
		public String getCatalog() throws SQLException {
			return delegate.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			delegate.setTransactionIsolation(level);
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
			return new StatementWrapper(delegate.createStatement(resultSetType, resultSetConcurrency));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return delegate.getTypeMap();
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			delegate.setTypeMap(map);
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			delegate.setHoldability(holdability);
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
			return delegate.setSavepoint(name);
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			delegate.rollback(savepoint);
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			delegate.releaseSavepoint(savepoint);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return new StatementWrapper(delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql, autoGeneratedKeys));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql, columnIndexes));
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return new PreparedStatementWrapper(delegate.prepareStatement(sql, columnNames));
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
			return delegate.isValid(timeout);
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			delegate.setClientInfo(name, value);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			delegate.setClientInfo(properties);
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			return delegate.getClientInfo(name);
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			return delegate.getClientInfo();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return delegate.createArrayOf(typeName, elements);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return delegate.createStruct(typeName, attributes);
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			delegate.setSchema(schema);
		}

		@Override
		public String getSchema() throws SQLException {
			return delegate.getSchema();
		}

		@Override
		public void abort(Executor executor) throws SQLException {
			delegate.abort(executor);
		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			delegate.setNetworkTimeout(executor, milliseconds);
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return delegate.getNetworkTimeout();
		}

		@Override
		public void beginRequest() throws SQLException {
			delegate.beginRequest();
		}

		@Override
		public void endRequest() throws SQLException {
			delegate.endRequest();
		}

		@Override
		public boolean setShardingKeyIfValid(ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
			return delegate.setShardingKeyIfValid(shardingKey, superShardingKey, timeout);
		}

		@Override
		public boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout) throws SQLException {
			return delegate.setShardingKeyIfValid(shardingKey, timeout);
		}

		@Override
		public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey) throws SQLException {
			delegate.setShardingKey(shardingKey, superShardingKey);
		}

		@Override
		public void setShardingKey(ShardingKey shardingKey) throws SQLException {
			delegate.setShardingKey(shardingKey);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}
	}

	private static class StatementWrapper implements Statement {

		private final Statement delegate;
		
		private StatementWrapper(Statement delegate) {
			this.delegate = delegate;
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			// This will detect where we're trying to compare a non-proxied Statement to a proxied one so cause a HashMap's comparison to fail
			return other == this || other instanceof StatementWrapper && delegate.equals(((StatementWrapper) other).delegate);
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException {
			return new ResultSetWrapper(delegate.executeQuery(sql), this);
		}

		@Override
		public int executeUpdate(String sql) throws SQLException {
			return delegate.executeUpdate(sql);
		}

		@Override
		public void close() throws SQLException {
			delegate.close();
		}

		@Override
		public int getMaxFieldSize() throws SQLException {
			return delegate.getMaxFieldSize();
		}

		@Override
		public void setMaxFieldSize(int max) throws SQLException {
			delegate.setMaxFieldSize(max);
		}

		@Override
		public int getMaxRows() throws SQLException {
			return delegate.getMaxRows();
		}

		@Override
		public void setMaxRows(int max) throws SQLException {
			delegate.setMaxRows(max);
		}

		@Override
		public void setEscapeProcessing(boolean enable) throws SQLException {
			delegate.setEscapeProcessing(enable);
		}

		@Override
		public int getQueryTimeout() throws SQLException {
			return delegate.getQueryTimeout();
		}

		@Override
		public void setQueryTimeout(int seconds) throws SQLException {
			delegate.setQueryTimeout(seconds);
		}

		@Override
		public void cancel() throws SQLException {
			delegate.cancel();
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
		public void setCursorName(String name) throws SQLException {
			delegate.setCursorName(name);
		}

		@Override
		public boolean execute(String sql) throws SQLException {
			return delegate.execute(sql);
		}

		@Override
		public ResultSet getResultSet() throws SQLException {
			return delegate.getResultSet();
		}

		@Override
		public int getUpdateCount() throws SQLException {
			return delegate.getUpdateCount();
		}

		@Override
		public boolean getMoreResults() throws SQLException {
			return delegate.getMoreResults();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			delegate.setFetchDirection(direction);
		}

		@Override
		public int getFetchDirection() throws SQLException {
			return delegate.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			delegate.setFetchSize(rows);
		}

		@Override
		public int getFetchSize() throws SQLException {
			return delegate.getFetchSize();
		}

		@Override
		public int getResultSetConcurrency() throws SQLException {
			return delegate.getResultSetConcurrency();
		}

		@Override
		public int getResultSetType() throws SQLException {
			return delegate.getResultSetType();
		}

		@Override
		public void addBatch(String sql) throws SQLException {
			delegate.addBatch(sql);
		}

		@Override
		public void clearBatch() throws SQLException {
			delegate.clearBatch();
		}

		@Override
		public int[] executeBatch() throws SQLException {
			return delegate.executeBatch();
		}

		@Override
		public Connection getConnection() throws SQLException {
			return delegate.getConnection();
		}

		@Override
		public boolean getMoreResults(int current) throws SQLException {
			return delegate.getMoreResults(current);
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException {
			return new ResultSetWrapper(delegate.getGeneratedKeys(), this);
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.executeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
			return delegate.executeUpdate(sql, columnIndexes);
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames) throws SQLException {
			return delegate.executeUpdate(sql, columnNames);
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.execute(sql, autoGeneratedKeys);
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes) throws SQLException {
			return delegate.execute(sql, columnIndexes);
		}

		@Override
		public boolean execute(String sql, String[] columnNames) throws SQLException {
			return delegate.execute(sql, columnNames);
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			return delegate.getResultSetHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

		@Override
		public void setPoolable(boolean poolable) throws SQLException {
			delegate.setPoolable(poolable);
		}

		@Override
		public boolean isPoolable() throws SQLException {
			return delegate.isPoolable();
		}

		@Override
		public void closeOnCompletion() throws SQLException {
			delegate.closeOnCompletion();
		}

		@Override
		public boolean isCloseOnCompletion() throws SQLException {
			return delegate.isCloseOnCompletion();
		}

		@Override
		public long getLargeUpdateCount() throws SQLException {
			return delegate.getLargeUpdateCount();
		}

		@Override
		public void setLargeMaxRows(long max) throws SQLException {
			delegate.setLargeMaxRows(max);
		}

		@Override
		public long getLargeMaxRows() throws SQLException {
			return delegate.getLargeMaxRows();
		}

		@Override
		public long[] executeLargeBatch() throws SQLException {
			return delegate.executeLargeBatch();
		}

		@Override
		public long executeLargeUpdate(String sql) throws SQLException {
			return delegate.executeLargeUpdate(sql);
		}

		@Override
		public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.executeLargeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
			return delegate.executeLargeUpdate(sql, columnIndexes);
		}

		@Override
		public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
			return delegate.executeLargeUpdate(sql, columnNames);
		}

		@Override
		public String enquoteLiteral(String val) throws SQLException {
			return delegate.enquoteLiteral(val);
		}

		@Override
		public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
			return delegate.enquoteIdentifier(identifier, alwaysQuote);
		}

		@Override
		public boolean isSimpleIdentifier(String identifier) throws SQLException {
			return delegate.isSimpleIdentifier(identifier);
		}

		@Override
		public String enquoteNCharLiteral(String val) throws SQLException {
			return delegate.enquoteNCharLiteral(val);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}
	}

	private static class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {

		private final PreparedStatement delegate;

		private PreparedStatementWrapper(PreparedStatement delegate) {
			super(delegate);
			this.delegate = delegate;
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
			delegate.setNull(parameterIndex, sqlType);
		}

		@Override
		public void setBoolean(int parameterIndex, boolean x) throws SQLException {
			delegate.setBoolean(parameterIndex, x);
		}

		@Override
		public void setByte(int parameterIndex, byte x) throws SQLException {
			delegate.setByte(parameterIndex, x);
		}

		@Override
		public void setShort(int parameterIndex, short x) throws SQLException {
			delegate.setShort(parameterIndex, x);
		}

		@Override
		public void setInt(int parameterIndex, int x) throws SQLException {
			delegate.setInt(parameterIndex, x);
		}

		@Override
		public void setLong(int parameterIndex, long x) throws SQLException {
			delegate.setLong(parameterIndex, x);
		}

		@Override
		public void setFloat(int parameterIndex, float x) throws SQLException {
			delegate.setFloat(parameterIndex, x);
		}

		@Override
		public void setDouble(int parameterIndex, double x) throws SQLException {
			delegate.setDouble(parameterIndex, x);
		}

		@Override
		public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
			delegate.setBigDecimal(parameterIndex, x);
		}

		@Override
		public void setString(int parameterIndex, String x) throws SQLException {
			delegate.setString(parameterIndex, x);
		}

		@Override
		public void setBytes(int parameterIndex, byte[] x) throws SQLException {
			delegate.setBytes(parameterIndex, x);
		}

		@Override
		public void setDate(int parameterIndex, Date x) throws SQLException {
			delegate.setDate(parameterIndex, x);
		}

		@Override
		public void setTime(int parameterIndex, Time x) throws SQLException {
			delegate.setTime(parameterIndex, x);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
			delegate.setTimestamp(parameterIndex, x);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		@Deprecated(since = "1.2")
		public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setUnicodeStream(parameterIndex, x, length);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
			delegate.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void clearParameters() throws SQLException {
			delegate.clearParameters();
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
			delegate.setObject(parameterIndex, x, targetSqlType);
		}

		@Override
		public void setObject(int parameterIndex, Object x) throws SQLException {
			delegate.setObject(parameterIndex, x);
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
			delegate.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setRef(int parameterIndex, Ref x) throws SQLException {
			delegate.setRef(parameterIndex, x);
		}

		@Override
		public void setBlob(int parameterIndex, Blob x) throws SQLException {
			delegate.setBlob(parameterIndex, x);
		}

		@Override
		public void setClob(int parameterIndex, Clob x) throws SQLException {
			delegate.setClob(parameterIndex, x);
		}

		@Override
		public void setArray(int parameterIndex, Array x) throws SQLException {
			delegate.setArray(parameterIndex, x);
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			return delegate.getMetaData();
		}

		@Override
		public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
			delegate.setDate(parameterIndex, x, cal);
		}

		@Override
		public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
			delegate.setTime(parameterIndex, x, cal);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
			delegate.setTimestamp(parameterIndex, x, cal);
		}

		@Override
		public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
			delegate.setNull(parameterIndex, sqlType, typeName);
		}

		@Override
		public void setURL(int parameterIndex, URL x) throws SQLException {
			delegate.setURL(parameterIndex, x);
		}

		@Override
		public ParameterMetaData getParameterMetaData() throws SQLException {
			return delegate.getParameterMetaData();
		}

		@Override
		public void setRowId(int parameterIndex, RowId x) throws SQLException {
			delegate.setRowId(parameterIndex, x);
		}

		@Override
		public void setNString(int parameterIndex, String value) throws SQLException {
			delegate.setNString(parameterIndex, value);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
			delegate.setNCharacterStream(parameterIndex, value, length);
		}

		@Override
		public void setNClob(int parameterIndex, NClob value) throws SQLException {
			delegate.setNClob(parameterIndex, value);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setClob(parameterIndex, reader, length);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
			delegate.setBlob(parameterIndex, inputStream, length);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setNClob(parameterIndex, reader, length);
		}

		@Override
		public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
			delegate.setSQLXML(parameterIndex, xmlObject);
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
			delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
			delegate.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
			delegate.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
			delegate.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
			delegate.setAsciiStream(parameterIndex, x);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
			delegate.setBinaryStream(parameterIndex, x);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
			delegate.setCharacterStream(parameterIndex, reader);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
			delegate.setNCharacterStream(parameterIndex, value);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader) throws SQLException {
			delegate.setClob(parameterIndex, reader);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
			delegate.setBlob(parameterIndex, inputStream);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader) throws SQLException {
			delegate.setNClob(parameterIndex, reader);
		}

		@Override
		public void setObject(int parameterIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
			delegate.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void setObject(int parameterIndex, Object x, SQLType targetSqlType) throws SQLException {
			delegate.setObject(parameterIndex, x, targetSqlType);
		}

		@Override
		public long executeLargeUpdate() throws SQLException {
			return delegate.executeLargeUpdate();
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException {
			return delegate.executeQuery(sql);
		}

		@Override
		public int executeUpdate(String sql) throws SQLException {
			return delegate.executeUpdate(sql);
		}

		@Override
		public void close() throws SQLException {
			delegate.close();
		}

		@Override
		public int getMaxFieldSize() throws SQLException {
			return delegate.getMaxFieldSize();
		}

		@Override
		public void setMaxFieldSize(int max) throws SQLException {
			delegate.setMaxFieldSize(max);
		}

		@Override
		public int getMaxRows() throws SQLException {
			return delegate.getMaxRows();
		}

		@Override
		public void setMaxRows(int max) throws SQLException {
			delegate.setMaxRows(max);
		}

		@Override
		public void setEscapeProcessing(boolean enable) throws SQLException {
			delegate.setEscapeProcessing(enable);
		}

		@Override
		public int getQueryTimeout() throws SQLException {
			return delegate.getQueryTimeout();
		}

		@Override
		public void setQueryTimeout(int seconds) throws SQLException {
			delegate.setQueryTimeout(seconds);
		}

		@Override
		public void cancel() throws SQLException {
			delegate.cancel();
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
		public void setCursorName(String name) throws SQLException {
			delegate.setCursorName(name);
		}

		@Override
		public boolean execute(String sql) throws SQLException {
			return delegate.execute(sql);
		}

		@Override
		public ResultSet getResultSet() throws SQLException {
			return new ResultSetWrapper(delegate.getResultSet(), this);
		}

		@Override
		public int getUpdateCount() throws SQLException {
			return delegate.getUpdateCount();
		}

		@Override
		public boolean getMoreResults() throws SQLException {
			return delegate.getMoreResults();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			delegate.setFetchDirection(direction);
		}

		@Override
		public int getFetchDirection() throws SQLException {
			return delegate.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			delegate.setFetchSize(rows);
		}

		@Override
		public int getFetchSize() throws SQLException {
			return delegate.getFetchSize();
		}

		@Override
		public int getResultSetConcurrency() throws SQLException {
			return delegate.getResultSetConcurrency();
		}

		@Override
		public int getResultSetType() throws SQLException {
			return delegate.getResultSetType();
		}

		@Override
		public void addBatch(String sql) throws SQLException {
			delegate.addBatch(sql);
		}

		@Override
		public void clearBatch() throws SQLException {
			delegate.clearBatch();
		}

		@Override
		public int[] executeBatch() throws SQLException {
			return delegate.executeBatch();
		}

		@Override
		public Connection getConnection() throws SQLException {
			return delegate.getConnection();
		}

		@Override
		public boolean getMoreResults(int current) throws SQLException {
			return delegate.getMoreResults(current);
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException {
			return new ResultSetWrapper(delegate.getGeneratedKeys(), this);
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.executeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
			return delegate.executeUpdate(sql, columnIndexes);
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames) throws SQLException {
			return delegate.executeUpdate(sql, columnNames);
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.execute(sql, autoGeneratedKeys);
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes) throws SQLException {
			return delegate.execute(sql, columnIndexes);
		}

		@Override
		public boolean execute(String sql, String[] columnNames) throws SQLException {
			return delegate.execute(sql, columnNames);
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			return delegate.getResultSetHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

		@Override
		public void setPoolable(boolean poolable) throws SQLException {
			delegate.setPoolable(poolable);
		}

		@Override
		public boolean isPoolable() throws SQLException {
			return delegate.isPoolable();
		}

		@Override
		public void closeOnCompletion() throws SQLException {
			delegate.closeOnCompletion();
		}

		@Override
		public boolean isCloseOnCompletion() throws SQLException {
			return delegate.isCloseOnCompletion();
		}

		@Override
		public long getLargeUpdateCount() throws SQLException {
			return delegate.getLargeUpdateCount();
		}

		@Override
		public void setLargeMaxRows(long max) throws SQLException {
			delegate.setLargeMaxRows(max);
		}

		@Override
		public long getLargeMaxRows() throws SQLException {
			return delegate.getLargeMaxRows();
		}

		@Override
		public long[] executeLargeBatch() throws SQLException {
			return delegate.executeLargeBatch();
		}

		@Override
		public long executeLargeUpdate(String sql) throws SQLException {
			return delegate.executeLargeUpdate(sql);
		}

		@Override
		public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
			return delegate.executeLargeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
			return delegate.executeLargeUpdate(sql, columnIndexes);
		}

		@Override
		public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
			return delegate.executeLargeUpdate(sql, columnNames);
		}

		@Override
		public String enquoteLiteral(String val) throws SQLException {
			return delegate.enquoteLiteral(val);
		}

		@Override
		public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
			return delegate.enquoteIdentifier(identifier, alwaysQuote);
		}

		@Override
		public boolean isSimpleIdentifier(String identifier) throws SQLException {
			return delegate.isSimpleIdentifier(identifier);
		}

		@Override
		public String enquoteNCharLiteral(String val) throws SQLException {
			return delegate.enquoteNCharLiteral(val);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}
	}

	private static class ResultSetWrapper implements ResultSet {

		private final ResultSet delegate;
		private final StatementWrapper owner;

        private ResultSetWrapper(ResultSet delegate, StatementWrapper owner) {
            this.delegate = delegate;
            this.owner = owner;
        }

		@Override
		public boolean next() throws SQLException {
			return delegate.next();
		}

		@Override
		public void close() throws SQLException {
			delegate.close();
		}

		@Override
		public boolean wasNull() throws SQLException {
			return delegate.wasNull();
		}

		@Override
		public String getString(int columnIndex) throws SQLException {
			return delegate.getString(columnIndex);
		}

		@Override
		public boolean getBoolean(int columnIndex) throws SQLException {
			return delegate.getBoolean(columnIndex);
		}

		@Override
		public byte getByte(int columnIndex) throws SQLException {
			return delegate.getByte(columnIndex);
		}

		@Override
		public short getShort(int columnIndex) throws SQLException {
			return delegate.getShort(columnIndex);
		}

		@Override
		public int getInt(int columnIndex) throws SQLException {
			return delegate.getInt(columnIndex);
		}

		@Override
		public long getLong(int columnIndex) throws SQLException {
			return delegate.getLong(columnIndex);
		}

		@Override
		public float getFloat(int columnIndex) throws SQLException {
			return delegate.getFloat(columnIndex);
		}

		@Override
		public double getDouble(int columnIndex) throws SQLException {
			return delegate.getDouble(columnIndex);
		}

		@Override
		@Deprecated(since = "1.2")
		public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
			return delegate.getBigDecimal(columnIndex, scale);
		}

		@Override
		public byte[] getBytes(int columnIndex) throws SQLException {
			return delegate.getBytes(columnIndex);
		}

		@Override
		public Date getDate(int columnIndex) throws SQLException {
			return delegate.getDate(columnIndex);
		}

		@Override
		public Time getTime(int columnIndex) throws SQLException {
			return delegate.getTime(columnIndex);
		}

		@Override
		public Timestamp getTimestamp(int columnIndex) throws SQLException {
			return delegate.getTimestamp(columnIndex);
		}

		@Override
		public InputStream getAsciiStream(int columnIndex) throws SQLException {
			return delegate.getAsciiStream(columnIndex);
		}

		@Override
		@Deprecated(since = "1.2")
		public InputStream getUnicodeStream(int columnIndex) throws SQLException {
			return delegate.getUnicodeStream(columnIndex);
		}

		@Override
		public InputStream getBinaryStream(int columnIndex) throws SQLException {
			return delegate.getBinaryStream(columnIndex);
		}

		@Override
		public String getString(String columnLabel) throws SQLException {
			return delegate.getString(columnLabel);
		}

		@Override
		public boolean getBoolean(String columnLabel) throws SQLException {
			return delegate.getBoolean(columnLabel);
		}

		@Override
		public byte getByte(String columnLabel) throws SQLException {
			return delegate.getByte(columnLabel);
		}

		@Override
		public short getShort(String columnLabel) throws SQLException {
			return delegate.getShort(columnLabel);
		}

		@Override
		public int getInt(String columnLabel) throws SQLException {
			return delegate.getInt(columnLabel);
		}

		@Override
		public long getLong(String columnLabel) throws SQLException {
			return delegate.getLong(columnLabel);
		}

		@Override
		public float getFloat(String columnLabel) throws SQLException {
			return delegate.getFloat(columnLabel);
		}

		@Override
		public double getDouble(String columnLabel) throws SQLException {
			return delegate.getDouble(columnLabel);
		}

		@Override
		@Deprecated(since = "1.2")
		public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
			return delegate.getBigDecimal(columnLabel, scale);
		}

		@Override
		public byte[] getBytes(String columnLabel) throws SQLException {
			return delegate.getBytes(columnLabel);
		}

		@Override
		public Date getDate(String columnLabel) throws SQLException {
			return delegate.getDate(columnLabel);
		}

		@Override
		public Time getTime(String columnLabel) throws SQLException {
			return delegate.getTime(columnLabel);
		}

		@Override
		public Timestamp getTimestamp(String columnLabel) throws SQLException {
			return delegate.getTimestamp(columnLabel);
		}

		@Override
		public InputStream getAsciiStream(String columnLabel) throws SQLException {
			return delegate.getAsciiStream(columnLabel);
		}

		@Override
		@Deprecated(since = "1.2")
		public InputStream getUnicodeStream(String columnLabel) throws SQLException {
			return delegate.getUnicodeStream(columnLabel);
		}

		@Override
		public InputStream getBinaryStream(String columnLabel) throws SQLException {
			return delegate.getBinaryStream(columnLabel);
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
		public String getCursorName() throws SQLException {
			return delegate.getCursorName();
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException {
			return delegate.getMetaData();
		}

		@Override
		public Object getObject(int columnIndex) throws SQLException {
			return delegate.getObject(columnIndex);
		}

		@Override
		public Object getObject(String columnLabel) throws SQLException {
			return delegate.getObject(columnLabel);
		}

		@Override
		public int findColumn(String columnLabel) throws SQLException {
			return delegate.findColumn(columnLabel);
		}

		@Override
		public Reader getCharacterStream(int columnIndex) throws SQLException {
			return delegate.getCharacterStream(columnIndex);
		}

		@Override
		public Reader getCharacterStream(String columnLabel) throws SQLException {
			return delegate.getCharacterStream(columnLabel);
		}

		@Override
		public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
			return delegate.getBigDecimal(columnIndex);
		}

		@Override
		public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
			return delegate.getBigDecimal(columnLabel);
		}

		@Override
		public boolean isBeforeFirst() throws SQLException {
			return delegate.isBeforeFirst();
		}

		@Override
		public boolean isAfterLast() throws SQLException {
			return delegate.isAfterLast();
		}

		@Override
		public boolean isFirst() throws SQLException {
			return delegate.isFirst();
		}

		@Override
		public boolean isLast() throws SQLException {
			return delegate.isLast();
		}

		@Override
		public void beforeFirst() throws SQLException {
			delegate.beforeFirst();
		}

		@Override
		public void afterLast() throws SQLException {
			delegate.afterLast();
		}

		@Override
		public boolean first() throws SQLException {
			return delegate.first();
		}

		@Override
		public boolean last() throws SQLException {
			return delegate.last();
		}

		@Override
		public int getRow() throws SQLException {
			return delegate.getRow();
		}

		@Override
		public boolean absolute(int row) throws SQLException {
			return delegate.absolute(row);
		}

		@Override
		public boolean relative(int rows) throws SQLException {
			return delegate.relative(rows);
		}

		@Override
		public boolean previous() throws SQLException {
			return delegate.previous();
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException {
			delegate.setFetchDirection(direction);
		}

		@Override
		public int getFetchDirection() throws SQLException {
			return delegate.getFetchDirection();
		}

		@Override
		public void setFetchSize(int rows) throws SQLException {
			delegate.setFetchSize(rows);
		}

		@Override
		public int getFetchSize() throws SQLException {
			return delegate.getFetchSize();
		}

		@Override
		public int getType() throws SQLException {
			return delegate.getType();
		}

		@Override
		public int getConcurrency() throws SQLException {
			return delegate.getConcurrency();
		}

		@Override
		public boolean rowUpdated() throws SQLException {
			return delegate.rowUpdated();
		}

		@Override
		public boolean rowInserted() throws SQLException {
			return delegate.rowInserted();
		}

		@Override
		public boolean rowDeleted() throws SQLException {
			return delegate.rowDeleted();
		}

		@Override
		public void updateNull(int columnIndex) throws SQLException {
			delegate.updateNull(columnIndex);
		}

		@Override
		public void updateBoolean(int columnIndex, boolean x) throws SQLException {
			delegate.updateBoolean(columnIndex, x);
		}

		@Override
		public void updateByte(int columnIndex, byte x) throws SQLException {
			delegate.updateByte(columnIndex, x);
		}

		@Override
		public void updateShort(int columnIndex, short x) throws SQLException {
			delegate.updateShort(columnIndex, x);
		}

		@Override
		public void updateInt(int columnIndex, int x) throws SQLException {
			delegate.updateInt(columnIndex, x);
		}

		@Override
		public void updateLong(int columnIndex, long x) throws SQLException {
			delegate.updateLong(columnIndex, x);
		}

		@Override
		public void updateFloat(int columnIndex, float x) throws SQLException {
			delegate.updateFloat(columnIndex, x);
		}

		@Override
		public void updateDouble(int columnIndex, double x) throws SQLException {
			delegate.updateDouble(columnIndex, x);
		}

		@Override
		public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
			delegate.updateBigDecimal(columnIndex, x);
		}

		@Override
		public void updateString(int columnIndex, String x) throws SQLException {
			delegate.updateString(columnIndex, x);
		}

		@Override
		public void updateBytes(int columnIndex, byte[] x) throws SQLException {
			delegate.updateBytes(columnIndex, x);
		}

		@Override
		public void updateDate(int columnIndex, Date x) throws SQLException {
			delegate.updateDate(columnIndex, x);
		}

		@Override
		public void updateTime(int columnIndex, Time x) throws SQLException {
			delegate.updateTime(columnIndex, x);
		}

		@Override
		public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
			delegate.updateTimestamp(columnIndex, x);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
			delegate.updateAsciiStream(columnIndex, x, length);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
			delegate.updateBinaryStream(columnIndex, x, length);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
			delegate.updateCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
			delegate.updateObject(columnIndex, x, scaleOrLength);
		}

		@Override
		public void updateObject(int columnIndex, Object x) throws SQLException {
			delegate.updateObject(columnIndex, x);
		}

		@Override
		public void updateNull(String columnLabel) throws SQLException {
			delegate.updateNull(columnLabel);
		}

		@Override
		public void updateBoolean(String columnLabel, boolean x) throws SQLException {
			delegate.updateBoolean(columnLabel, x);
		}

		@Override
		public void updateByte(String columnLabel, byte x) throws SQLException {
			delegate.updateByte(columnLabel, x);
		}

		@Override
		public void updateShort(String columnLabel, short x) throws SQLException {
			delegate.updateShort(columnLabel, x);
		}

		@Override
		public void updateInt(String columnLabel, int x) throws SQLException {
			delegate.updateInt(columnLabel, x);
		}

		@Override
		public void updateLong(String columnLabel, long x) throws SQLException {
			delegate.updateLong(columnLabel, x);
		}

		@Override
		public void updateFloat(String columnLabel, float x) throws SQLException {
			delegate.updateFloat(columnLabel, x);
		}

		@Override
		public void updateDouble(String columnLabel, double x) throws SQLException {
			delegate.updateDouble(columnLabel, x);
		}

		@Override
		public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
			delegate.updateBigDecimal(columnLabel, x);
		}

		@Override
		public void updateString(String columnLabel, String x) throws SQLException {
			delegate.updateString(columnLabel, x);
		}

		@Override
		public void updateBytes(String columnLabel, byte[] x) throws SQLException {
			delegate.updateBytes(columnLabel, x);
		}

		@Override
		public void updateDate(String columnLabel, Date x) throws SQLException {
			delegate.updateDate(columnLabel, x);
		}

		@Override
		public void updateTime(String columnLabel, Time x) throws SQLException {
			delegate.updateTime(columnLabel, x);
		}

		@Override
		public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
			delegate.updateTimestamp(columnLabel, x);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
			delegate.updateAsciiStream(columnLabel, x, length);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
			delegate.updateBinaryStream(columnLabel, x, length);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
			delegate.updateCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
			delegate.updateObject(columnLabel, x, scaleOrLength);
		}

		@Override
		public void updateObject(String columnLabel, Object x) throws SQLException {
			delegate.updateObject(columnLabel, x);
		}

		@Override
		public void insertRow() throws SQLException {
			delegate.insertRow();
		}

		@Override
		public void updateRow() throws SQLException {
			delegate.updateRow();
		}

		@Override
		public void deleteRow() throws SQLException {
			delegate.deleteRow();
		}

		@Override
		public void refreshRow() throws SQLException {
			delegate.refreshRow();
		}

		@Override
		public void cancelRowUpdates() throws SQLException {
			delegate.cancelRowUpdates();
		}

		@Override
		public void moveToInsertRow() throws SQLException {
			delegate.moveToInsertRow();
		}

		@Override
		public void moveToCurrentRow() throws SQLException {
			delegate.moveToCurrentRow();
		}

		@Override
		public Statement getStatement() throws SQLException {
			// Note: we're purposefully not wrapping this in a StatementWrapper so that the underling database statement is leaked into Hibernate
			return delegate.getStatement();
		}

		@Override
		public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject(columnIndex, map);
		}

		@Override
		public Ref getRef(int columnIndex) throws SQLException {
			return delegate.getRef(columnIndex);
		}

		@Override
		public Blob getBlob(int columnIndex) throws SQLException {
			return delegate.getBlob(columnIndex);
		}

		@Override
		public Clob getClob(int columnIndex) throws SQLException {
			return delegate.getClob(columnIndex);
		}

		@Override
		public Array getArray(int columnIndex) throws SQLException {
			return delegate.getArray(columnIndex);
		}

		@Override
		public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
			return delegate.getObject(columnLabel, map);
		}

		@Override
		public Ref getRef(String columnLabel) throws SQLException {
			return delegate.getRef(columnLabel);
		}

		@Override
		public Blob getBlob(String columnLabel) throws SQLException {
			return delegate.getBlob(columnLabel);
		}

		@Override
		public Clob getClob(String columnLabel) throws SQLException {
			return delegate.getClob(columnLabel);
		}

		@Override
		public Array getArray(String columnLabel) throws SQLException {
			return delegate.getArray(columnLabel);
		}

		@Override
		public Date getDate(int columnIndex, Calendar cal) throws SQLException {
			return delegate.getDate(columnIndex, cal);
		}

		@Override
		public Date getDate(String columnLabel, Calendar cal) throws SQLException {
			return delegate.getDate(columnLabel, cal);
		}

		@Override
		public Time getTime(int columnIndex, Calendar cal) throws SQLException {
			return delegate.getTime(columnIndex, cal);
		}

		@Override
		public Time getTime(String columnLabel, Calendar cal) throws SQLException {
			return delegate.getTime(columnLabel, cal);
		}

		@Override
		public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
			return delegate.getTimestamp(columnIndex, cal);
		}

		@Override
		public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
			return delegate.getTimestamp(columnLabel, cal);
		}

		@Override
		public URL getURL(int columnIndex) throws SQLException {
			return delegate.getURL(columnIndex);
		}

		@Override
		public URL getURL(String columnLabel) throws SQLException {
			return delegate.getURL(columnLabel);
		}

		@Override
		public void updateRef(int columnIndex, Ref x) throws SQLException {
			delegate.updateRef(columnIndex, x);
		}

		@Override
		public void updateRef(String columnLabel, Ref x) throws SQLException {
			delegate.updateRef(columnLabel, x);
		}

		@Override
		public void updateBlob(int columnIndex, Blob x) throws SQLException {
			delegate.updateBlob(columnIndex, x);
		}

		@Override
		public void updateBlob(String columnLabel, Blob x) throws SQLException {
			delegate.updateBlob(columnLabel, x);
		}

		@Override
		public void updateClob(int columnIndex, Clob x) throws SQLException {
			delegate.updateClob(columnIndex, x);
		}

		@Override
		public void updateClob(String columnLabel, Clob x) throws SQLException {
			delegate.updateClob(columnLabel, x);
		}

		@Override
		public void updateArray(int columnIndex, Array x) throws SQLException {
			delegate.updateArray(columnIndex, x);
		}

		@Override
		public void updateArray(String columnLabel, Array x) throws SQLException {
			delegate.updateArray(columnLabel, x);
		}

		@Override
		public RowId getRowId(int columnIndex) throws SQLException {
			return delegate.getRowId(columnIndex);
		}

		@Override
		public RowId getRowId(String columnLabel) throws SQLException {
			return delegate.getRowId(columnLabel);
		}

		@Override
		public void updateRowId(int columnIndex, RowId x) throws SQLException {
			delegate.updateRowId(columnIndex, x);
		}

		@Override
		public void updateRowId(String columnLabel, RowId x) throws SQLException {
			delegate.updateRowId(columnLabel, x);
		}

		@Override
		public int getHoldability() throws SQLException {
			return delegate.getHoldability();
		}

		@Override
		public boolean isClosed() throws SQLException {
			return delegate.isClosed();
		}

		@Override
		public void updateNString(int columnIndex, String nString) throws SQLException {
			delegate.updateNString(columnIndex, nString);
		}

		@Override
		public void updateNString(String columnLabel, String nString) throws SQLException {
			delegate.updateNString(columnLabel, nString);
		}

		@Override
		public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
			delegate.updateNClob(columnIndex, nClob);
		}

		@Override
		public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
			delegate.updateNClob(columnLabel, nClob);
		}

		@Override
		public NClob getNClob(int columnIndex) throws SQLException {
			return delegate.getNClob(columnIndex);
		}

		@Override
		public NClob getNClob(String columnLabel) throws SQLException {
			return delegate.getNClob(columnLabel);
		}

		@Override
		public SQLXML getSQLXML(int columnIndex) throws SQLException {
			return delegate.getSQLXML(columnIndex);
		}

		@Override
		public SQLXML getSQLXML(String columnLabel) throws SQLException {
			return delegate.getSQLXML(columnLabel);
		}

		@Override
		public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
			delegate.updateSQLXML(columnIndex, xmlObject);
		}

		@Override
		public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
			delegate.updateSQLXML(columnLabel, xmlObject);
		}

		@Override
		public String getNString(int columnIndex) throws SQLException {
			return delegate.getNString(columnIndex);
		}

		@Override
		public String getNString(String columnLabel) throws SQLException {
			return delegate.getNString(columnLabel);
		}

		@Override
		public Reader getNCharacterStream(int columnIndex) throws SQLException {
			return delegate.getNCharacterStream(columnIndex);
		}

		@Override
		public Reader getNCharacterStream(String columnLabel) throws SQLException {
			return delegate.getNCharacterStream(columnLabel);
		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
			delegate.updateNCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
			delegate.updateNCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
			delegate.updateAsciiStream(columnIndex, x, length);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
			delegate.updateBinaryStream(columnIndex, x, length);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
			delegate.updateCharacterStream(columnIndex, x, length);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
			delegate.updateAsciiStream(columnLabel, x, length);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
			delegate.updateBinaryStream(columnLabel, x, length);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
			delegate.updateCharacterStream(columnLabel, reader, length);
		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
			delegate.updateBlob(columnIndex, inputStream, length);
		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
			delegate.updateBlob(columnLabel, inputStream, length);
		}

		@Override
		public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
			delegate.updateClob(columnIndex, reader, length);
		}

		@Override
		public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
			delegate.updateClob(columnLabel, reader, length);
		}

		@Override
		public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
			delegate.updateNClob(columnIndex, reader, length);
		}

		@Override
		public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
			delegate.updateNClob(columnLabel, reader, length);
		}

		@Override
		public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
			delegate.updateNCharacterStream(columnIndex, x);
		}

		@Override
		public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
			delegate.updateNCharacterStream(columnLabel, reader);
		}

		@Override
		public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
			delegate.updateAsciiStream(columnIndex, x);
		}

		@Override
		public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
			delegate.updateBinaryStream(columnIndex, x);
		}

		@Override
		public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
			delegate.updateCharacterStream(columnIndex, x);
		}

		@Override
		public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
			delegate.updateAsciiStream(columnLabel, x);
		}

		@Override
		public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
			delegate.updateBinaryStream(columnLabel, x);
		}

		@Override
		public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
			delegate.updateCharacterStream(columnLabel, reader);
		}

		@Override
		public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
			delegate.updateBlob(columnIndex, inputStream);
		}

		@Override
		public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
			delegate.updateBlob(columnLabel, inputStream);
		}

		@Override
		public void updateClob(int columnIndex, Reader reader) throws SQLException {
			delegate.updateClob(columnIndex, reader);
		}

		@Override
		public void updateClob(String columnLabel, Reader reader) throws SQLException {
			delegate.updateClob(columnLabel, reader);
		}

		@Override
		public void updateNClob(int columnIndex, Reader reader) throws SQLException {
			delegate.updateNClob(columnIndex, reader);
		}

		@Override
		public void updateNClob(String columnLabel, Reader reader) throws SQLException {
			delegate.updateNClob(columnLabel, reader);
		}

		@Override
		public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
			return delegate.getObject(columnIndex, type);
		}

		@Override
		public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
			return delegate.getObject(columnLabel, type);
		}

		@Override
		public void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
			delegate.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
			delegate.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
			delegate.updateObject(columnIndex, x, targetSqlType);
		}

		@Override
		public void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
			delegate.updateObject(columnLabel, x, targetSqlType);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return delegate.isWrapperFor(iface);
		}
	}
}
