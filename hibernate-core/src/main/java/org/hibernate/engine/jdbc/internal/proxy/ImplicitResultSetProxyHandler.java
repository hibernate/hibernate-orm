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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.JdbcResourceRegistry;
import org.hibernate.engine.jdbc.spi.JdbcServices;

/**
 * Invocation handler for {@link java.sql.ResultSet} proxies obtained from other JDBC object proxies
 *
 * @author Steve Ebersole
 */
public class ImplicitResultSetProxyHandler extends AbstractResultSetProxyHandler {
	private ConnectionProxyHandler connectionProxyHandler;
	private Connection connectionProxy;
	private Statement sourceStatement;

	public ImplicitResultSetProxyHandler(ResultSet resultSet, ConnectionProxyHandler connectionProxyHandler, Connection connectionProxy) {
		super( resultSet );
		this.connectionProxyHandler = connectionProxyHandler;
		this.connectionProxy = connectionProxy;
	}

	public ImplicitResultSetProxyHandler(ResultSet resultSet, ConnectionProxyHandler connectionProxyHandler, Connection connectionProxy, Statement sourceStatement) {
		super( resultSet );
		this.connectionProxyHandler = connectionProxyHandler;
		this.connectionProxy = connectionProxy;
		this.sourceStatement = sourceStatement;
	}

	@Override
	protected JdbcServices getJdbcServices() {
		return connectionProxyHandler.getJdbcServices();
	}

	@Override
	protected JdbcResourceRegistry getResourceRegistry() {
		return connectionProxyHandler.getResourceRegistry();
	}

	@Override
	protected Statement getExposableStatement() {
		if ( sourceStatement == null ) {
			try {
				Statement stmnt = getResultSet().getStatement();
				if ( stmnt == null ) {
					return null;
				}
				sourceStatement = ProxyBuilder.buildImplicitStatement( stmnt, connectionProxyHandler, connectionProxy );
			}
			catch ( SQLException e ) {
				throw getJdbcServices().getSqlExceptionHelper().convert( e, e.getMessage() );
			}
		}
		return sourceStatement;
	}

	protected void invalidateHandle() {
		sourceStatement = null;
		super.invalidateHandle();
	}
}
