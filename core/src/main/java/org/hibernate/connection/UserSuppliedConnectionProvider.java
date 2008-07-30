/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.connection;

import java.sql.Connection;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;

/**
 * An implementation of the <literal>ConnectionProvider</literal> interface that
 * simply throws an exception when a connection is requested. This implementation
 * indicates that the user is expected to supply a JDBC connection.
 * @see ConnectionProvider
 * @author Gavin King
 */
public class UserSuppliedConnectionProvider implements ConnectionProvider {

	/**
	 * @see org.hibernate.connection.ConnectionProvider#configure(Properties)
	 */
	public void configure(Properties props) throws HibernateException {
		LoggerFactory.getLogger( UserSuppliedConnectionProvider.class )
				.warn( "No connection properties specified - the user must supply JDBC connections" );
	}

	/**
	 * @see org.hibernate.connection.ConnectionProvider#getConnection()
	 */
	public Connection getConnection() {
		throw new UnsupportedOperationException("The user must supply a JDBC connection");
	}

	/**
	 * @see org.hibernate.connection.ConnectionProvider#closeConnection(Connection)
	 */
	public void closeConnection(Connection conn) {
		throw new UnsupportedOperationException("The user must supply a JDBC connection");
	}

	public void close() {
	}

	/**
	 * @see ConnectionProvider#supportsAggressiveRelease()
	 */
	public boolean supportsAggressiveRelease() {
		return false;
	}

}






