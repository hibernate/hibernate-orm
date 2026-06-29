/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import jakarta.annotation.Nonnull;
import org.hibernate.IrrelevantEntity;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = IrrelevantEntity.class)
@SessionFactory
class PersistThenRemoveEventTest {

	private static final CountingPostInsertListener insertListener = new CountingPostInsertListener();
	private static final CountingPostDeleteListener deleteListener = new CountingPostDeleteListener();

	@BeforeAll
	void registerListeners(SessionFactoryScope scope) {
		final EventListenerRegistry registry = scope.getSessionFactory().getEventListenerRegistry();
		registry.getEventListenerGroup( EventType.POST_INSERT ).appendListener( insertListener );
		registry.getEventListenerGroup( EventType.POST_DELETE ).appendListener( deleteListener );
	}

	@BeforeEach
	void resetCounters() {
		insertListener.count = 0;
		deleteListener.count = 0;
	}

	@Test
	public void testPersistThenRemoveBeforeFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IrrelevantEntity entity = new IrrelevantEntity();
			entity.setName( "transient" );

			session.persist( entity );
			session.remove( entity );
		} );

		assertEquals( 0, insertListener.count,
				"PostInsertEvent should not fire when persist+remove happens before flush" );
		assertEquals( 0, deleteListener.count,
				"PostDeleteEvent should not fire when persist+remove happens before flush" );
	}

	@Test
	public void testPersistThenRemoveCleansUpPersistenceContext(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IrrelevantEntity entity = new IrrelevantEntity();
			entity.setName( "transient" );

			session.persist( entity );
			session.remove( entity );
			session.flush();

			assertFalse( session.contains( entity ),
					"Entity should not be contained in the session after persist+remove+flush" );

			final var persistenceContext = session.unwrap( org.hibernate.engine.spi.SessionImplementor.class )
					.getPersistenceContextInternal();
			assertNull( persistenceContext.getEntry( entity ),
					"Entity entry should be removed from persistence context after persist+remove" );
		} );
	}

	@Test
	public void testPersistThenRemoveWithExplicitFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			IrrelevantEntity entity = new IrrelevantEntity();
			entity.setName( "transient" );

			session.persist( entity );
			session.remove( entity );
			session.flush();
		} );

		assertEquals( 0, insertListener.count,
				"PostInsertEvent should not fire when persist+remove is flushed together" );
		assertEquals( 0, deleteListener.count,
				"PostDeleteEvent should not fire when persist+remove is flushed together" );
	}

	private static class CountingPostInsertListener implements PostInsertEventListener {
		int count;

		@Override
		public void onPostInsert(@Nonnull PostInsertEvent event) {
			count++;
		}

	}

	private static class CountingPostDeleteListener implements PostDeleteEventListener {
		int count;

		@Override
		public void onPostDelete(@Nonnull PostDeleteEvent event) {
			count++;
		}

	}
}
