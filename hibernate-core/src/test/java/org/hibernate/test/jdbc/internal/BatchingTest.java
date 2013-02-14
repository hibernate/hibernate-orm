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
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.hibernate.Session;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.internal.NonBatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.test.common.JournalingBatchObserver;
import org.hibernate.test.common.JournalingTransactionObserver;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class BatchingTest extends BaseCoreFunctionalTestCase implements BatchKey {
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
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		TransactionCoordinator transactionCoordinator = sessionImpl.getTransactionCoordinator();
		JournalingTransactionObserver observer = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( observer );

		final JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
		LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();

		// set up some tables to use
		Statement statement = jdbcCoordinator.getStatementPreparer().createStatement();
		String dropSql = getDialect().getDropTableString( "SANDBOX_JDBC_TST" );
		try {
			jdbcCoordinator.getResultSetReturn().execute( statement, dropSql );
		}
		catch ( Exception e ) {
			// ignore if the DB doesn't support "if exists" and the table doesn't exist
		}
		jdbcCoordinator.getResultSetReturn().execute( statement, "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		assertTrue( jdbcCoordinator.hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		jdbcCoordinator.release( statement );
		assertFalse( jdbcCoordinator.hasRegisteredResources() );
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
		assertFalse( jdbcCoordinator.hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( jdbcCoordinator.hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		session.close();
	}

	@Test
	public void testBatchingUsage() throws Exception {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		TransactionCoordinator transactionCoordinator = sessionImpl.getTransactionCoordinator();
		JournalingTransactionObserver observer = new JournalingTransactionObserver();
		transactionCoordinator.addObserver( observer );

		final JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
		LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();

		// set up some tables to use
		Statement statement = jdbcCoordinator.getStatementPreparer().createStatement();
		String dropSql = getDialect().getDropTableString( "SANDBOX_JDBC_TST" );
		try {
			jdbcCoordinator.getResultSetReturn().execute( statement, dropSql );
		}
		catch ( Exception e ) {
			// ignore if the DB doesn't support "if exists" and the table doesn't exist
		}		jdbcCoordinator.getResultSetReturn().execute( statement, "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
		assertTrue( jdbcCoordinator.hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		jdbcCoordinator.release( statement );
		assertFalse( jdbcCoordinator.hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

		// ok, now we can get down to it...
		TransactionImplementor txn = transactionCoordinator.getTransaction();  // same as Session#getTransaction
		txn.begin();
		assertEquals( 1, observer.getBegins() );

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
		assertTrue( jdbcCoordinator.hasRegisteredResources() );

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
		assertTrue( jdbcCoordinator.hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( jdbcCoordinator.hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		session.close();
	}

}
