/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import org.hibernate.IrrelevantEntity;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test to ensure that the existing post commit behavior when using plain PostXEventListeners fire on both success and failure.
 *
 * @author ShawnClowater
 */
@DomainModel(
		annotatedClasses = {
				IrrelevantEntity.class
		}
)
@SessionFactory
public class LegacyPostCommitListenerTest {
	private final PostInsertEventListener postCommitInsertEventListener = new LegacyPostCommitInsertEventListener();
	private final PostDeleteEventListener postCommitDeleteEventListener = new LegacyPostCommitDeleteEventListener();
	private final PostUpdateEventListener postCommitUpdateEventListener = new LegacyPostCommitUpdateEventListener();

	@BeforeEach
	void prepareTest() {
		((LegacyPostCommitInsertEventListener) postCommitInsertEventListener).fired = 0;
		((LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener).fired = 0;
		((LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener).fired = 0;
	}

	@BeforeAll
	protected void prepareBootstrapRegistryBuilder(SessionFactoryScope scope) {
		final EventListenerRegistry listenerRegistry = scope.getSessionFactory().getEventListenerRegistry();
		listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_DELETE )
				.appendListener( postCommitDeleteEventListener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_UPDATE )
				.appendListener( postCommitUpdateEventListener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COMMIT_INSERT )
				.appendListener( postCommitInsertEventListener );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitInsertListenerSuccess(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
		} );

		assertEquals( 1, ((LegacyPostCommitInsertEventListener) postCommitInsertEventListener).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitInsertListenerRollback(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
			session.getTransaction().setRollbackOnly();
		} );

		//the legacy implementation fires the listener on failure as well
		assertEquals( 1, ((LegacyPostCommitInsertEventListener) postCommitInsertEventListener).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitUpdateListenerSuccess(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
		} );

		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant 2" );
			session.merge( irrelevantEntity );
			session.flush();
		} );

		assertEquals( 1, ((LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitUpdateListenerRollback(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
		} );

		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant 2" );
			session.merge( irrelevantEntity );
			session.flush();
			session.getTransaction().setRollbackOnly();
		} );

		//the legacy implementation fires the listener on failure as well
		assertEquals( 1, ((LegacyPostCommitUpdateEventListener) postCommitUpdateEventListener).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitDeleteListenerSuccess(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
		} );

		scope.inTransaction( session -> {
			session.remove( irrelevantEntity );
			session.flush();
		} );

		assertEquals( 1, ((LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener).fired );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitDeleteListenerRollback(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant" );

			session.persist( irrelevantEntity );
			session.flush();
		} );

		scope.inTransaction( session -> {
			session.remove( irrelevantEntity );
			session.flush();
			session.getTransaction().setRollbackOnly();
		} );

		//the legacy implementation fires the listener on failure as well
		assertEquals( 1, ((LegacyPostCommitDeleteEventListener) postCommitDeleteEventListener).fired );
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
}
