/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.lang.reflect.Field;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Transaction;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.envers.internal.synchronization.AuditProcessManager;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.IntTestEntity;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * An envers specific quest that verifies the {@link AuditProcessManager} gets flushed.
 *
 * There is a similar to `org.hibernate.test.tm.JtaAfterCompletionTest` in hibernate-core
 * which verifies that the callbacks fires.
 *
 * The premise behind this test is to verify that when a JTA transaction is aborted by
 * Arjuna's reaper thread, the original thread will still invoke the after-completion
 * callbacks making sure that the Envers {@link AuditProcessManager} gets flushed to
 * avoid memory leaks.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12448")
public class JtaTransactionAfterCallbackTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IntTestEntity.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		TestingJtaBootstrap.prepare( options );
		options.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta" );
		options.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, Boolean.TRUE );
	}

	@Test
	@Priority(10)
	public void testAuditProcessManagerFlushedOnTransactionTimeout() throws Exception {
		// We will set the timeout to 5 seconds to allow the transaction reaper to kick in for us.
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().setTransactionTimeout( 5 );

		// Begin the transaction and do some extensive 10s long work
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		EntityManager entityManager = null;
		try {
			entityManager = getEntityManager();

			IntTestEntity ite = new IntTestEntity( 10 );
			entityManager.persist( ite );

			// Register before completion callback
			// The before causes this thread to wait until the Reaper thread aborts our transaction
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			session.getActionQueue().registerProcess( new BeforeCallbackCompletionHandler() );

			TestingJtaPlatformImpl.transactionManager().commit();
		}
		catch ( Exception e ) {
			// This is expected
			assertTyping( RollbackException.class, e );
		}
		finally {
			try {
				if ( entityManager != null ) {
					entityManager.close();
				}
			}
			catch ( PersistenceException e ) {
				// we expect this
				assertTrue( e.getMessage().contains( "Transaction was rolled back in a different thread" ) );
			}

			// test the audit process manager was flushed
			assertAuditProcessManagerEmpty();
		}
	}

	public static class BeforeCallbackCompletionHandler implements BeforeTransactionCompletionProcess {
		@Override
		public void doBeforeTransactionCompletion(SessionImplementor session) {
			try {
				// Wait for the transaction to be rolled back by the Reaper thread.
				final Transaction transaction = TestingJtaPlatformImpl.transactionManager().getTransaction();
				while ( transaction.getStatus() != Status.STATUS_ROLLEDBACK )
					Thread.sleep( 10 );
			}
			catch ( Exception e ) {
				// we aren't concerned with this.
			}
		}
	}

	private void assertAuditProcessManagerEmpty() throws Exception {
		final SessionFactoryImplementor sf = entityManagerFactory().unwrap( SessionFactoryImplementor.class );
		final EnversService enversService = sf.getServiceRegistry().getService( EnversService.class );
		final AuditProcessManager auditProcessManager = enversService.getAuditProcessManager();

		Map<Transaction, AuditProcess> values;

		Field field = auditProcessManager.getClass().getDeclaredField( "auditProcesses" );
		field.setAccessible( true );

		values = (Map<Transaction, AuditProcess>) field.get( auditProcessManager );

		// assert that the AuditProcess map is not null but empty (e.g. flushed).
		assertNotNull( values );
		assertEquals( 0, values.size() );
	}

}
