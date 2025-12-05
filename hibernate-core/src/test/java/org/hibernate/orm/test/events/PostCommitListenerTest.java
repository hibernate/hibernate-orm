/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import org.hibernate.IrrelevantEntity;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
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
public class PostCommitListenerTest {
	private final PostInsertEventListener postCommitInsertEventListener = new TestPostCommitInsertEventListener();
	private final PostDeleteEventListener postCommitDeleteEventListener = new TestPostCommitDeleteEventListener();
	private final PostUpdateEventListener postCommitUpdateEventListener = new TestPostCommitUpdateEventListener();

	@BeforeEach
	void prepareTest() throws Exception {
		((TestPostCommitInsertEventListener) postCommitInsertEventListener).success = 0;
		((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed = 0;
		((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success = 0;
		((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed = 0;
		((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).success = 0;
		((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed = 0;
	}

	@BeforeAll
	void prepareBootstrapRegistryBuilder(SessionFactoryScope scope) {
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

		assertEquals( 1, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).success );
		assertEquals( 0, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed );
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

		assertEquals( 0, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).success );
		assertEquals( 1, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed );
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

		assertEquals( 1, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).success );
		assertEquals( 0, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitUpdateListenerRollback(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = scope.fromTransaction( session -> {
			IrrelevantEntity e = new IrrelevantEntity();
			e.setName( "Irrelevant" );

			session.persist( e );
			session.flush();
			return e;
		} );

		scope.inTransaction( session -> {
			irrelevantEntity.setName( "Irrelevant 2" );
			session.merge( irrelevantEntity );
			session.flush();
			session.getTransaction().setRollbackOnly();
		} );

		assertEquals( 0, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).success );
		assertEquals( 1, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed );
	}

	@Test
	@JiraKey("HHH-1582")
	public void testPostCommitDeleteListenerSuccess(SessionFactoryScope scope) {
		IrrelevantEntity irrelevantEntity = scope.fromTransaction( session -> {
			IrrelevantEntity e = new IrrelevantEntity();
			e.setName( "Irrelevant" );

			session.persist( e );
			session.flush();
			return e;
		} );

		scope.inTransaction( session -> {
			session.remove( irrelevantEntity );
			session.flush();
		} );

		assertEquals( 1, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success );
		assertEquals( 0, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed );
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

		assertEquals( 0, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success );
		assertEquals( 1, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed );
	}

	private static class TestPostCommitDeleteEventListener implements PostCommitDeleteEventListener {
		int success;
		int failed;

		@Override
		public void onPostDelete(PostDeleteEvent event) {
			success++;
		}

		@Override
		public void onPostDeleteCommitFailed(PostDeleteEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}

	private static class TestPostCommitUpdateEventListener implements PostCommitUpdateEventListener {
		int success;
		int failed;

		@Override
		public void onPostUpdate(PostUpdateEvent event) {
			success++;
		}

		@Override
		public void onPostUpdateCommitFailed(PostUpdateEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}

	private static class TestPostCommitInsertEventListener implements PostCommitInsertEventListener {
		int success;
		int failed;

		@Override
		public void onPostInsert(PostInsertEvent event) {
			success++;
		}

		@Override
		public void onPostInsertCommitFailed(PostInsertEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHandling(EntityPersister persister) {
			return true;
		}
	}
}
