/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.transaction.batch;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13050")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class JtaWithStatementsBatchTest extends AbstractJtaBatchTest {

	private static TestBatch testBatch;

	@Test
	public void testUnableToReleaseStatementMessageIsNotLogged()
			throws Exception {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
		EntityManager em = createEntityManager();
		try {
			transactionManager.begin();

			em.setFlushMode( FlushModeType.AUTO );

			// Persist entity with non-generated id
			EventLog eventLog1 = new EventLog();
			eventLog1.setMessage( "Foo1" );
			em.persist( eventLog1 );

			// Persist entity with non-generated id
			EventLog eventLog2 = new EventLog();
			eventLog2.setMessage( "Foo2" );
			em.persist( eventLog2 );

			Comment comment = new Comment();
			comment.setMessage( "Bar" );
			em.persist( comment );

			transactionManager.commit();
			assertStatementsListIsCleared();
			assertAllStatementsAreClosed( testBatch.createtdStatements );
		}
		finally {
			if ( transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
				transactionManager.rollback();
			}
			em.close();
		}

		assertFalse( "HHH000352: Unable to release batch statement... has been thrown", triggerable.wasTriggered() );

		em = createEntityManager();

		try {
			transactionManager.begin();
			Integer savedComments
					= em.createQuery( "from Comment" ).getResultList().size();
			assertThat( savedComments, is( 1 ) );

			Integer savedEventLogs
					= em.createQuery( "from EventLog" ).getResultList().size();
			assertThat( savedEventLogs, is( 2 ) );
		}
		finally {
			if ( transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
				transactionManager.rollback();
			}
			em.close();
		}
	}

	private void assertStatementsListIsCleared() {
		assertThat( testBatch.createtdStatements.size(), not( 0 ) );
		assertThat(
				"Not all PreparedStatements have been released",
				testBatch.numberOfStatementsAfterReleasing,
				is( 0 )
		);
	}

	public static class TestBatch extends BatchingBatch {
		private int numberOfStatementsAfterReleasing;
		private List<PreparedStatement> createtdStatements = new ArrayList<>();

		public TestBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int batchSize) {
			super( key, jdbcCoordinator, batchSize );
		}

		protected void releaseStatements() {
			createtdStatements.addAll( getStatements().values() );
			super.releaseStatements();
			numberOfStatementsAfterReleasing += getStatements().size();
		}
	}

	@Override
	protected String getBatchBuilderClassName() {
		return TestBatchBuilder.class.getName();
	}

	public static class TestBatchBuilder extends BatchBuilderImpl {
		private int jdbcBatchSize;

		@Override
		public void setJdbcBatchSize(int jdbcBatchSize) {
			this.jdbcBatchSize = jdbcBatchSize;
		}

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return buildBatchTest( key, jdbcCoordinator, jdbcBatchSize );
		}

		protected BatchingBatch buildBatchTest(BatchKey key, JdbcCoordinator jdbcCoordinator, int jdbcBatchSize) {
			testBatch = new TestBatch( key, jdbcCoordinator, jdbcBatchSize );
			return testBatch;
		}
	}
}
