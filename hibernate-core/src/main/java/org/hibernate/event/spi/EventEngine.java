/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static java.util.Collections.unmodifiableMap;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.hibernate.jpa.event.internal.CallbacksFactory.buildCallbackRegistry;

/**
 * Composite for the things related to Hibernate's event system.
 *
 * @author Steve Ebersole
 */
public class EventEngine {

	private final Map<String,EventType<?>> registeredEventTypes;
	private final EventListenerRegistry listenerRegistry;

	private final CallbackRegistry callbackRegistry;

	public EventEngine(MetadataImplementor mappings, SessionFactoryImplementor sessionFactory) {

		final SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
		final ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// resolve (JPA) callback handlers

		callbackRegistry = buildCallbackRegistry( sessionFactoryOptions, serviceRegistry, mappings.getEntityBindings() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// resolve event types and listeners

		final EventListenerRegistryImpl.Builder listenerRegistryBuilder =
				new EventListenerRegistryImpl.Builder( callbackRegistry, sessionFactoryOptions.isJpaBootstrap() );

		final Map<String,EventType<?>> eventTypes = new HashMap<>();
		EventType.registerStandardTypes( eventTypes );

		callContributors( serviceRegistry, new ContributionManager( eventTypes, listenerRegistryBuilder ) );

		registeredEventTypes = unmodifiableMap( eventTypes );
		listenerRegistry = listenerRegistryBuilder.buildRegistry( registeredEventTypes );
	}

	private static void callContributors(
			ServiceRegistryImplementor serviceRegistry, EventEngineContributions contributionManager) {
		final Collection<EventEngineContributor> discoveredContributors =
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( EventEngineContributor.class );
		if ( isNotEmpty( discoveredContributors ) ) {
			for ( EventEngineContributor contributor : discoveredContributors ) {
				contributor.contribute( contributionManager );
			}
		}
	}

	public Collection<EventType<?>> getRegisteredEventTypes() {
		return registeredEventTypes.values();
	}

	public <T> EventType<T> findRegisteredEventType(String name) {
		//noinspection unchecked
		return (EventType<T>) registeredEventTypes.get( name );
	}

	public EventListenerRegistry getListenerRegistry() {
		return listenerRegistry;
	}

	public CallbackRegistry getCallbackRegistry() {
		return callbackRegistry;
	}

	public void stop() {
		if ( listenerRegistry instanceof Stoppable stoppable ) {
			stoppable.stop();
		}
		callbackRegistry.release();
	}

	private static class ContributionManager implements EventEngineContributions {
		private final Map<String, EventType<?>> eventTypes;
		private final EventListenerRegistryImpl.Builder listenerRegistryBuilder;

		public ContributionManager(
				Map<String, EventType<?>> eventTypes,
				EventListenerRegistryImpl.Builder listenerRegistryBuilder) {
			this.eventTypes = eventTypes;
			this.listenerRegistryBuilder = listenerRegistryBuilder;
		}

		@Override
		public <T> EventType<T> findEventType(String name) {
			//noinspection unchecked
			return (EventType<T>) eventTypes.get( name );
		}

		@Override
		public <T> EventType<T> contributeEventType(String name, Class<T> listenerRole) {
			final EventType<T> eventType = registerEventType( name, listenerRole );
			listenerRegistryBuilder.prepareListeners( eventType );
			return eventType;
		}

		private <T> EventType<T> registerEventType(String name, Class<T> listenerRole) {
			if ( name == null ) {
				throw new HibernateException( "Custom event-type name must be non-null." );
			}
			else if ( listenerRole == null ) {
				throw new HibernateException( "Custom event-type listener role must be non-null." );
			}
			// make sure it does not match an existing name...
			else if ( eventTypes.containsKey( name ) ) {
				final EventType<?> existing = eventTypes.get( name );
				throw new HibernateException(
						"Custom event-type already registered: " + name + " => " + existing
				);
			}
			else {
				final EventType<T> eventType = EventType.create( name, listenerRole, eventTypes.size() );
				eventTypes.put( name, eventType );
				return eventType;
			}
		}

		@Override
		@SafeVarargs
		public final <T> EventType<T> contributeEventType(String name, Class<T> listenerRole, T... defaultListeners) {
			final EventType<T> eventType = contributeEventType( name, listenerRole );
			if ( defaultListeners != null ) {
				listenerRegistryBuilder.getListenerGroup( eventType ).appendListeners( defaultListeners );
			}
			return eventType;
		}

		@Override
		public <T> void configureListeners(EventType<T> eventType, Consumer<EventListenerGroup<T>> action) {
			if ( !eventTypes.containsValue( eventType ) ) {
				throw new HibernateException( "EventType [" + eventType + "] not registered" );
			}
			action.accept( listenerRegistryBuilder.getListenerGroup( eventType ) );
		}
	}
}
