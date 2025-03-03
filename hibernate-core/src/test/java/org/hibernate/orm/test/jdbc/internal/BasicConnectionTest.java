/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;

import org.hibernate.testing.SkipForDialect;
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
			String sql = "select count(*) from NON_EXISTENT";
			PreparedStatement ps = sessionImpl.getJdbcCoordinator().getStatementPreparer()
					.prepareStatement( sql );
			sessionImpl.getJdbcCoordinator().getResultSetReturn().execute( ps, sql );
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
	@SkipForDialect(value = DerbyDialect.class,comment = "Derby can't drop tables that are still referred to from open ResultSets")
	public void testBasicJdbcUsage() throws JDBCException {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		JdbcCoordinator jdbcCoord = sessionImpl.getJdbcCoordinator();

		try {
			Transaction ddlTxn = session.beginTransaction();
			Statement statement = jdbcCoord.getStatementPreparer().createStatement();
			String dropSql = sessionFactory().getJdbcServices().getDialect().getDropTableString( "SANDBOX_JDBC_TST" );
			try {
				jdbcCoord.getResultSetReturn().execute( statement, dropSql );
			}
			catch ( Exception e ) {
				// ignore if the DB doesn't support "if exists" and the table doesn't exist
			}
			jdbcCoord.getResultSetReturn().execute( statement,
					"create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( getResourceRegistry( jdbcCoord ).hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() );
			getResourceRegistry( jdbcCoord ).release( statement );
			assertFalse( getResourceRegistry( jdbcCoord ).hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() ); // after_transaction specified
			ddlTxn.commit();

			Transaction dmlTxn = session.beginTransaction();
			final String insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";
			PreparedStatement ps = jdbcCoord.getStatementPreparer().prepareStatement( insertSql );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoord.getResultSetReturn().execute( ps, insertSql );

			final String selectSql = "select * from SANDBOX_JDBC_TST";
			ps = jdbcCoord.getStatementPreparer().prepareStatement( selectSql );
			jdbcCoord.getResultSetReturn().extract( ps, selectSql );
			assertTrue( getResourceRegistry( jdbcCoord ).hasRegisteredResources() );
			dmlTxn.commit();
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			try {
				Transaction ddlTx = session.beginTransaction();
				session.doWork( connection -> {
					final Statement stmnt = connection.createStatement();

					stmnt.execute( sessionFactory().getJdbcServices().getDialect().getDropTableString( "SANDBOX_JDBC_TST" ) );
				} );
				ddlTx.commit();
			}
			finally {
				session.close();
			}
		}

		assertFalse( getResourceRegistry( jdbcCoord ).hasRegisteredResources() );
	}

	private ResourceRegistry getResourceRegistry(JdbcCoordinator jdbcCoord) {
		return jdbcCoord.getLogicalConnection().getResourceRegistry();
	}
}
