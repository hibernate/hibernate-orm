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
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class BasicConnectionTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testExceptionHandling() {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		boolean caught = false;
		try {
			PreparedStatement ps = sessionImpl.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer()
					.prepareStatement( "select count(*) from NON_EXISTENT" );
			sessionImpl.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().execute( ps );
		}
		catch ( JDBCException ok ) {
			caught = true;
		}
		finally {
			session.close();
		}

		assertTrue( "The connection did not throw a JDBCException as expected", caught );
	}

	@Test
	public void testBasicJdbcUsage() throws JDBCException {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		JdbcCoordinator jdbcCoord = sessionImpl.getTransactionCoordinator().getJdbcCoordinator();

		try {
			Statement statement = jdbcCoord.getStatementPreparer().createStatement();
			String dropSql = getDialect().getDropTableString( "SANDBOX_JDBC_TST" );
			try {
				jdbcCoord.getResultSetReturn().execute( statement, dropSql );
			}
			catch ( Exception e ) {
				// ignore if the DB doesn't support "if exists" and the table doesn't exist
			}
			jdbcCoord.getResultSetReturn().execute( statement,
					"create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( jdbcCoord.hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() );
			jdbcCoord.release( statement );
			assertFalse( jdbcCoord.hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() ); // after_transaction specified

			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement(
					"insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoord.getResultSetReturn().execute( ps );

			ps = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			jdbcCoord.getResultSetReturn().extract( ps );

			assertTrue( jdbcCoord.hasRegisteredResources() );
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			session.close();
		}

		assertFalse( jdbcCoord.hasRegisteredResources() );
	}
}
