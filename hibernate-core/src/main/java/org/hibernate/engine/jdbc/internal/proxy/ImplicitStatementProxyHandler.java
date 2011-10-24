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

import org.hibernate.HibernateException;

/**
 * Invocation handler for {@link java.sql.Statement} proxies obtained from other JDBC object proxies
 *
 * @author Steve Ebersole
 */
public class ImplicitStatementProxyHandler extends AbstractStatementProxyHandler {
	protected ImplicitStatementProxyHandler(Statement statement, ConnectionProxyHandler connectionProxyHandler, Connection connectionProxy) {
		super( statement, connectionProxyHandler, connectionProxy );
	}

	protected void beginningInvocationHandling(Method method, Object[] args) {
		// disallow executions...
		if ( method.getName().startsWith( "execute" ) ) {
			throw new HibernateException( "execution not allowed on implicit statement object" );
		}
	}
}
