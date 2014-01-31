/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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

package org.hibernate.test.connections;

import java.sql.Connection;
import java.sql.ResultSet;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * Implementation of SuppliedConnectionTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class SuppliedConnectionTest extends ConnectionManagementTestCase {
	private ConnectionProvider cp = ConnectionProviderBuilder.buildConnectionProvider();
	private Connection connectionUnderTest;

	@BeforeClassOnce
	private void prepareConnectionProvider() {
		cp = ConnectionProviderBuilder.buildConnectionProvider();
	}

	@AfterClassOnce
	private void releaseConnectionProvider() {
		try {
			if ( cp instanceof Stoppable ) {
					( ( Stoppable ) cp ).stop();
			}
			cp = null;
		}
		catch( Throwable ignore ) {
		}
	}

	@Override
	protected Session getSessionUnderTest() throws Throwable {
		connectionUnderTest = cp.getConnection();
		return sessionFactory().withOptions().connection( connectionUnderTest ).openSession();
	}

	@Override
	protected void reconnect(Session session) {
		session.reconnect( connectionUnderTest );
	}

	@Override
	protected void done() throws Throwable {
		cp.closeConnection( connectionUnderTest );
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.RELEASE_CONNECTIONS, ConnectionReleaseMode.ON_CLOSE.toString() );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, UserSuppliedConnectionProviderImpl.class.getName() );
		boolean supportsScroll = true;
		Connection conn = null;
		try {
			conn = cp.getConnection();
			supportsScroll = conn.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
		}
		catch( Throwable ignore ) {
		}
		finally {
			if ( conn != null ) {
				try {
					conn.close();
				}
				catch( Throwable ignore ) {
					// ignore it...
				}
			}
		}
		cfg.setProperty( Environment.USE_SCROLLABLE_RESULTSET, "" + supportsScroll );
	}

	@Override
	public boolean createSchema() {
		return false;
	}

	@Override
	public boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Override
	protected void prepareTest() throws Exception {
		super.prepareTest();
		Connection conn = cp.getConnection();
		try {
			new SchemaExport( metadata(), conn ).create( false, true );
		}
		finally {
			if ( conn != null ) {
				try {
					cp.closeConnection( conn );
				}
				catch( Throwable ignore ) {
				}
			}
		}
	}

	@Override
	protected void cleanupTest() throws Exception {
		Connection conn = cp.getConnection();
		try {
			new SchemaExport( metadata(), conn ).drop( false, true );
		}
		finally {
			if ( conn != null ) {
				try {
					cp.closeConnection( conn );
				}
				catch( Throwable ignore ) {
				}
			}
		}
		super.cleanupTest();
	}

}
