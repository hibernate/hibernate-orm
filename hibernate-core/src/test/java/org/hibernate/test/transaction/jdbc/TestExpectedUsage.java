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
package org.hibernate.test.transaction.jdbc;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistry;
import org.hibernate.service.spi.StandardServiceInitiators;
import org.hibernate.test.common.ConnectionProviderBuilder;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.test.common.TransactionContextImpl;
import org.hibernate.test.common.TransactionEnvironmentImpl;
import org.hibernate.testing.junit.UnitTestCase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class TestExpectedUsage extends UnitTestCase {
	private ServiceRegistry serviceRegistry;

	public TestExpectedUsage(String string) {
		super( string );
	}

	public void setUp() throws Exception {
		super.setUp();
		serviceRegistry = new ServiceRegistryImpl(
				StandardServiceInitiators.LIST,
				ConnectionProviderBuilder.getConnectionProviderProperties()
		);
	}

	public void tearDown() throws Exception {
		( (ServiceRegistryImpl) serviceRegistry).destroy();
		super.tearDown();
	}

	public void testBasicUsage() {
		final TransactionContext transactionContext = new TransactionContextImpl( new TransactionEnvironmentImpl( serviceRegistry ) ) {
			@Override
			public ConnectionReleaseMode getConnectionReleaseMode() {
				return ConnectionReleaseMode.AFTER_TRANSACTION;
			}
		};

		TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl( null, transactionContext );
		JournalingTransactionObserver observer = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( observer );

		LogicalConnectionImplementor logicalConnection = transactionCoordinator.getJdbcCoordinator().getLogicalConnection();
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
			assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : SQLException" );
		}

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, observer.getBegins() );
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

			// and commit the transaction...
			txn.commit();

			// we should now have:
			//		1) no resources because of after_transaction release mode
			assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
			//		2) non-physically connected logical connection, again because of after_transaction release mode
			assertFalse( logicalConnection.isPhysicallyConnected() );
			//		3) transaction observer callbacks
			assertEquals( 1, observer.getBeforeCompletions() );
			assertEquals( 1, observer.getAfterCompletions() );
		}
		catch ( SQLException sqle ) {
			fail( "incorrect exception type : SQLException" );
		}
		finally {
			logicalConnection.close();
		}
	}

}
