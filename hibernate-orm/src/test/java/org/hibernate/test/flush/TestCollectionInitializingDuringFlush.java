/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.flush;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-2763" )
public class TestCollectionInitializingDuringFlush extends BaseCoreFunctionalTestCase {
	@Test
	public void testInitializationDuringFlush() {
		assertFalse( InitializingPreUpdateEventListener.INSTANCE.executed );
		assertFalse( InitializingPreUpdateEventListener.INSTANCE.foundAny );

		Session s = openSession();
		s.beginTransaction();
		Publisher publisher = new Publisher( "acme" );
		Author author = new Author( "john" );
		author.setPublisher( publisher );
		publisher.getAuthors().add( author );
		author.getBooks().add( new Book( "Reflections on a Wimpy Kid", author ) );
		s.save( author );
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.beginTransaction();
		publisher = (Publisher) s.get( Publisher.class, publisher.getId() );
		publisher.setName( "random nally" );
		s.flush();
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.beginTransaction();
		s.delete( author );
		s.getTransaction().commit();
		s.clear();

		assertTrue( InitializingPreUpdateEventListener.INSTANCE.executed );
		assertTrue( InitializingPreUpdateEventListener.INSTANCE.foundAny );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Publisher.class };
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.applyIntegrator(
				new Integrator() {
					@Override
					public void integrate(
							Metadata metadata,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate( serviceRegistry );
					}

					private void integrate(SessionFactoryServiceRegistry serviceRegistry) {
						serviceRegistry.getService( EventListenerRegistry.class )
								.getEventListenerGroup( EventType.PRE_UPDATE )
								.appendListener( InitializingPreUpdateEventListener.INSTANCE );
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
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
					if ( ! Hibernate.isInitialized( oldValues[i] ) ) {
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
