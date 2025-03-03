/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.orm.test.jpa.transaction.JtaPlatformSettingProvider;

import org.hibernate.testing.orm.junit.JiraKey;
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
import org.assertj.core.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13050")
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
						provider = AbstractBatchingTest.Batch2BuilderSettingProvider.class
				)
		}
)
public class JtaWithStatementsBatchTest extends AbstractJtaBatchTest {

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

						Assertions.assertThat( wasReleaseCalled ).isTrue();
						Assertions.assertThat( numberOfStatementsBeforeRelease ).isEqualTo( 1 );
						Assertions.assertThat( numberOfStatementsAfterRelease ).isEqualTo( 0 );
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
}
