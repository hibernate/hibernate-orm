/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;

import org.hibernate.TransactionException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.resource.common.SynchronizationCollectorImpl;
import org.hibernate.orm.test.resource.common.SynchronizationErrorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inSession;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicJdbcTransactionTests extends BaseUnitTestCase {

	private SessionFactoryImplementor generateSessionFactory() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				// should be the default, but lets be specific about which we want to test
				.applySetting( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jdbc" )
				.build();
		try {
			return (SessionFactoryImplementor) new MetadataSources( ssr ).buildMetadata().buildSessionFactory();
		}
		catch (Exception e) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}

	@Test
	public void basicUsageTest() {
		try ( final SessionFactoryImplementor sf = generateSessionFactory() ) {
			inSession(
					sf,
					session-> {
						final TransactionCoordinator coordinator = session.getTransactionCoordinator();

						final SynchronizationCollectorImpl sync = new SynchronizationCollectorImpl();
						coordinator.getLocalSynchronizations()
								.registerSynchronization( sync );

						coordinator.getTransactionDriverControl().begin();
						assertEquals( 0, sync.getBeforeCompletionCount() );
						assertEquals( 0, sync.getSuccessfulCompletionCount() );
						assertEquals( 0, sync.getFailedCompletionCount() );

						coordinator.getTransactionDriverControl().commit();
						assertEquals( 1, sync.getBeforeCompletionCount() );
						assertEquals( 1, sync.getSuccessfulCompletionCount() );
						assertEquals( 0, sync.getFailedCompletionCount() );
					}
			);

		}
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testMarkRollbackOnly() {
		try ( final SessionFactoryImplementor sf = generateSessionFactory() ) {
			inSession(
					sf,
					session-> {
						final TransactionCoordinator coordinator = session.getTransactionCoordinator();


						assertEquals( TransactionStatus.NOT_ACTIVE, coordinator.getTransactionDriverControl().getStatus() );

						session.getTransaction().begin();
						assertEquals( TransactionStatus.ACTIVE, coordinator.getTransactionDriverControl().getStatus() );

						session.getTransaction().markRollbackOnly();
						assertEquals( TransactionStatus.MARKED_ROLLBACK, coordinator.getTransactionDriverControl().getStatus() );

						try {
							session.getTransaction().commit();
						}
						catch (TransactionException expected) {
						}
						finally {
							assertThat(
									coordinator.getTransactionDriverControl().getStatus(),
									anyOf(
											is( TransactionStatus.NOT_ACTIVE ),
											is( TransactionStatus.ROLLED_BACK )
									)
							);
						}
					}
			);

		}
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSynchronizationFailure() {
		try ( final SessionFactoryImplementor sf = generateSessionFactory() ) {
			inSession(
					sf,
					session -> {
						final TransactionCoordinator coordinator = session.getTransactionCoordinator();

						assertEquals( TransactionStatus.NOT_ACTIVE, coordinator.getTransactionDriverControl().getStatus() );
						coordinator.getLocalSynchronizations().registerSynchronization( SynchronizationErrorImpl.forBefore() );

						coordinator.getTransactionDriverControl().begin();
						assertEquals( TransactionStatus.ACTIVE, coordinator.getTransactionDriverControl().getStatus() );

						try {
							coordinator.getTransactionDriverControl().commit();
						}
						catch (Exception expected) {
						}
						finally {
							assertThat(
									coordinator.getTransactionDriverControl().getStatus(),
									anyOf(
											is( TransactionStatus.NOT_ACTIVE ),
											is( TransactionStatus.ROLLED_BACK )
									)
							);
						}
					}
			);
		}
	}
}
