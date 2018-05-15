/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.transaction.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
 * @author Gail Badner
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13050")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class JtaWithFailingBatchTest extends AbstractJtaBatchTest {

	private static TestBatch testBatch;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Comment.class, EventLog.class };
	}

	@Test
	public void testAllStatementsAreClosedInCaseOfBatchExecutionFailure() throws Exception {
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

			try {
				em.persist( comment );
				transactionManager.commit();
			}
			catch (Exception expected) {
				//expected
				if ( transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
					transactionManager.rollback();
				}
			}

			assertThat(
					"AbstractBatchImpl#releaseStatements() has not been callled",
					testBatch.calledReleaseStatements,
					is( true )
			);
			assertAllStatementsAreClosed( testBatch.createdStatements );
			assertStatementsListIsCleared();
		}
		finally {

			em.close();
		}

		assertFalse( "HHH000352: Unable to release batch statement... has been thrown", triggerable.wasTriggered() );
	}

	private void assertStatementsListIsCleared() {
		assertThat( testBatch.createdStatements.size(), not( 0 ) );
		assertThat(
				"Not all PreparedStatements have been released",
				testBatch.numberOfStatementsAfterReleasing,
				is( 0 )
		);
	}

	public static class TestBatch extends BatchingBatch {
		private int numberOfStatementsAfterReleasing;
		private List<PreparedStatement> createdStatements = new ArrayList<>();
		private boolean calledReleaseStatements;

		private String currentStatementSql;

		public TestBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int batchSize) {
			super( key, jdbcCoordinator, batchSize );
		}

		@Override
		public PreparedStatement getBatchStatement(String sql, boolean callable) {
			currentStatementSql = sql;
			PreparedStatement batchStatement = super.getBatchStatement( sql, callable );
			createdStatements.add( batchStatement );
			return batchStatement;
		}

		@Override
		public void addToBatch() {
			// Implementations really should call abortBatch() before throwing an exception.
			// Purposely skipping the call to abortBatch() to ensure that Hibernate works properly when
			// a legacy implementation does not call abortBatch().
			throw sqlExceptionHelper().convert(
					new SQLException( "fake SQLException" ),
					"could not perform addBatch",
					currentStatementSql
			);
		}

		@Override
		protected void releaseStatements() {
			super.releaseStatements();
			calledReleaseStatements = true;
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
