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
package org.hibernate.engine.jdbc.proxy;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hibernate.service.jdbc.spi.JdbcWrapper;
import org.hibernate.service.jdbc.spi.InvalidatableWrapper;
import org.hibernate.service.jdbc.spi.LogicalConnectionImplementor;

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

	public static Connection buildConnection(LogicalConnectionImplementor logicalConnection) {
		ConnectionProxyHandler proxyHandler = new ConnectionProxyHandler( logicalConnection );
		return ( Connection ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				CONNECTION_PROXY_INTERFACES,
				proxyHandler
		);
	}


	// Statement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] STMNT_PROXY_INTERFACES = new Class[] {
			Statement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	public static Statement buildStatement(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		BasicStatementProxyHandler proxyHandler = new BasicStatementProxyHandler(
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		return ( Statement ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				STMNT_PROXY_INTERFACES,
				proxyHandler
		);
	}

	public static Statement buildImplicitStatement(
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		if ( statement == null ) {
			return null;
		}
		ImplicitStatementProxyHandler handler = new ImplicitStatementProxyHandler( statement, connectionProxyHandler, connectionProxy );
		return ( Statement ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				STMNT_PROXY_INTERFACES,
				handler
		);
	}


	// PreparedStatement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] PREPARED_STMNT_PROXY_INTERFACES = new Class[] {
			PreparedStatement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	public static PreparedStatement buildPreparedStatement(
			String sql,
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		PreparedStatementProxyHandler proxyHandler = new PreparedStatementProxyHandler(
				sql,
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		return ( PreparedStatement ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				PREPARED_STMNT_PROXY_INTERFACES,
				proxyHandler
		);
	}


	// CallableStatement ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] CALLABLE_STMNT_PROXY_INTERFACES = new Class[] {
			CallableStatement.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};

	public static CallableStatement buildCallableStatement(
			String sql,
			CallableStatement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		PreparedStatementProxyHandler proxyHandler = new PreparedStatementProxyHandler(
				sql,
				statement,
				connectionProxyHandler,
				connectionProxy
		);
		return ( CallableStatement ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				CALLABLE_STMNT_PROXY_INTERFACES,
				proxyHandler
		);
	}


	// ResultSet ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] RESULTSET_PROXY_INTERFACES = new Class[] {
			ResultSet.class,
			JdbcWrapper.class,
			InvalidatableWrapper.class
	};


	public static ResultSet buildResultSet(
			ResultSet resultSet,
			AbstractStatementProxyHandler statementProxyHandler,
			Statement statementProxy) {
		ResultSetProxyHandler proxyHandler = new ResultSetProxyHandler( resultSet, statementProxyHandler, statementProxy );
		return ( ResultSet ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				RESULTSET_PROXY_INTERFACES,
				proxyHandler
		);
	}

	public static ResultSet buildImplicitResultSet(
			ResultSet resultSet,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		ImplicitResultSetProxyHandler proxyHandler = new ImplicitResultSetProxyHandler( resultSet, connectionProxyHandler, connectionProxy );
		return ( ResultSet ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				RESULTSET_PROXY_INTERFACES,
				proxyHandler
		);
	}


	// DatabaseMetaData ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static final Class[] METADATA_PROXY_INTERFACES = new Class[] {
			DatabaseMetaData.class,
			JdbcWrapper.class
	};

	public static DatabaseMetaData buildDatabaseMetaData(
			DatabaseMetaData metaData,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		DatabaseMetaDataProxyHandler handler = new DatabaseMetaDataProxyHandler( metaData, connectionProxyHandler, connectionProxy );
		return ( DatabaseMetaData ) Proxy.newProxyInstance(
				JdbcWrapper.class.getClassLoader(),
				METADATA_PROXY_INTERFACES,
				handler
		);
	}
}
