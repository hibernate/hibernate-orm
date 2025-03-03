/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Beikov
 */
@SuppressWarnings({"unused"})
public class JdbcSpies {

	public interface Callback {
		void onCall(Object spy, Method method, Object[] args, Object result);
	}
	public static class SpyContext {
		private final Map<Method, Map<Object, List<Object[]>>> calls = new HashMap<>();
		private final List<Callback> callbacks = new ArrayList<>();

		private Object call(Object spy, Object realObject, Method method, Object[] args) throws Throwable {
			return onCall( spy, method, args, callOnly( realObject, method, args ) );
		}

		private Object callOnly(Object realObject, Method method, Object[] args) throws Throwable {
			try {
				return method.invoke( realObject, args );
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

		private <T> T onCall(Object spy, Method method, Object[] args, T result) {
			calls.computeIfAbsent( method, m -> new IdentityHashMap<>() )
					.computeIfAbsent( spy, s -> new ArrayList<>() )
					.add( args );
			for ( Callback callback : callbacks ) {
				callback.onCall( spy, method, args, result );
			}
			return result;
		}

		public SpyContext registerCallback(Callback callback) {
			callbacks.add( callback );
			return this;
		}

		public List<Object[]> getCalls(Method method, Object spy) {
			return calls.getOrDefault( method, Collections.emptyMap() ).getOrDefault( spy, Collections.emptyList() );
		}

		public void clear() {
			calls.clear();
		}

		public <T> T getSpiedInstance(T spy) {
			if ( Proxy.isProxyClass( spy.getClass() ) ) {
				final InvocationHandler invocationHandler = Proxy.getInvocationHandler( spy );
				if ( invocationHandler instanceof Spy ) {
					//noinspection unchecked
					return (T) ( (Spy) invocationHandler ).getSpiedInstance();
				}
			}
			throw new IllegalArgumentException( "Passed object is not a spy: " + spy );
		}
	}

	public static Connection spy(Connection connection, SpyContext context) {
		return (Connection) Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(),
				new Class[]{ Connection.class },
				new ConnectionHandler( connection, context )
		);
	}

	private interface Spy {
		Object getSpiedInstance();
	}

	private static class ConnectionHandler implements InvocationHandler, Spy {
		private final Connection connection;
		private final SpyContext context;
		private DatabaseMetaData databaseMetaDataProxy;

		public ConnectionHandler(Connection connection, SpyContext context) {
			this.connection = connection;
			this.context = context;
		}

		@Override
		public Object getSpiedInstance() {
			return connection;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "getMetaData":
					return context.onCall( proxy, method, args, getDatabaseMetaDataProxy( (Connection) proxy ) );
				case "createStatement":
					return context.onCall(
							proxy,
							method,
							args,
							createStatementProxy(
									(Statement) context.callOnly( connection, method, args ),
									(Connection) proxy
							)
					);
				case "prepareStatement":
					return context.onCall(
							proxy,
							method,
							args,
							prepareStatementProxy(
									(PreparedStatement) context.callOnly( connection, method, args ),
									(Connection) proxy
							)
					);
				case "prepareCall":
					return context.onCall(
							proxy,
							method,
							args,
							prepareCallProxy(
									(CallableStatement) context.callOnly( connection, method, args ),
									(Connection) proxy
							)
					);
				case "toString":
					return context.onCall( proxy, method, args, "Connection proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				case "setSavepoint":
					return savepointProxy( (Savepoint) context.call( proxy, connection, method, args ) );
				case "releaseSavepoint":
					if ( Proxy.isProxyClass( args[0].getClass() ) ) {
						args[0] = ( (SavepointHandler) Proxy.getInvocationHandler( args[0] ) ).savepoint;
					}
					return context.call( proxy, connection, method, args );
				case "rollback":
					if ( args != null && args.length != 0 && Proxy.isProxyClass( args[0].getClass() ) ) {
						args[0] = ( (SavepointHandler) Proxy.getInvocationHandler( args[0] ) ).savepoint;
					}
					return context.call( proxy, connection, method, args );
				default:
					return context.call( proxy, connection, method, args );
			}
		}

		private DatabaseMetaData getDatabaseMetaDataProxy(Connection connectionProxy) throws Throwable {
			if ( databaseMetaDataProxy == null ) {
				// we need to make it
				final DatabaseMetaDataHandler metadataHandler = new DatabaseMetaDataHandler( connection.getMetaData(), connectionProxy, context );
				databaseMetaDataProxy = (DatabaseMetaData) Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] {DatabaseMetaData.class},
						metadataHandler
				);
			}
			return databaseMetaDataProxy;
		}

