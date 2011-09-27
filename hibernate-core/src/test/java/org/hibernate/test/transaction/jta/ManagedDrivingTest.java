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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.test.common.TransactionContextImpl;
import org.hibernate.test.common.TransactionEnvironmentImpl;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing transaction facade handling when the transaction is being driven by something other than the facade.
 *
 * @author Steve Ebersole
 */
public class ManagedDrivingTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	@SuppressWarnings( {"unchecked"})
	public void setUp() throws Exception {
		Map configValues = new HashMap();
		TestingJtaBootstrap.prepare( configValues );
		configValues.put( Environment.TRANSACTION_STRATEGY, CMTTransactionFactory.class.getName() );

		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder()
				.applySettings( configValues )
				.buildServiceRegistry();
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

		final LogicalConnectionImplementor logicalConnection = transactionCoordinator.getJdbcCoordinator().getLogicalConnection();
		Connection connection = logicalConnection.getShareableConnectionProxy();

		// set up some tables to use
		try {
			Statement statement = connection.createStatement();
			statement.execute( "drop table SANDBOX_JDBC_TST if exists" );
			statement.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );
			statement.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertFalse( logicalConnection.isPhysicallyConnected() ); // after_statement specified
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : SQLException" );
		}

		JtaPlatform instance = serviceRegistry.getService( JtaPlatform.class );
		TransactionManager transactionManager = instance.retrieveTransactionManager();

		// start the cmt
		transactionManager.begin();

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, transactionObserver.getBegins() );
		assertFalse( txn.isInitiator() );
		connection = logicalConnection.getShareableConnectionProxy();
		try {
			PreparedStatement ps = connection.prepareStatement( "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps.setLong( 1, 1 );
			ps.setString( 2, "name" );
			ps.execute();
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			ps.close();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );

			ps = connection.prepareStatement( "select * from SANDBOX_JDBC_TST" );
			ps.executeQuery();
			connection.prepareStatement( "delete from SANDBOX_JDBC_TST" ).execute();
			// lets forget to close these...
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );

			// and commit the transaction...
			txn.commit();

			// since txn is not a driver, nothing should have changed...
			assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			assertTrue( logicalConnection.isPhysicallyConnected() );
			assertEquals( 0, transactionObserver.getBeforeCompletions() );
			assertEquals( 0, transactionObserver.getAfterCompletions() );

			transactionManager.commit();
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
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
		}
	}
}
