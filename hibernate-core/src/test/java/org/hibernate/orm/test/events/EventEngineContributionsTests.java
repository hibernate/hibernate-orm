/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.event.spi.EventEngineContributions;
import org.hibernate.event.spi.EventEngineContributor;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-13890")
public class EventEngineContributionsTests extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {
		super.configureBootstrapServiceRegistryBuilder( bsrb );
		bsrb.applyClassLoaderService( new TestingClassLoaderService() );
	}

	@Test
	public void testCustomEventAccess() {
		final EventEngine eventEngine = sessionFactory().getEventEngine();

		{
			final EventType<SexyRxySaveListener> saveEventType = eventEngine.findRegisteredEventType( SexyRxySaveListener.EVENT_NAME );
			assertThat( saveEventType, sameInstance( TheContributor.INSTANCE.saveEventType ) );
			assertThat( saveEventType.isStandardEvent(), is( false ) );

			final EventListenerRegistry listenerRegistry = eventEngine.getListenerRegistry();
			final EventListenerGroup<SexyRxySaveListener> listenerGroup = listenerRegistry.getEventListenerGroup( saveEventType );
			assertThat( listenerGroup.count(), is( 1 ) );

			listenerGroup.fireEventOnEachListener( RxySaveEvent.INSTANCE, SexyRxySaveListener::doIt );

			assertThat( SexyRxySaveListener.INSTANCE.didIt, is(true ) );
		}

		{
			final EventType<SexyRxyPersistListener> persistEventType = eventEngine.findRegisteredEventType( SexyRxyPersistListener.EVENT_NAME );
			assertThat( persistEventType, sameInstance( TheContributor.INSTANCE.persistEventType ) );
			assertThat( persistEventType.isStandardEvent(), is( false ) );

			final EventListenerRegistry listenerRegistry = eventEngine.getListenerRegistry();
			final EventListenerGroup<SexyRxyPersistListener> listenerGroup = listenerRegistry.getEventListenerGroup( persistEventType );
			assertThat( listenerGroup.count(), is( 1 ) );

			listenerGroup.fireEventOnEachListener( RxyPersistEvent.INSTANCE, SexyRxyPersistListener::doIt );

			assertThat( SexyRxyPersistListener.INSTANCE.didIt, is(true ) );
		}
	}

	public interface SexyRxyBaseListener {
	}

	public static class RxySaveEvent {
		public static final RxySaveEvent INSTANCE = new RxySaveEvent();
	}

	public static class SexyRxySaveListener implements SexyRxyBaseListener {
		public static final String EVENT_NAME = "rx-save";

		public static final SexyRxySaveListener INSTANCE = new SexyRxySaveListener();

		private boolean didIt;

		public void doIt(RxySaveEvent event) {
			didIt = true;
		}
	}

	public static class RxyPersistEvent {
		public static final RxyPersistEvent INSTANCE = new RxyPersistEvent();
	}

	public static class SexyRxyPersistListener implements SexyRxyBaseListener {
		public static final String EVENT_NAME = "rx-persist";

		public static final SexyRxyPersistListener INSTANCE = new SexyRxyPersistListener();

		private boolean didIt;

		public void doIt(RxyPersistEvent event) {
			didIt = true;
		}
	}

	public static class TheContributor implements EventEngineContributor {
		/**
		 * Singleton access
		 */
		public static final TheContributor INSTANCE = new TheContributor();

		private EventType<SexyRxySaveListener> saveEventType;
		private EventType<SexyRxyPersistListener> persistEventType;

		@Override
		public void contribute(EventEngineContributions target) {
			saveEventType = target.contributeEventType(
					SexyRxySaveListener.EVENT_NAME,
					SexyRxySaveListener.class,
					SexyRxySaveListener.INSTANCE
			);

			persistEventType = target.contributeEventType(
					SexyRxyPersistListener.EVENT_NAME,
					SexyRxyPersistListener.class
			);

			target.configureListeners(
					persistEventType,
					(group) -> group.appendListener( SexyRxyPersistListener.INSTANCE )
			);
		}
	}

	public static class TestingClassLoaderService extends ClassLoaderServiceImpl {
		@Override
		public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
			if ( serviceContract.equals( EventEngineContributor.class ) ) {
				//noinspection unchecked
				return (Collection<S>) Collections.singleton( TheContributor.INSTANCE );
			}

			return super.loadJavaServices( serviceContract );
		}
	}
}
