/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import org.hibernate.Session;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.internal.tools.MutableInteger;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RegisterUserEventListenersTest extends BaseEnversFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {StrTestEntity.class};
	}

	@Test
	@JiraKey(value = "HHH-7478")
	public void testTransactionProcessSynchronization() {
		final EventListenerRegistry registry = sessionFactory().getEventListenerRegistry();
		final CountingPostInsertTransactionBoundaryListener listener = new CountingPostInsertTransactionBoundaryListener();

		registry.getEventListenerGroup( EventType.POST_INSERT ).appendListener( listener );

		Session session = openSession();
		session.getTransaction().begin();
		StrTestEntity entity = new StrTestEntity( "str1" );
		session.persist( entity );
		session.getTransaction().commit();
		session.close();

		// Post insert listener invoked three times - before/after insertion of original data,
		// revision entity and audit row.
		Assert.assertEquals( 3, listener.getBeforeCount() );
		Assert.assertEquals( 3, listener.getAfterCount() );
	}

	private static class CountingPostInsertTransactionBoundaryListener implements PostInsertEventListener {
		private final MutableInteger beforeCounter = new MutableInteger();
		private final MutableInteger afterCounter = new MutableInteger();

		@Override
		public void onPostInsert(PostInsertEvent event) {
			event.getSession().getActionQueue().registerProcess(
					new BeforeTransactionCompletionProcess() {
						@Override
						public void doBeforeTransactionCompletion(SessionImplementor session) {
							beforeCounter.increase();
						}
					}
			);
			event.getSession().getActionQueue().registerProcess(
					new AfterTransactionCompletionProcess() {
						@Override
						public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
							afterCounter.increase();
						}
					}
			);
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}

		public int getBeforeCount() {
			return beforeCounter.get();
		}

		public int getAfterCount() {
			return afterCounter.get();
		}
	}
}
