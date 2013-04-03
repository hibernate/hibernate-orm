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

import javax.transaction.TransactionManager;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.test.common.TransactionContextImpl;
import org.hibernate.test.common.TransactionEnvironmentImpl;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing transaction facade handling when the transaction is being driven by something other than the facade.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class ManagedDrivingTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	@SuppressWarnings( {"unchecked"})
	public void setUp() throws Exception {
		Map configValues = new HashMap();
		TestingJtaBootstrap.prepare( configValues );
		configValues.put( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );

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
		final TransactionContext transactionContext = new TransactionContextImpl( new TransactionEnvironmentImpl( serviceRegistry ) ) {
			@Override
			public ConnectionReleaseMode getConnectionReleaseMode() {
				return ConnectionReleaseMode.AFTER_STATEMENT;
			}
		};

		final TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl( null, transactionContext );
		final JournalingTransactionObserver transactionObserver = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( transactionObserver );

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

		JtaPlatform instance = serviceRegistry.getService( JtaPlatform.class );
		TransactionManager transactionManager = instance.retrieveTransactionManager();

		// start the cmt
		transactionManager.begin();

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, transactionObserver.getBegins() );
		assertFalse( txn.isInitiator() );
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

			// since txn is not a driver, nothing should have changed...
			assertTrue( jdbcCoordinator.hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );
			assertEquals( 0, transactionObserver.getBeforeCompletions() );
			assertEquals( 0, transactionObserver.getAfterCompletions() );

			transactionManager.commit();
			assertFalse( jdbcCoordinator.hasRegisteredResources() );
			assertFalse( logicalConnection.isPhysicallyConnected() );
			assertEquals( 1, transactionObserver.getBeforeCompletions() );
			assertEquals( 1, transactionObserver.getAfterCompletions() );
		}
		catch ( SQLException sqle ) {
			try {
				transactionManager.rollback();
			}
			catch (Exception ignore) {
			}
			fail( "incorrect exception type : SQLException" );
		}
		catch (Throwable reThrowable) {
			try {
				transactionManager.rollback();
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
