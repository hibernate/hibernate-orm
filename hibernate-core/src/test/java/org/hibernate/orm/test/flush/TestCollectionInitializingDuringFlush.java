/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.flush;

import org.hibernate.Hibernate;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-2763")
@DomainModel(
		annotatedClasses = {
				Author.class,
				Book.class,
				Publisher.class
		}
)
@SessionFactory
@BootstrapServiceRegistry(integrators = TestCollectionInitializingDuringFlush.CustomLoadIntegrator.class)
public class TestCollectionInitializingDuringFlush {

	@Test
	public void testInitializationDuringFlush(SessionFactoryScope scope) {
		assertFalse( InitializingPreUpdateEventListener.INSTANCE.executed );
		assertFalse( InitializingPreUpdateEventListener.INSTANCE.foundAny );

		final Publisher publisher = new Publisher( "acme" );
		Author author = new Author( "john" );
		scope.inTransaction(
				session -> {
					author.setPublisher( publisher );
					publisher.getAuthors().add( author );
					author.getBooks().add( new Book( "Reflections on a Wimpy Kid", author ) );
					session.persist( author );
				}
		);

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						Publisher p = session.get( Publisher.class, publisher.getId() );
						p.setName( "random nally" );
						session.flush();
						session.getTransaction().commit();
					}
					catch (Exception e) {
						session.getTransaction().rollback();
						throw e;
					}
					session.clear();

					scope.inSession(
							s -> {
								s.beginTransaction();
								try {
									s.remove( author );
									s.getTransaction().commit();
								}
								catch (Exception e) {
									session.getTransaction().rollback();
									throw e;
								}
								s.clear();

								assertTrue( InitializingPreUpdateEventListener.INSTANCE.executed );
								assertTrue( InitializingPreUpdateEventListener.INSTANCE.foundAny );
							}
					);

				}
		);
	}

	public static class CustomLoadIntegrator implements Integrator {
		@Override
		public void integrate(
				Metadata metadata,
				BootstrapContext bootstrapContext,
				SessionFactoryImplementor sessionFactory) {
			integrate( sessionFactory );
		}

		private void integrate(SessionFactoryImplementor sessionFactory) {
			sessionFactory.getEventListenerRegistry()
					.getEventListenerGroup( EventType.PRE_UPDATE )
					.appendListener( InitializingPreUpdateEventListener.INSTANCE );
		}
	}

	public static class InitializingPreUpdateEventListener implements PreUpdateEventListener {
		public static final InitializingPreUpdateEventListener INSTANCE = new InitializingPreUpdateEventListener();

		private boolean executed = false;
		private boolean foundAny = false;

		@Override
		public boolean onPreUpdate(PreUpdateEvent event) {
			executed = true;

			final Object[] oldValues = event.getOldState();
			final String[] properties = event.getPersister().getPropertyNames();

			// Iterate through all fields of the updated object
			for ( int i = 0; i < properties.length; i++ ) {
				if ( oldValues != null && oldValues[i] != null ) {
					if ( !Hibernate.isInitialized( oldValues[i] ) ) {
						// force any proxies and/or collections to initialize to illustrate HHH-2763
						foundAny = true;
						Hibernate.initialize( oldValues );
					}
				}
			}
			return true;
		}
	}
}