		private Statement createStatementProxy(Statement statement, Connection connectionProxy) {
			return (Statement) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {Statement.class},
					new StatementHandler( statement, context, connectionProxy )
			);
		}

		private PreparedStatement prepareStatementProxy(PreparedStatement statement, Connection connectionProxy) {
			return (PreparedStatement) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {PreparedStatement.class},
					new PreparedStatementHandler( statement, context, connectionProxy )
			);
		}

		private CallableStatement prepareCallProxy(CallableStatement statement, Connection connectionProxy) {
			return (CallableStatement) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {CallableStatement.class},
					new CallableStatementHandler( statement, context, connectionProxy )
			);
		}

		private Savepoint savepointProxy(Savepoint savepoint) {
			return (Savepoint) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {Savepoint.class},
					new SavepointHandler( savepoint, context )
			);
		}
	}

	private static class StatementHandler implements InvocationHandler, Spy {
		protected final Statement statement;
		protected final SpyContext context;
		protected final Connection connectionProxy;

		public StatementHandler(Statement statement, SpyContext context, Connection connectionProxy) {
			this.statement = statement;
			this.context = context;
			this.connectionProxy = connectionProxy;
		}

		@Override
		public Object getSpiedInstance() {
			return statement;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "getConnection":
					return context.onCall( proxy, method, args, connectionProxy );
				case "toString":
					return context.onCall( proxy, method, args, "Statement proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				case "executeQuery":
					return context.onCall( proxy, method, args, getResultSetProxy( statement.executeQuery( (String) args[0] ), (Statement) proxy ) );
				case "getResultSet":
					return context.onCall( proxy, method, args, getResultSetProxy( statement.getResultSet(), (Statement) proxy ) );
				case "getGeneratedKeys":
					return context.onCall( proxy, method, args, getResultSetProxy( statement.getGeneratedKeys(), (Statement) proxy ) );
				default:
					return context.call( proxy, statement, method, args );
			}
		}

		protected ResultSet getResultSetProxy(ResultSet resultSet, Statement statementProxy) throws Throwable {
			return (ResultSet) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {ResultSet.class},
					new ResultSetHandler( resultSet, context, statementProxy )
			);
		}
	}

	private static class PreparedStatementHandler extends StatementHandler {
		private ResultSetMetaData resultSetMetaDataProxy;
		private ParameterMetaData parameterMetaDataProxy;

		public PreparedStatementHandler(PreparedStatement statement, SpyContext context, Connection connectionProxy) {
			super( statement, context, connectionProxy );
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "toString":
					return context.onCall( proxy, method, args, "PreparedStatement proxy [@" + hashCode() + "]" );
				case "executeQuery":
					return context.onCall(
							proxy,
							method,
							args,
							getResultSetProxy(
									(ResultSet) context.callOnly( statement, method, args ),
									(PreparedStatement) proxy
							)
					);
				case "getMetaData":
					return context.onCall(
							proxy,
							method,
							args,
							getResultSetMetaDataProxy( ( (PreparedStatement) statement ).getMetaData() )
					);
				case "getParameterMetaData":
					return context.onCall(
							proxy,
							method,
							args,
							getParameterMetaDataProxy( ( (PreparedStatement) statement ).getParameterMetaData() )
					);
				default:
					return super.invoke( proxy, method, args );
			}
		}

		private ResultSetMetaData getResultSetMetaDataProxy(ResultSetMetaData metaData) throws Throwable {
			if ( resultSetMetaDataProxy == null ) {
				// we need to make it
				resultSetMetaDataProxy = (ResultSetMetaData) Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] {ResultSetMetaData.class},
						new ResultSetMetaDataHandler( metaData, context )
				);
			}
			return resultSetMetaDataProxy;
		}

		private ParameterMetaData getParameterMetaDataProxy(ParameterMetaData metaData) throws Throwable {
			if ( parameterMetaDataProxy == null ) {
				// we need to make it
				parameterMetaDataProxy = (ParameterMetaData) Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] {ParameterMetaData.class},
						new ParameterMetaDataHandler( metaData, context )
				);
			}
			return parameterMetaDataProxy;
		}
	}

	private static class CallableStatementHandler extends PreparedStatementHandler {

		public CallableStatementHandler(CallableStatement statement, SpyContext context, Connection connectionProxy) {
			super( statement, context, connectionProxy );
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( "toString".equals( method.getName() ) ) {
				return context.onCall( proxy, method, args, "CallableStatement proxy [@" + hashCode() + "]" );
			}
			else {
				return super.invoke( proxy, method, args );
			}
		}
	}

	private static class DatabaseMetaDataHandler implements InvocationHandler, Spy {
		private final DatabaseMetaData databaseMetaData;
		private final Connection connectionProxy;
		private final SpyContext context;

		public DatabaseMetaDataHandler(
				DatabaseMetaData databaseMetaData,
				Connection connectionProxy,
				SpyContext context) {
			this.databaseMetaData = databaseMetaData;
			this.connectionProxy = connectionProxy;
			this.context = context;
		}

		@Override
		public Object getSpiedInstance() {
			return databaseMetaData;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "getConnection":
					return context.onCall( proxy, method, args, connectionProxy );
				case "toString":
					return context.onCall( proxy, method, args, "DatabaseMetaData proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				case "getProcedures":
				case "getProcedureColumns":
				case "getTables":
				case "getSchemas":
				case "getCatalogs":
				case "getTableTypes":
				case "getColumns":
				case "getColumnPrivileges":
				case "getTablePrivileges":
				case "getBestRowIdentifier":
				case "getVersionColumns":
				case "getPrimaryKeys":
				case "getImportedKeys":
				case "getExportedKeys":
				case "getCrossReference":
				case "getTypeInfo":
				case "getIndexInfo":
				case "getUDTs":
				case "getSuperTypes":
				case "getSuperTables":
				case "getAttributes":
				case "getClientInfoProperties":
				case "getFunctions":
				case "getFunctionColumns":
				case "getPseudoColumns":
					final ResultSet resultSet = (ResultSet) context.callOnly( databaseMetaData, method, args );
					return context.onCall( proxy, method, args, getResultSetProxy( resultSet, getStatementProxy( resultSet.getStatement() ) ) );
				default:
					return context.call( proxy, databaseMetaData, method, args );
			}
		}

		protected ResultSet getResultSetProxy(ResultSet resultSet, Statement statementProxy) throws Throwable {
			return (ResultSet) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {ResultSet.class},
					new ResultSetHandler( resultSet, context, statementProxy )
			);
		}

		protected Statement getStatementProxy(Statement statement) throws Throwable {
			final InvocationHandler handler;
			if ( statement instanceof CallableStatement ) {
				handler = new CallableStatementHandler( (CallableStatement) statement, context, connectionProxy );
			}
			else if ( statement instanceof PreparedStatement ) {
				handler = new PreparedStatementHandler( (PreparedStatement) statement, context, connectionProxy );
			}
			else {
				handler = new StatementHandler( statement, context, connectionProxy );
			}
			return (Statement) Proxy.newProxyInstance(
					ClassLoader.getSystemClassLoader(),
					new Class[] {Statement.class},
					handler
			);
		}
	}

	private static class ParameterMetaDataHandler implements InvocationHandler, Spy {
		private final ParameterMetaData parameterMetaData;
		private final SpyContext context;

		public ParameterMetaDataHandler(ParameterMetaData parameterMetaData, SpyContext context) {
			this.parameterMetaData = parameterMetaData;
			this.context = context;
		}

		@Override
		public Object getSpiedInstance() {
			return parameterMetaData;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "toString":
					return context.onCall( proxy, method, args, "DatabaseMetaData proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				default:
					return context.call( proxy, parameterMetaData, method, args );
			}
		}
	}

	private static class ResultSetHandler implements InvocationHandler, Spy {
		private final ResultSet resultSet;
		private final SpyContext context;
		private final Statement statementProxy;
		private ResultSetMetaData metadataProxy;

		public ResultSetHandler(ResultSet resultSet, SpyContext context, Statement statementProxy) {
			this.resultSet = resultSet;
			this.context = context;
			this.statementProxy = statementProxy;
		}

		@Override
		public Object getSpiedInstance() {
			return resultSet;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();
			switch ( methodName ) {
				case "getMetaData":
					return context.onCall( proxy, method, args, getResultSetMetaDataProxy( resultSet.getMetaData() ) );
				case "getStatement":
					return context.onCall( proxy, method, args, statementProxy );
				case "toString":
					return context.onCall( proxy, method, args, "ResultSet proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				default:
					return context.call( proxy, resultSet, method, args );
			}
		}

		private ResultSetMetaData getResultSetMetaDataProxy(ResultSetMetaData metaData) throws Throwable {
			if ( metadataProxy == null ) {
				// we need to make it
				final ResultSetMetaDataHandler metadataHandler = new ResultSetMetaDataHandler( metaData, context );
				metadataProxy = (ResultSetMetaData) Proxy.newProxyInstance(
						ClassLoader.getSystemClassLoader(),
						new Class[] {ResultSetMetaData.class},
						metadataHandler
				);
			}
			return metadataProxy;
		}
	}

	private static class ResultSetMetaDataHandler implements InvocationHandler, Spy {
		private final ResultSetMetaData resultSetMetaData;
		private final SpyContext context;

		public ResultSetMetaDataHandler(ResultSetMetaData resultSetMetaData, SpyContext context) {
			this.resultSetMetaData = resultSetMetaData;
			this.context = context;
		}

		@Override
		public Object getSpiedInstance() {
			return resultSetMetaData;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "toString":
					return context.onCall( proxy, method, args, "ResultSetMetaData proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				default:
					return context.call( proxy, resultSetMetaData, method, args );
			}
		}
	}

	private static class SavepointHandler implements InvocationHandler, Spy {
		private final Savepoint savepoint;
		private final SpyContext context;

		public SavepointHandler(Savepoint savepoint, SpyContext context) {
			this.savepoint = savepoint;
			this.context = context;
		}

		@Override
		public Object getSpiedInstance() {
			return savepoint;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch ( method.getName() ) {
				case "toString":
					return context.onCall( proxy, method, args, "Savepoint proxy [@" + hashCode() + "]" );
				case "hashCode":
					return context.onCall( proxy, method, args, hashCode() );
				case "equals":
					return context.onCall( proxy, method, args, proxy == args[0] );
				default:
					return context.call( proxy, savepoint, method, args );
			}
		}
	}

}
