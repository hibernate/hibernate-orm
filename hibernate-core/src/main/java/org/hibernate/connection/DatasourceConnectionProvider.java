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
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.NamingHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection provider that uses a <tt>DataSource</tt> registered with JNDI.
 * Hibernate will use this <tt>ConnectionProvider</tt> by default if the
 * property <tt>hibernate.connection.datasource</tt> is set.
 * @see ConnectionProvider
 * @author Gavin King
 */
public class DatasourceConnectionProvider implements ConnectionProvider {
	private DataSource ds;
	private String user;
	private String pass;

	private static final Logger log = LoggerFactory.getLogger(DatasourceConnectionProvider.class);

	public DataSource getDataSource() {
		return ds;
	}

	public void setDataSource(DataSource ds) {
		this.ds = ds;
	}

	public void configure(Properties props) throws HibernateException {

		String jndiName = props.getProperty( Environment.DATASOURCE );
		if ( jndiName == null ) {
			String msg = "datasource JNDI name was not specified by property " + Environment.DATASOURCE;
			log.error( msg );
			throw new HibernateException( msg );
		}

		user = props.getProperty( Environment.USER );
		pass = props.getProperty( Environment.PASS );

		try {
			ds = ( DataSource ) NamingHelper.getInitialContext( props ).lookup( jndiName );
		}
		catch ( Exception e ) {
			log.error( "Could not find datasource: " + jndiName, e );
			throw new HibernateException( "Could not find datasource", e );
		}
		if ( ds == null ) {
			throw new HibernateException( "Could not find datasource: " + jndiName );
		}
		log.info( "Using datasource: " + jndiName );
	}

	public Connection getConnection() throws SQLException {
		if (user != null || pass != null) {
			return ds.getConnection(user, pass);
		}
		else {
			return ds.getConnection();
		}
	}

	public void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	public void close() {}

	/**
	 * @see ConnectionProvider#supportsAggressiveRelease()
	 */
	public boolean supportsAggressiveRelease() {
		return true;
	}

}







