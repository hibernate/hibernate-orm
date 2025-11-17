/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		annotatedClasses = {
				AutoFlushEventListenerTest.Entity1.class,
				AutoFlushEventListenerTest.Entity2.class
		}
)
@SessionFactory
@BootstrapServiceRegistry(
		integrators = AutoFlushEventListenerTest.CustomLoadIntegrator.class
)
public class AutoFlushEventListenerTest {

	private static final TheListener LISTENER = new TheListener();

	@Test
	public void testAutoFlushRequired(SessionFactoryScope scope) {
		LISTENER.events.clear();

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.persist( new Entity1() );
						assertThat( LISTENER.events.size() ).isEqualTo( 0 );

						// An entity of this type was persisted; a flush is required
						session.createQuery( "select i from Entity1 i", Entity1.class )
								.setHibernateFlushMode( FlushMode.AUTO )
								.getResultList();
						assertThat( LISTENER.events.size() ).isEqualTo( 1 );
						assertTrue( LISTENER.events.get( 0 ).isFlushRequired() );

						session.getTransaction().commit();
					}
					catch (Exception e) {
						session.getTransaction().rollback();
						throw e;
					}
					assertThat( LISTENER.events.size() ).isEqualTo( 1 );
				}
		);
		assertThat( LISTENER.events.size() ).isEqualTo( 1 );
	}

	@Test
	public void testAutoFlushNotRequired(SessionFactoryScope scope) {
		LISTENER.events.clear();

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.persist( new Entity2() );
						assertThat( LISTENER.events.size() ).isEqualTo( 0 );

						// No entity of this type was persisted; no flush is required
						session.createQuery( "select i from Entity1 i", Entity1.class )
								.setHibernateFlushMode( FlushMode.AUTO )
								.getResultList();
						assertThat( LISTENER.events.size() ).isEqualTo( 1 );
						assertFalse( LISTENER.events.get( 0 ).isFlushRequired() );

						session.getTransaction().commit();
					}
					catch (Exception e) {
						session.getTransaction().rollback();
						throw e;
					}
					assertThat( LISTENER.events.size() ).isEqualTo( 1 );
				}
		);

		assertThat( LISTENER.events.size() ).isEqualTo( 1 );
	}

	public static class CustomLoadIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			sessionFactory.getEventListenerRegistry().appendListeners(
					EventType.AUTO_FLUSH,
					LISTENER
			);
		}
	}

	@Entity(name = "Entity1")
	static class Entity1 {
		@Id
		@GeneratedValue
		private Integer id;

		public Entity1() {
		}
	}

	@Entity(name = "Entity2")
	static class Entity2 {
		@Id
		@GeneratedValue
		private Integer id;

		public Entity2() {
		}
	}

	private static class TheListener implements AutoFlushEventListener {
		final private List<AutoFlushEvent> events = new ArrayList<>();

		@Override
		public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
			events.add( event );
		}
	}
}
