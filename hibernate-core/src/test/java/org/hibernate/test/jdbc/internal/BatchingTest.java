/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.internal.NonBatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.transaction.TransactionCoordinator;

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
		
		final JdbcCoordinator jdbcCoordinator = sessionImpl.getJdbcCoordinator();
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
		assertTrue( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		jdbcCoordinator.getResourceRegistry().release( statement );
		assertFalse( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

		// ok, now we can get down to it...
		Transaction txn = session.getTransaction();  // same as Session#getTransaction
		txn.begin();

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
		assertFalse( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		session.close();
	}

	@Test
	public void testBatchingUsage() throws Exception {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		
		final JdbcCoordinator jdbcCoordinator = sessionImpl.getJdbcCoordinator();
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
		assertTrue( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() );
		jdbcCoordinator.getResourceRegistry().release( statement );
		assertFalse( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );
		assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified

		// ok, now we can get down to it...
		Transaction txn = session.getTransaction();  // same as Session#getTransaction
		txn.begin();

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
		assertTrue( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );

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
		assertTrue( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );

		insertBatch.execute();
		assertEquals( 1, batchObserver.getExplicitExecutionCount() );
		assertEquals( 1, batchObserver.getImplicitExecutionCount() );
		assertFalse( jdbcCoordinator.getResourceRegistry().hasRegisteredResources() );

		insertBatch.release();

		txn.commit();
		session.close();
	}

}
