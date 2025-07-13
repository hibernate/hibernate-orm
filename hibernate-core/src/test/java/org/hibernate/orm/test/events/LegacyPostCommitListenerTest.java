/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to ensure that the existing post commit behavior when using plain PostXEventListeners fire on both success and failure.
 *
 * @author ShawnClowater
 */
public class LegacyPostCommitListenerTest extends BaseCoreFunctionalTestCase {
	private final PostInsertEventListener postCommitInsertEventListener = new LegacyPostCommitInsertEventListener();
	private final PostDeleteEventListener postCommitDeleteEventListener = new LegacyPostCommitDeleteEventListener();
	private final PostUpdateEventListener postCommitUpdateEventListener = new LegacyPostCommitUpdateEventListener();

	@Override
	protected void prepareTest() throws Exception {
		( (LegacyPostCommitInsertEventListener) postCommitInsertEventListener ).fired = 0;
		( (LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener ).fired = 0;
		( (LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener ).fired = 0;
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.applyIntegrator(
				new Integrator() {
					@Override
					public void integrate(
							Metadata metadata,
							BootstrapContext bootstrapContext,
							SessionFactoryImplementor sessionFactory) {
						integrate( sessionFactory );
					}

					private void integrate(SessionFactoryImplementor sessionFactory) {
						final EventListenerRegistry listenerRegistry = sessionFactory.getEventListenerRegistry();
						listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_DELETE )
								.appendListener( postCommitDeleteEventListener );
						listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_UPDATE )
								.appendListener( postCommitUpdateEventListener );
						listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_INSERT )
								.appendListener( postCommitInsertEventListener );
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitInsertListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		Assert.assertEquals( 1, ( (LegacyPostCommitInsertEventListener) postCommitInsertEventListener ).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitInsertListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.rollback();
		session.close();

		//the legacy implementation fires the listener on failure as well
		Assert.assertEquals( 1, ( (LegacyPostCommitInsertEventListener) postCommitInsertEventListener ).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitUpdateListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.commit();

		session = openSession();
		transaction = session.beginTransaction();
		irrelevantEntity.setName( "Irrelevant 2" );
		session.merge( irrelevantEntity );
		session.flush();
		transaction.commit();

		session.close();

		Assert.assertEquals( 1, ( (LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener ).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitUpdateListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		irrelevantEntity.setName( "Irrelevant 2" );
		session.merge( irrelevantEntity );
		session.flush();
		transaction.rollback();

		session.close();

		//the legacy implementation fires the listener on failure as well
		Assert.assertEquals( 1, ( (LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener ).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitDeleteListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		session.remove( irrelevantEntity );
		session.flush();
		transaction.commit();

		session.close();

		Assert.assertEquals( 1, ( (LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener ).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitDeleteListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.persist( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		session.remove( irrelevantEntity );
		session.flush();
		transaction.rollback();

		session.close();

		//the legacy implementation fires the listener on failure as well
		Assert.assertEquals( 1, ( (LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener ).fired );
	}

	private static class LegacyPostCommitDeleteEventListener implements PostDeleteEventListener {
		int fired;

		@Override
		public void onPostDelete(PostDeleteEvent event) {
			fired++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}

	private static class LegacyPostCommitUpdateEventListener implements PostUpdateEventListener {
		int fired;

		@Override
		public void onPostUpdate(PostUpdateEvent event) {
			fired++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}

	private static class LegacyPostCommitInsertEventListener implements PostInsertEventListener {
		int fired;

		@Override
		public void onPostInsert(PostInsertEvent event) {
			fired++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IrrelevantEntity.class };
	}
}
