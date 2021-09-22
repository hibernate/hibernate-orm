/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import java.sql.PreparedStatement;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
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
				JtaWithStatementsBatchTest.BatchBuilderNonStringValueSettingProvider.class
		}
)
public class JtaWithStatementsBatchTest extends AbstractJtaBatchTest {

	private static TestBatch testBatch;

	@Test
	public void testUnableToReleaseStatementMessageIsNotLogged(EntityManagerFactoryScope scope) {
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
						assertStatementsListIsCleared();
						assertAllStatementsAreClosed( testBatch.createdStatements );
					}
					catch (Exception | AssertionError e) {
						try {
							switch ( transactionManager.getStatus() ) {
								case Status.STATUS_ACTIVE:
								case Status.STATUS_MARKED_ROLLBACK:
									transactionManager.rollback();
							}
						}
						catch (Exception exception) {
							//ignore e
						}
						throw new RuntimeException( e );
					}

					assertFalse(
							triggerable.wasTriggered(),
							"HHH000352: Unable to release batch statement... has been thrown"
					);
				}
		);

		scope.inEntityManager(
				em -> {
					try {
						transactionManager.begin();
						Integer savedComments
								= em.createQuery( "from Comment" ).getResultList().size();
						assertThat( savedComments, is( 1 ) );

						Integer savedEventLogs
								= em.createQuery( "from EventLog" ).getResultList().size();
						assertThat( savedEventLogs, is( 2 ) );
					}
					catch (Exception e) {
						try {
							switch ( transactionManager.getStatus() ) {
								case Status.STATUS_ACTIVE:
								case Status.STATUS_MARKED_ROLLBACK:
									transactionManager.rollback();
							}
						}
						catch (Exception e2) {
							//ignore e
						}
					}
					finally {
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

		public TestBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int batchSize) {
			super( key, jdbcCoordinator, batchSize );
		}

		protected void releaseStatements() {
			createdStatements.addAll( getStatements().values() );
			super.releaseStatements();
			numberOfStatementsAfterReleasing += getStatements().size();
		}
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
