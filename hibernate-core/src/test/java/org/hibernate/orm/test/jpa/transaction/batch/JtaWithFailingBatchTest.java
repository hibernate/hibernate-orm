/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.FlushModeType;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
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
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, value = "true"),
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50")
		},
		settingProviders = {
				@SettingProvider(
						settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = AbstractJtaBatchTest.ConnectionSettingProvider.class
				),
				@SettingProvider(
						settingName = AvailableSettings.JTA_PLATFORM,
						provider = JtaPlatformSettingProvider.class
				),
				@SettingProvider(
						settingName = BatchSettings.BUILDER,
						provider = AbstractBatchingTest.ErrorBatch2BuilderSettingProvider.class
				)
		}
)
public class JtaWithFailingBatchTest extends AbstractJtaBatchTest {

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

					assertThat( wasReleaseCalled ).isTrue();
					assertThat( numberOfStatementsBeforeRelease ).isEqualTo( 1 );
					assertThat( numberOfStatementsAfterRelease ).isEqualTo( 0 );

					assertThat( triggerable.wasTriggered() )
							.describedAs( "HHH000352: Unable to release batch statement... has been thrown" )
							.isFalse();
				}
		);
	}
}
