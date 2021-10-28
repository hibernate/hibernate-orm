/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.FlushModeType;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformNonStringValueSettingProvider;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13050")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@Jpa(
		annotatedClasses = {
				AbstractJtaBatchTest.Comment.class,
				AbstractJtaBatchTest.EventLog.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_COMPLIANCE, value = "true"),
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50")
		},
		nonStringValueSettingProviders = {
				JtaPlatformNonStringValueSettingProvider.class,
				AbstractJtaBatchTest.ConnectionNonStringValueSettingProvider.class,
				JtaWithFailingBatchTest.BatchBuilderNonStringValueSettingProvider.class
		}
)
public class JtaWithFailingBatchTest extends AbstractJtaBatchTest {

	private static TestBatch testBatch;

	@Test
	public void testAllStatementsAreClosedInCaseOfBatchExecutionFailure(EntityManagerFactoryScope scope) {
		TransactionManager transactionManager = TestingJtaPlatformImpl.INSTANCE.getTransactionManager();

		scope.inEntityManager(
				em -> {
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
						fail("An Exception is expected");
					}
					catch (Exception expected) {
						//expected
						try {
							switch ( transactionManager.getStatus() ) {
								case Status.STATUS_ACTIVE:
								case Status.STATUS_MARKED_ROLLBACK:
									transactionManager.rollback();
							}
						}
						catch (Exception e) {
							//ignore e
						}
					}

					assertThat(
							"AbstractBatchImpl#releaseStatements() has not been callled",
							testBatch.calledReleaseStatements,
							is( true )
					);
					assertAllStatementsAreClosed( testBatch.createdStatements );
					assertStatementsListIsCleared();

					assertFalse(
							triggerable.wasTriggered(),
							"HHH000352: Unable to release batch statement... has been thrown"
					);
				}
		);
	}

	private void assertStatementsListIsCleared() {
		assertThat( testBatch.createdStatements.size(), not( 0 ) );
		assertThat(
				"Not all PreparedStatements have been released",
				testBatch.numberOfStatementsAfterReleasing,
				is( 0 )
		);
	}

	public static class BatchBuilderNonStringValueSettingProvider extends NonStringValueSettingProvider {
		@Override
		public String getKey() {
			return BatchBuilderInitiator.BUILDER;
		}

		@Override
		public Object getValue() {
			return TestBatchBuilder.class.getName();
		}
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

	public static class TestBatchBuilder extends BatchBuilderImpl {

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return buildBatchTest( key, jdbcCoordinator, getJdbcBatchSize() );
		}

		protected BatchingBatch buildBatchTest(BatchKey key, JdbcCoordinator jdbcCoordinator, int jdbcBatchSize) {
			testBatch = new TestBatch( key, jdbcCoordinator, jdbcBatchSize );
			return testBatch;
		}
	}
}
