/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.internal.EventListenerRegistryImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.internal.CallbackRegistryImplementor;
import org.hibernate.jpa.event.internal.CallbacksFactory;
import org.hibernate.service.spi.Stoppable;

/**
 * Composite for the things related to Hibernate's event system.
 *
 * @author Steve Ebersole
 */
public class EventEngine {
	@SuppressWarnings("rawtypes")
	private final Map<String,EventType> registeredEventTypes;
	private final EventListenerRegistry listenerRegistry;

	private final CallbackRegistryImplementor callbackRegistry;

	public EventEngine(
			MetadataImplementor mappings,
			SessionFactoryImplementor sessionFactory) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// resolve (JPA) callback handlers

		this.callbackRegistry = CallbacksFactory.buildCallbackRegistry( sessionFactory.getSessionFactoryOptions(),
				sessionFactory.getServiceRegistry(), mappings.getEntityBindings() );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// resolve event types and listeners

		final EventListenerRegistryImpl.Builder listenerRegistryBuilder = new EventListenerRegistryImpl.Builder(
				callbackRegistry,
				sessionFactory.getSessionFactoryOptions().isJpaBootstrap()
		);

		final Map<String,EventType> eventTypes = new HashMap<>();
		EventType.registerStandardTypes( eventTypes );

		final EventEngineContributions contributionManager = new EventEngineContributions() {
			@Override
			public <T> EventType<T> findEventType(String name) {
				//noinspection unchecked
				return eventTypes.get( name );
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

				if ( listenerRole == null ) {
					throw new HibernateException( "Custom event-type listener role must be non-null." );
				}

				// make sure it does not match an existing name...
				if ( eventTypes.containsKey( name ) ) {
					final EventType<?> existing = eventTypes.get( name );
					throw new HibernateException(
							"Custom event-type already registered: " + name + " => " + existing
					);
				}

				final EventType<T> eventType = EventType.create(
						name,
						listenerRole,
						eventTypes.size()
				);

				eventTypes.put( name, eventType );
				return eventType;
			}

			@Override
			public <T> EventType<T> contributeEventType(String name, Class<T> listenerRole, T... defaultListeners) {
				final EventType<T> eventType = contributeEventType( name, listenerRole );

				if ( defaultListeners != null ) {
					final EventListenerGroup<T> listenerGroup = listenerRegistryBuilder.getListenerGroup( eventType );
					listenerGroup.appendListeners( defaultListeners );
				}

				return eventType;
			}

			@Override
			public <T> void configureListeners(
					EventType<T> eventType,
					Consumer<EventListenerGroup<T>> action) {
				if ( ! eventTypes.containsValue( eventType ) ) {
					throw new HibernateException( "EventType [" + eventType + "] not registered" );
				}

				action.accept( listenerRegistryBuilder.getListenerGroup( eventType ) );
			}
		};

		final Collection<EventEngineContributor> discoveredContributors = sessionFactory.getServiceRegistry()
				.getService( ClassLoaderService.class )
				.loadJavaServices( EventEngineContributor.class );
		if ( CollectionHelper.isNotEmpty( discoveredContributors ) ) {
			for ( EventEngineContributor contributor : discoveredContributors ) {
				contributor.contribute( contributionManager );
			}
		}

		this.registeredEventTypes = Collections.unmodifiableMap( eventTypes );
		this.listenerRegistry = listenerRegistryBuilder.buildRegistry( registeredEventTypes );
	}

	public Collection<EventType<?>> getRegisteredEventTypes() {
		//noinspection unchecked,rawtypes
		return (Collection) registeredEventTypes.values();
	}

	public <T> EventType<T> findRegisteredEventType(String name) {
		//noinspection unchecked
		return registeredEventTypes.get( name );
	}

	public EventListenerRegistry getListenerRegistry() {
		return listenerRegistry;
	}

	public CallbackRegistryImplementor getCallbackRegistry() {
		return callbackRegistry;
	}

	public void stop() {
		if ( listenerRegistry instanceof Stoppable ) {
			( (Stoppable) listenerRegistry ).stop();
		}

		callbackRegistry.release();
	}
}
