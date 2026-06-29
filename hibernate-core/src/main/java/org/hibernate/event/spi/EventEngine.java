/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.Stoppable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.unmodifiableMap;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import jakarta.annotation.Nonnull;

/**
 * Composite for the things related to Hibernate's event system.
 *
 * @author Steve Ebersole
 */
public class EventEngine {

	private final Map<String,EventType<?>> registeredEventTypes;
	private final EventListenerRegistry listenerRegistry;

	@Deprecated(since = "8.0", forRemoval = true)
	public EventEngine(@Nonnull MetadataImplementor mappings, @Nonnull SessionFactoryImplementor sessionFactory) {
		this( sessionFactory.getSessionFactoryOptions(), sessionFactory.getServiceRegistry() );
	}

	public EventEngine(@Nonnull SessionFactoryOptions options, @Nonnull ServiceRegistry registry) {
		final var listenerRegistryBuilder =
				new EventListenerRegistryImpl.Builder( options.isJpaBootstrap() );
		final Map<String,EventType<?>> eventTypes = new HashMap<>();
		EventType.registerStandardTypes( eventTypes );
		callContributors( registry, new ContributionManager( eventTypes, listenerRegistryBuilder ) );
		registeredEventTypes = unmodifiableMap( eventTypes );
		listenerRegistry = listenerRegistryBuilder.buildRegistry( registeredEventTypes );
	}

	private static void callContributors(
			@Nonnull ServiceRegistry serviceRegistry,
			@Nonnull EventEngineContributions contributionManager) {
		final var discoveredContributors =
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( EventEngineContributor.class );
		if ( isNotEmpty( discoveredContributors ) ) {
			for ( EventEngineContributor contributor : discoveredContributors ) {
				contributor.contribute( contributionManager );
			}
		}
	}

	@Nonnull
	public Collection<EventType<?>> getRegisteredEventTypes() {
		return registeredEventTypes.values();
	}

	@Nonnull
	public <T> EventType<T> findRegisteredEventType(@Nonnull String name) {
		//noinspection unchecked
		return (EventType<T>) registeredEventTypes.get( name );
	}

	@Nonnull
	public EventListenerRegistry getListenerRegistry() {
		return listenerRegistry;
	}

	public void stop() {
		if ( listenerRegistry instanceof Stoppable stoppable ) {
			stoppable.stop();
		}
	}

	private record ContributionManager(
			Map<String, EventType<?>> eventTypes,
			EventListenerRegistryImpl.Builder listenerRegistryBuilder)
			implements EventEngineContributions {

		@Override
		@Nonnull
		public <T> EventType<T> findEventType(@Nonnull String name) {
			//noinspection unchecked
			return (EventType<T>) eventTypes.get( name );
		}

		@Override
		@Nonnull
		public <T> EventType<T> contributeEventType(@Nonnull String name, @Nonnull Class<T> listenerRole) {
			final var eventType = registerEventType( name, listenerRole );
			listenerRegistryBuilder.prepareListeners( eventType );
			return eventType;
		}

		@Nonnull
		private <T> EventType<T> registerEventType(@Nonnull String name, @Nonnull Class<T> listenerRole) {
			if ( name == null ) {
				throw new HibernateException( "Custom event-type name must be non-null." );
			}
			else if ( listenerRole == null ) {
				throw new HibernateException( "Custom event-type listener role must be non-null." );
			}
			// make sure it does not match an existing name...
			else if ( eventTypes.containsKey( name ) ) {
				final var existing = eventTypes.get( name );
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
		@Nonnull
		@SafeVarargs
		public final <T> EventType<T> contributeEventType(@Nonnull String name, @Nonnull Class<T> listenerRole, @Nonnull T... defaultListeners) {
			final var eventType = contributeEventType( name, listenerRole );
			if ( defaultListeners != null ) {
				listenerRegistryBuilder.getListenerGroup( eventType ).appendListeners( defaultListeners );
			}
			return eventType;
		}

		@Override
		public <T> void configureListeners(@Nonnull EventType<T> eventType, @Nonnull Consumer<EventListenerGroup<T>> action) {
			if ( !eventTypes.containsValue( eventType ) ) {
				throw new HibernateException( "EventType [" + eventType + "] not registered" );
			}
			action.accept( listenerRegistryBuilder.getListenerGroup( eventType ) );
		}
	}
}
