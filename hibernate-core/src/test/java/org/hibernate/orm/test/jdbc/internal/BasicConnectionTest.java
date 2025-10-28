/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.internal;

import org.hibernate.JDBCException;
import org.hibernate.Transaction;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
public class BasicConnectionTest {
	@Test
	public void testExceptionHandling(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				String sql = "select count(*) from NON_EXISTENT";
				PreparedStatement ps = session.getJdbcCoordinator().getStatementPreparer()
						.prepareStatement( sql );
				session.getJdbcCoordinator().getResultSetReturn().execute( ps, sql );
				Assertions.fail( "The connection did not throw a JDBCException as expected" );
			}
			catch (JDBCException expected) {
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class,
			reason = "Derby can't drop tables that are still referred to from open ResultSets")
	public void testBasicJdbcUsage(SessionFactoryScope factoryScope) throws JDBCException {
		factoryScope.inSession( (session) -> {
			var jdbcCoordinator = session.getJdbcCoordinator();
			var dialect = session.getDialect();
			try {
				Transaction ddlTxn = session.beginTransaction();
				Statement statement = jdbcCoordinator.getStatementPreparer().createStatement();
				String dropSql = dialect.getDropTableString( "SANDBOX_JDBC_TST" );
				try {
					jdbcCoordinator.getResultSetReturn().execute( statement, dropSql );
				}
				catch ( Exception e ) {
					// ignore if the DB doesn't support "if exists" and the table doesn't exist
				}
				jdbcCoordinator.getResultSetReturn().execute( statement,
						"create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
				Assertions.assertTrue( getResourceRegistry( jdbcCoordinator ).hasRegisteredResources() );
				Assertions.assertTrue( jdbcCoordinator.getLogicalConnection().isPhysicallyConnected() );
				getResourceRegistry( jdbcCoordinator ).release( statement );
				Assertions.assertFalse( getResourceRegistry( jdbcCoordinator ).hasRegisteredResources() );
				Assertions.assertTrue( jdbcCoordinator.getLogicalConnection().isPhysicallyConnected() ); // after_transaction specified
				ddlTxn.commit();

				Transaction dmlTxn = session.beginTransaction();
				final String insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";
				PreparedStatement ps = jdbcCoordinator.getStatementPreparer().prepareStatement( insertSql );
				ps.setLong( 1, 1 );
				ps.setString( 2, "name" );
				jdbcCoordinator.getResultSetReturn().execute( ps, insertSql );

				final String selectSql = "select * from SANDBOX_JDBC_TST";
				ps = jdbcCoordinator.getStatementPreparer().prepareStatement( selectSql );
				jdbcCoordinator.getResultSetReturn().extract( ps, selectSql );
				Assertions.assertTrue( getResourceRegistry( jdbcCoordinator ).hasRegisteredResources() );
				dmlTxn.commit();
			}
			catch ( SQLException e ) {
				Assertions.fail( "incorrect exception type : sqlexception" );
			}
			finally {
				factoryScope.inTransaction( session, (s) -> session.doWork( connection -> {
					final Statement stmnt = connection.createStatement();

					stmnt.execute( dialect.getDropTableString( "SANDBOX_JDBC_TST" ) );
				} ) );
			}

			Assertions.assertFalse( getResourceRegistry( jdbcCoordinator ).hasRegisteredResources() );
		} );
	}

	private ResourceRegistry getResourceRegistry(JdbcCoordinator jdbcCoord) {
		return jdbcCoord.getLogicalConnection().getResourceRegistry();
	}
}
