/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.internal.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.InvalidatableWrapper;
import org.hibernate.engine.jdbc.spi.JdbcWrapper;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.internal.util.ValueHolder;

/**
 * Centralized builder for proxy instances
 *
 * @author Steve Ebersole
 */
public class ProxyBuilder {

	// Connection ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] CONNECTION_PROXY_INTERFACES = new Class[] {
			Connection.class,
			JdbcWrapper.class
	};

	private static final ValueHolder<Constructor<Connection>> connectionProxyConstructorValue = new ValueHolder<Constructor<Connection>>(
			new ValueHolder.DeferredInitializer<Constructor<Connection>>() {
				@Override
				public Constructor<Connection> initialize() {
					try {
						return locateConnectionProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Connection proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<Connection> locateConnectionProxyClass() {
					return (Class<Connection>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							CONNECTION_PROXY_INTERFACES
					);
				}
			}
	);

	public static Connection buildConnection(LogicalConnectionImplementor logicalConnection) {
		final ConnectionProxyHandler proxyHandler = new ConnectionProxyHandler( logicalConnection );
		try {
			return connectionProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC Connection proxy", e );
		}
	}


	// Statement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] STMNT_PROXY_INTERFACES = new Class[] {
			Statement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	private static final ValueHolder<Constructor<Statement>> statementProxyConstructorValue = new ValueHolder<Constructor<Statement>>(
			new ValueHolder.DeferredInitializer<Constructor<Statement>>() {
				@Override
				public Constructor<Statement> initialize() {
					try {
						return locateStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Statement proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<Statement> locateStatementProxyClass() {
					return (Class<Statement>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							STMNT_PROXY_INTERFACES
					);
				}
			}
	);

	public static Statement buildStatement(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		final BasicStatementProxyHandler proxyHandler = new BasicStatementProxyHandler(
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return statementProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC Statement proxy", e );
		}
	}

	public static Statement buildImplicitStatement(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		if ( statement == null ) {
			return null;
		}
		final ImplicitStatementProxyHandler proxyHandler = new ImplicitStatementProxyHandler( statement, connectionProxyHandler, connectionProxy );
		try {
			return statementProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC Statement proxy", e );
		}
	}


	// PreparedStatement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] PREPARED_STMNT_PROXY_INTERFACES = new Class[] {
			PreparedStatement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	private static final ValueHolder<Constructor<PreparedStatement>> preparedStatementProxyConstructorValue = new ValueHolder<Constructor<PreparedStatement>>(
			new ValueHolder.DeferredInitializer<Constructor<PreparedStatement>>() {
				@Override
				public Constructor<PreparedStatement> initialize() {
					try {
						return locatePreparedStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Statement proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<PreparedStatement> locatePreparedStatementProxyClass() {
					return (Class<PreparedStatement>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							PREPARED_STMNT_PROXY_INTERFACES
					);
				}
			}
	);

	public static PreparedStatement buildPreparedStatement(
			String sql,
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		final PreparedStatementProxyHandler proxyHandler = new PreparedStatementProxyHandler(
				sql,
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return preparedStatementProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC PreparedStatement proxy", e );
		}
	}


	// CallableStatement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] CALLABLE_STMNT_PROXY_INTERFACES = new Class[] {
			CallableStatement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	private static final ValueHolder<Constructor<CallableStatement>> callableStatementProxyConstructorValue = new ValueHolder<Constructor<CallableStatement>>(
			new ValueHolder.DeferredInitializer<Constructor<CallableStatement>>() {
				@Override
				public Constructor<CallableStatement> initialize() {
					try {
						return locateCallableStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated Statement proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<CallableStatement> locateCallableStatementProxyClass() {
					return (Class<CallableStatement>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							CALLABLE_STMNT_PROXY_INTERFACES
					);
				}
			}
	);

	public static CallableStatement buildCallableStatement(
			String sql,
			CallableStatement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		final CallableStatementProxyHandler proxyHandler = new CallableStatementProxyHandler(
				sql,
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return callableStatementProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC CallableStatement proxy", e );
		}
	}


	// ResultSet ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] RESULTSET_PROXY_INTERFACES = new Class[] {
			ResultSet.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	private static final ValueHolder<Constructor<ResultSet>> resultSetProxyConstructorValue = new ValueHolder<Constructor<ResultSet>>(
			new ValueHolder.DeferredInitializer<Constructor<ResultSet>>() {
				@Override
				public Constructor<ResultSet> initialize() {
					try {
						return locateCallableStatementProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated ResultSet proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<ResultSet> locateCallableStatementProxyClass() {
					return (Class<ResultSet>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							RESULTSET_PROXY_INTERFACES
					);
				}
			}
	);

	public static ResultSet buildResultSet(
			ResultSet resultSet,
			AbstractStatementProxyHandler statementProxyHandler,
			Statement statementProxy) {
		final ResultSetProxyHandler proxyHandler = new ResultSetProxyHandler( resultSet, statementProxyHandler, statementProxy );
		try {
			return resultSetProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC ResultSet proxy", e );
		}
	}

	public static ResultSet buildImplicitResultSet(
			ResultSet resultSet,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		final ImplicitResultSetProxyHandler proxyHandler = new ImplicitResultSetProxyHandler(
				resultSet,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return resultSetProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC ResultSet proxy", e );
		}
	}

	public static ResultSet buildImplicitResultSet(
			ResultSet resultSet,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy,
			Statement sourceStatement) {
		final ImplicitResultSetProxyHandler proxyHandler = new ImplicitResultSetProxyHandler(
				resultSet,
				connectionProxyHandler,
				connectionProxy,
				sourceStatement
		);
		try {
			return resultSetProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC ResultSet proxy", e );
		}
	}


	// DatabaseMetaData ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] METADATA_PROXY_INTERFACES = new Class[] {
			DatabaseMetaData.class,
			JdbcWrapper.class
	};

	private static final ValueHolder<Constructor<DatabaseMetaData>> metadataProxyConstructorValue = new ValueHolder<Constructor<DatabaseMetaData>>(
			new ValueHolder.DeferredInitializer<Constructor<DatabaseMetaData>>() {
				@Override
				public Constructor<DatabaseMetaData> initialize() {
					try {
						return locateDatabaseMetaDataProxyClass().getConstructor( InvocationHandler.class );
					}
					catch (NoSuchMethodException e) {
						throw new JdbcProxyException( "Could not find proxy constructor in JDK generated DatabaseMetaData proxy class", e );
					}
				}

				@SuppressWarnings("unchecked")
				private Class<DatabaseMetaData> locateDatabaseMetaDataProxyClass() {
					return (Class<DatabaseMetaData>) Proxy.getProxyClass(
							JdbcWrapper.class.getClassLoader(),
							METADATA_PROXY_INTERFACES
					);
				}
			}
	);

	public static DatabaseMetaData buildDatabaseMetaData(
			DatabaseMetaData metaData,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		final DatabaseMetaDataProxyHandler proxyHandler = new DatabaseMetaDataProxyHandler(
				metaData,
				connectionProxyHandler,
				connectionProxy
		);
		try {
			return metadataProxyConstructorValue.getValue().newInstance( proxyHandler );
		}
		catch (Exception e) {
			throw new JdbcProxyException( "Could not instantiate JDBC DatabaseMetaData proxy", e );
		}
	}
}
