/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;

import org.hibernate.Interceptor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies lifecycle deferral for queue-neutral collection input during speculative auto-flush.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		CollectionAutoFlushLifecycleTest.Owner.class,
		CollectionAutoFlushLifecycleTest.Unrelated.class
})
@SessionFactory(interceptorClass = CollectionAutoFlushLifecycleTest.CountingInterceptor.class)
public class CollectionAutoFlushLifecycleTest {
	@Test
	void lifecycleWaitsForPositiveFlushDecision(SessionFactoryScope scope) {
		scope.getSessionFactory().getEventListenerRegistry()
				.getEventListenerGroup( EventType.PRE_COLLECTION_UPDATE )
				.appendListener( CountingPreUpdateListener.INSTANCE );
		final Long ownerId = scope.fromTransaction( session -> {
			final var owner = new Owner();
			final var unrelated = new Unrelated();
			session.persist( owner );
			session.persist( unrelated );
			return owner.id;
		} );

		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, ownerId );
			owner.values.size();
			CountingInterceptor.reset();
			CountingPreUpdateListener.COUNT.set( 0 );
			Owner.PRE_UPDATE_COUNT.set( 0 );

			owner.values.add( "changed" );
			session.createQuery( "from Unrelated", Unrelated.class ).getResultList();

			assertEquals( 0, CountingInterceptor.UPDATE_COUNT.get() );
			assertEquals( 0, CountingPreUpdateListener.COUNT.get() );
			assertEquals( 0, Owner.PRE_UPDATE_COUNT.get() );

			assertEquals(
					1L,
					session.createQuery(
							"select count(v) from Owner owner join owner.values v",
							Long.class
					).getSingleResult()
			);
			assertEquals( 1, CountingInterceptor.UPDATE_COUNT.get() );
			assertEquals( 1, CountingPreUpdateListener.COUNT.get() );
			assertEquals( 1, Owner.PRE_UPDATE_COUNT.get() );
		} );
	}

	public static class CountingInterceptor implements Interceptor {
		private static final AtomicInteger UPDATE_COUNT = new AtomicInteger();

		@Override
		public void onCollectionUpdate(Object collection, Object key) {
			UPDATE_COUNT.incrementAndGet();
		}

		private static void reset() {
			UPDATE_COUNT.set( 0 );
		}
	}

	private static class CountingPreUpdateListener implements PreCollectionUpdateEventListener {
		private static final CountingPreUpdateListener INSTANCE = new CountingPreUpdateListener();
		private static final AtomicInteger COUNT = new AtomicInteger();

		@Override
		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			COUNT.incrementAndGet();
		}
	}

	@Entity(name = "Owner")
	public static class Owner {
		private static final AtomicInteger PRE_UPDATE_COUNT = new AtomicInteger();

		@Id
		@GeneratedValue
		private Long id;

		@ElementCollection
		@CollectionTable(name = "owner_values")
		@Column(name = "value_text")
		private Set<String> values = new HashSet<>();

		@PreUpdate
		void preUpdate() {
			PRE_UPDATE_COUNT.incrementAndGet();
		}
	}

	@Entity(name = "Unrelated")
	public static class Unrelated {
		@Id
		@GeneratedValue
		private Long id;
	}
}
