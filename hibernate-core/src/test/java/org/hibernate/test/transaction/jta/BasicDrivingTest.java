/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.transaction.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.internal.jta.JtaTransactionFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.test.common.TransactionContextImpl;
import org.hibernate.test.common.TransactionEnvironmentImpl;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing transaction handling when the JTA transaction facade is the driver.
 *
 * @author Steve Ebersole
 */
public class BasicDrivingTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	@SuppressWarnings( {"unchecked"})
	public void setUp() throws Exception {
		Map configValues = new HashMap();
		configValues.putAll( ConnectionProviderBuilder.getConnectionProviderProperties() );
		configValues.put( Environment.TRANSACTION_STRATEGY, JtaTransactionFactory.class.getName() );
		TestingJtaBootstrap.prepare( configValues );
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( configValues )
				.build();
	}

	@After
	public void tearDown() throws Exception {
		serviceRegistry.destroy();
	}

	@Test
	public void testBasicUsage() throws Throwable {
		final TransactionContext transactionContext = new TransactionContextImpl( new TransactionEnvironmentImpl( serviceRegistry ) );

		TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl( null, transactionContext );
		JournalingTransactionObserver observer = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( observer );

		JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
		LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();

		// set up some tables to use
		Statement statement = jdbcCoordinator.getStatementPreparer().createStatement();
		jdbcCoordinator.getResultSetReturn().execute( statement, "drop table SANDBOX_JDBC_TST if exists" );
		jdbcCoordinator.getResultSetReturn().execute( statement, "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		assertTrue( jdbcCoordinator.hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		jdbcCoordinator.release( statement );
		assertFalse( jdbcCoordinator.hasRegisteredResources() );
		assertFalse( logicalConnection.isPhysicallyConnected() ); // after_statement specified

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, observer.getBegins() );
		assertTrue( txn.isInitiator() );
		try {
			PreparedStatement ps = jdbcCoordinator.getStatementPreparer().prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			jdbcCoordinator.getResultSetReturn().execute( ps );
			assertTrue( jdbcCoordinator.hasRegisteredResources() );
			jdbcCoordinator.release( ps );
			assertFalse( jdbcCoordinator.hasRegisteredResources() );

			ps = jdbcCoordinator.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			jdbcCoordinator.getResultSetReturn().extract( ps );
			ps = jdbcCoordinator.getStatementPreparer().prepareStatement( "delete from SANDBOX_JDBC_TST" );
			jdbcCoordinator.getResultSetReturn().execute( ps );
			// lets forget to close these...
			assertTrue( jdbcCoordinator.hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );

			// and commit the transaction...
			txn.commit();

			// we should now have:
			//		1) no resources because of after_transaction release mode
			assertFalse( jdbcCoordinator.hasRegisteredResources() );
			//		2) non-physically connected logical connection, again because of after_transaction release mode
			assertFalse( logicalConnection.isPhysicallyConnected() );
			//		3) transaction observer callbacks
			assertEquals( 1, observer.getBeforeCompletions() );
			assertEquals( 1, observer.getAfterCompletions() );
		}
		catch ( SQLException sqle ) {
			try {
				serviceRegistry.getService( JtaPlatform.class ).retrieveTransactionManager().rollback();
			}
			catch (Exception ignore) {
			}
			fail( "incorrect exception type : SQLException" );
		}
		catch (Throwable reThrowable) {
			try {
				serviceRegistry.getService( JtaPlatform.class ).retrieveTransactionManager().rollback();
			}
			catch (Exception ignore) {
			}
			throw reThrowable;
		}
		finally {
			logicalConnection.close();
			transactionContext.getTransactionEnvironment().getSessionFactory().close();
		}
	}

}
