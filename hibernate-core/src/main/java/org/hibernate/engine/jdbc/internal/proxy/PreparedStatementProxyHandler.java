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
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;

/**
 * Invocation handler for {@link java.sql.PreparedStatement} proxies
 *
 * @author Steve Ebersole
 */
public class PreparedStatementProxyHandler extends AbstractStatementProxyHandler {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, PreparedStatementProxyHandler.class.getName() );

	private final String sql;

	protected PreparedStatementProxyHandler(
			String sql,
			Statement statement,
			ConnectionProxyHandler connectionProxyHandler,
			Connection connectionProxy) {
		super( statement, connectionProxyHandler, connectionProxy );
		connectionProxyHandler.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		this.sql = sql;
	}

	@Override
	protected void beginningInvocationHandling(Method method, Object[] args) {
		if ( isExecution( method ) ) {
			logExecution();
		}
		else {
			journalPossibleParameterBind( method, args );
		}
	}

	private void journalPossibleParameterBind(Method method, Object[] args) {
		String methodName = method.getName();
		// todo : is this enough???
		if ( methodName.startsWith( "set" ) && args != null && args.length >= 2 ) {
			journalParameterBind( method, args );
		}
	}

	private void journalParameterBind(Method method, Object[] args) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Binding via {0}: {1}", method.getName(), Arrays.asList( args ) );
		}
	}

	private boolean isExecution(Method method) {
		return false;
	}

    private void logExecution() {
    }
}