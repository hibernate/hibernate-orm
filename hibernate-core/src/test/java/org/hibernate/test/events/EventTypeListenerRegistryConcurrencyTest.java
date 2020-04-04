/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Concurrency test that registering a custom EventType does not interfere with
 * looking up a registered EventListenerGroup.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13890")
public class EventTypeListenerRegistryConcurrencyTest {
	private static CoreMessageLogger LOG = CoreLogging.messageLogger( EventTypeListenerRegistryConcurrencyTest.class );

	@Test
	public void test() {
		final TheConcurrencyIntegrator integrator = new TheConcurrencyIntegrator();
		BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.applyIntegrator( integrator )
				.build();
		SessionFactoryImplementor sessionFactory = null;
		try {
			final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr ).build();
			 sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
					.buildMetadata()
					.getSessionFactoryBuilder()
					.build();
			integrator.checkResults( sessionFactory.getServiceRegistry() );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
			bsr.close();
		}
	}

	private static class TheConcurrencyIntegrator implements Integrator {
		private final int NUMBER_OF_EVENT_TYPES_NEW = 10000;
		private final int NUMBER_OF_THREADS = 10;
		private final AtomicInteger START_VALUE = new AtomicInteger( 0 );
		private final List<Exception> exceptions = new ArrayList<>();
		private final Set<EventType> customEventTypes = new HashSet<>( NUMBER_OF_EVENT_TYPES_NEW );

		// Capture number of "standard" event types (before adding custom event types).
		private final int numberEventTypesBefore = EventType.values().size();

		@Override
		public void integrate(
				Metadata metadata,
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {

			final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

			final Runnable createAndRegisterEventTypes = () -> {
				try {
					for ( int i = START_VALUE.getAndIncrement();
							i < NUMBER_OF_EVENT_TYPES_NEW;
							i += NUMBER_OF_THREADS ) {
						final EventType eventType = EventType.addCustomEventType(
								"event" + i,
								DummyListener.class
						);
						eventListenerRegistry.setListeners( eventType, new DummyListener() );
						try {
							eventListenerRegistry.getEventListenerGroup( eventType );
						}
						catch (Exception ex) {
							exceptions.add( ex );
						}
					}
				}
				catch (Exception ex) {
					LOG.info( ex );
					exceptions.add( ex );
				}
			};

			final Runnable eventListenerGroupsGetter = () -> {
				while( true ) {
					try {
						assertNotNull( eventListenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ) );
					}
					catch (Exception ex) {
						exceptions.add( ex );
					}
				}
			};

			final Thread[] threadsCreateAndRegisterEventTypes = new Thread[NUMBER_OF_THREADS];
			final Thread[] threadsEventListenerGroupsGetter = new Thread[NUMBER_OF_THREADS];
			for ( int i = 0 ; i < NUMBER_OF_THREADS; i++ ) {
				threadsCreateAndRegisterEventTypes[i] = new Thread( createAndRegisterEventTypes );
				threadsEventListenerGroupsGetter[i] = new Thread( eventListenerGroupsGetter );
			}

			for ( int i = 0 ; i < NUMBER_OF_THREADS; i++ ) {
				threadsCreateAndRegisterEventTypes[i].start();
				threadsEventListenerGroupsGetter[i].start();
			}

			try {
				for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
					threadsCreateAndRegisterEventTypes[i].join();
					threadsEventListenerGroupsGetter[i].interrupt();
				}
			}
			catch (InterruptedException ex) {
				LOG.info( ex );
				exceptions.add( ex );
			}
		}

		@Override
		public void disintegrate(
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {
		}

		public void checkResults(ServiceRegistry serviceRegistry) {
			LOG.info( exceptions );
			assertTrue( exceptions.isEmpty() );
			assertEquals( numberEventTypesBefore + NUMBER_OF_EVENT_TYPES_NEW, EventType.values().size() );
			final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
			for ( EventType eventType : customEventTypes) {
				final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
				final Iterator iterator = eventListenerGroup.listeners().iterator();
				assertTrue( iterator.hasNext() );
				assertTrue( DummyListener.class.isInstance( iterator.next() ) );
				assertFalse( iterator.hasNext() );
			}
		}
	}

	private static class DummyListener {
	}
}
