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
package org.hibernate.test.jdbc.proxies;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.internal.NonBatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.test.common.JournalingBatchObserver;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.test.common.TransactionContextImpl;
import org.hibernate.test.common.TransactionEnvironmentImpl;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class BatchingTest extends BaseUnitTestCase implements BatchKey {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() throws Exception {
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.buildServiceRegistry();
	}

	@After
	public void tearDown() throws Exception {
		serviceRegistry.destroy();
	}

	@Override
	public int getBatchedStatementCount() {
		return 1;
	}

	@Override
	public Expectation getExpectation() {
		return Expectations.BASIC;
	}

	@Test
	public void testNonBatchingUsage() throws Exception {
		final TransactionContext transactionContext = new TransactionContextImpl(
				new TransactionEnvironmentImpl( serviceRegistry )
		);

		TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl( null, transactionContext );
		JournalingTransactionObserver observer = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( observer );

		final JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
		LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();
		Connection connection = logicalConnection.getShareableConnectionProxy();

		// set up some tables to use
		Statement statement = connection.createStatement();
		statement.execute( "drop table SANDBOX_JDBC_TST if exists" );
		statement.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		statement.close();
		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, observer.getBegins() );

		final String insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";

		final BatchBuilder batchBuilder = new BatchBuilderImpl( -1 );
		final BatchKey batchKey = new BasicBatchKey( "this", Expectations.BASIC );
		final Batch insertBatch = batchBuilder.buildBatch( batchKey, jdbcCoordinator );

		final JournalingBatchObserver batchObserver = new JournalingBatchObserver();
		insertBatch.addObserver( batchObserver );

		assertTrue( "unexpected Batch impl", NonBatchingBatch.class.isInstance( insertBatch ) );
		PreparedStatement insert = insertBatch.getBatchStatement( insertSql, false );
		insert.setLong( 1, 1 );
		insert.setString( 2, "name" );
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 0, batchObserver.getImplicitExecutionCount() );
		insertBatch.addToBatch();
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		logicalConnection.close();
	}

	@Test
	public void testBatchingUsage() throws Exception {
		final TransactionContext transactionContext = new TransactionContextImpl( new TransactionEnvironmentImpl( serviceRegistry ) );

		TransactionCoordinatorImpl transactionCoordinator = new TransactionCoordinatorImpl( null, transactionContext );
		JournalingTransactionObserver transactionObserver = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( transactionObserver );

		final JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
		LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();
		Connection connection = logicalConnection.getShareableConnectionProxy();

		// set up some tables to use
		Statement statement = connection.createStatement();
		statement.execute( "drop table SANDBOX_JDBC_TST if exists" );
		statement.execute( "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		statement.close();
		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, transactionObserver.getBegins() );

		final BatchBuilder batchBuilder = new BatchBuilderImpl( 2 );
		final BatchKey batchKey = new BasicBatchKey( "this", Expectations.BASIC );
		final Batch insertBatch = batchBuilder.buildBatch( batchKey, jdbcCoordinator );
		assertTrue( "unexpected Batch impl", BatchingBatch.class.isInstance( insertBatch ) );

		final JournalingBatchObserver batchObserver = new JournalingBatchObserver();
		insertBatch.addObserver( batchObserver );

		final String insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";

		PreparedStatement insert = insertBatch.getBatchStatement( insertSql, false );
		insert.setLong( 1, 1 );
		insert.setString( 2, "name" );
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 0, batchObserver.getImplicitExecutionCount() );
		insertBatch.addToBatch();
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 0, batchObserver.getImplicitExecutionCount() );
		assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );

		PreparedStatement insert2 = insertBatch.getBatchStatement( insertSql, false );
		assertSame( insert, insert2 );
		insert = insert2;
		insert.setLong( 1, 2 );
		insert.setString( 2, "another name" );
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 0, batchObserver.getImplicitExecutionCount() );
		insertBatch.addToBatch();
		assertEquals( 0, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertTrue( logicalConnection.getResourceRegistry().hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( logicalConnection.getResourceRegistry().hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		logicalConnection.close();
	}

}
