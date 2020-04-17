/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.internal.DefaultDeleteEventListener;
import org.hibernate.event.internal.DefaultDirtyCheckEventListener;
import org.hibernate.event.internal.DefaultEvictEventListener;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.internal.DefaultInitializeCollectionEventListener;
import org.hibernate.event.internal.DefaultLoadEventListener;
import org.hibernate.event.internal.DefaultLockEventListener;
import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.internal.DefaultPersistOnFlushEventListener;
import org.hibernate.event.internal.DefaultPostLoadEventListener;
import org.hibernate.event.internal.DefaultPreLoadEventListener;
import org.hibernate.event.internal.DefaultRefreshEventListener;
import org.hibernate.event.internal.DefaultReplicateEventListener;
import org.hibernate.event.internal.DefaultResolveNaturalIdEventListener;
import org.hibernate.event.internal.DefaultSaveEventListener;
import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.internal.DefaultUpdateEventListener;
import org.hibernate.event.internal.PostDeleteEventListenerStandardImpl;
import org.hibernate.event.internal.PostInsertEventListenerStandardImpl;
import org.hibernate.event.internal.PostUpdateEventListenerStandardImpl;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.internal.CallbackRegistryImplementor;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.event.spi.EventType.AUTO_FLUSH;
import static org.hibernate.event.spi.EventType.CLEAR;
import static org.hibernate.event.spi.EventType.DELETE;
import static org.hibernate.event.spi.EventType.DIRTY_CHECK;
import static org.hibernate.event.spi.EventType.EVICT;
import static org.hibernate.event.spi.EventType.FLUSH;
import static org.hibernate.event.spi.EventType.FLUSH_ENTITY;
import static org.hibernate.event.spi.EventType.INIT_COLLECTION;
import static org.hibernate.event.spi.EventType.LOAD;
import static org.hibernate.event.spi.EventType.LOCK;
import static org.hibernate.event.spi.EventType.MERGE;
import static org.hibernate.event.spi.EventType.PERSIST;
import static org.hibernate.event.spi.EventType.PERSIST_ONFLUSH;
import static org.hibernate.event.spi.EventType.POST_COLLECTION_RECREATE;
import static org.hibernate.event.spi.EventType.POST_COLLECTION_REMOVE;
import static org.hibernate.event.spi.EventType.POST_COLLECTION_UPDATE;
import static org.hibernate.event.spi.EventType.POST_COMMIT_DELETE;
import static org.hibernate.event.spi.EventType.POST_COMMIT_INSERT;
import static org.hibernate.event.spi.EventType.POST_COMMIT_UPDATE;
import static org.hibernate.event.spi.EventType.POST_DELETE;
import static org.hibernate.event.spi.EventType.POST_INSERT;
import static org.hibernate.event.spi.EventType.POST_LOAD;
import static org.hibernate.event.spi.EventType.POST_UPDATE;
import static org.hibernate.event.spi.EventType.PRE_COLLECTION_RECREATE;
import static org.hibernate.event.spi.EventType.PRE_COLLECTION_REMOVE;
import static org.hibernate.event.spi.EventType.PRE_COLLECTION_UPDATE;
import static org.hibernate.event.spi.EventType.PRE_DELETE;
import static org.hibernate.event.spi.EventType.PRE_INSERT;
import static org.hibernate.event.spi.EventType.PRE_LOAD;
import static org.hibernate.event.spi.EventType.PRE_UPDATE;
import static org.hibernate.event.spi.EventType.REFRESH;
import static org.hibernate.event.spi.EventType.REPLICATE;
import static org.hibernate.event.spi.EventType.RESOLVE_NATURAL_ID;
import static org.hibernate.event.spi.EventType.SAVE;
import static org.hibernate.event.spi.EventType.SAVE_UPDATE;
import static org.hibernate.event.spi.EventType.UPDATE;

/**
 * Standard implementation of EventListenerRegistry
 *
 * @author Steve Ebersole
 */
public class EventListenerRegistryImpl implements EventListenerRegistry, Stoppable {
	@SuppressWarnings("rawtypes")
	private final EventListenerGroup[] eventListeners;
	private final Map<Class<?>,Object> listenerClassToInstanceMap = new HashMap<>();

	@SuppressWarnings("rawtypes")
	private EventListenerRegistryImpl(EventListenerGroup[] eventListeners) {
		this.eventListeners = eventListeners;
	}

	@SuppressWarnings({ "unchecked" })
	public <T> EventListenerGroup<T> getEventListenerGroup(EventType<T> eventType) {
		if ( eventListeners.length < eventType.ordinal() + 1 ) {
			// eventTpe is a custom EventType that has not been registered.
			// registeredEventListeners array was not allocated enough space to
			// accommodate it.
			throw new HibernateException( "Unable to find listeners for type [" + eventType.eventName() + "]" );
		}
		final EventListenerGroup<T> listeners = eventListeners[ eventType.ordinal() ];
		if ( listeners == null ) {
			throw new HibernateException( "Unable to find listeners for type [" + eventType.eventName() + "]" );
		}
		return listeners;
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		//noinspection rawtypes
		for ( EventListenerGroup group : eventListeners ) {
			if ( group != null ) {
				group.addDuplicationStrategy( strategy );
			}
		}
	}

	@Override
	@SafeVarargs
	public final <T> void setListeners(EventType<T> type, Class<? extends T>... listenerClasses) {
		setListeners( type, resolveListenerInstances( type, listenerClasses ) );
	}

	@SuppressWarnings( {"unchecked"})
	private <T> T[] resolveListenerInstances(EventType<T> type, Class<? extends T>... listenerClasses) {
		T[] listeners = (T[]) Array.newInstance( type.baseListenerInterface(), listenerClasses.length );
		for ( int i = 0; i < listenerClasses.length; i++ ) {
			listeners[i] = resolveListenerInstance( listenerClasses[i] );
		}
		return listeners;
	}

	@SuppressWarnings( {"unchecked"})
	private <T> T resolveListenerInstance(Class<T> listenerClass) {
		T listenerInstance = (T) listenerClassToInstanceMap.get( listenerClass );
		if ( listenerInstance == null ) {
			listenerInstance = instantiateListener( listenerClass );
			listenerClassToInstanceMap.put( listenerClass, listenerInstance );
		}
		return listenerInstance;
	}

	private <T> T instantiateListener(Class<T> listenerClass) {
		try {
			//noinspection deprecation
			return listenerClass.newInstance();
		}
		catch ( Exception e ) {
			throw new EventListenerRegistrationException(
					"Unable to instantiate specified event listener class: " + listenerClass.getName(),
					e
			);
		}
	}

	@Override
	@SafeVarargs
	public final <T> void setListeners(EventType<T> type, T... listeners) {
		final EventListenerGroup<T> registeredListeners = getEventListenerGroup( type );
		registeredListeners.clear();
		if ( listeners != null ) {
			for ( T listener : listeners ) {
				registeredListeners.appendListener( listener );
			}
		}
	}

	@Override
	@SafeVarargs
	public final <T> void appendListeners(EventType<T> type, Class<? extends T>... listenerClasses) {
		appendListeners( type, resolveListenerInstances( type, listenerClasses ) );
	}

	@Override
	@SafeVarargs
	public final <T> void appendListeners(EventType<T> type, T... listeners) {
		getEventListenerGroup( type ).appendListeners( listeners );
	}

	@Override
	@SafeVarargs
	public final <T> void prependListeners(EventType<T> type, Class<? extends T>... listenerClasses) {
		prependListeners( type, resolveListenerInstances( type, listenerClasses ) );
	}

	@Override
	@SafeVarargs
	public final <T> void prependListeners(EventType<T> type, T... listeners) {
		getEventListenerGroup( type ).prependListeners( listeners );
	}

	@Deprecated
	@Override
	public void stop() {
		// legacy - no longer used
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Builder

	public static class Builder {
		private final CallbackRegistryImplementor callbackRegistry;
		private final boolean jpaBootstrap;

		private final Map<EventType<?>,EventListenerGroup<?>> listenerGroupMap = new TreeMap<>(
				Comparator.comparing( EventType::ordinal )
		);

		public Builder(CallbackRegistryImplementor callbackRegistry, boolean jpaBootstrap) {
			this.callbackRegistry = callbackRegistry;
			this.jpaBootstrap = jpaBootstrap;

			applyStandardListeners();
		}

		private void applyStandardListeners() {
			// auto-flush listeners
			prepareListeners( AUTO_FLUSH, new DefaultAutoFlushEventListener() );

			// create listeners
			prepareListeners( PERSIST, new DefaultPersistEventListener() );

			// create-onflush listeners
			prepareListeners( PERSIST_ONFLUSH, new DefaultPersistOnFlushEventListener() );

			// delete listeners
			prepareListeners( DELETE, new DefaultDeleteEventListener() );

			// dirty-check listeners
			prepareListeners( DIRTY_CHECK, new DefaultDirtyCheckEventListener() );

			// evict listeners
			prepareListeners( EVICT, new DefaultEvictEventListener() );

			prepareListeners( CLEAR );

			// flush listeners
			prepareListeners( FLUSH, new DefaultFlushEventListener() );

			// flush-entity listeners
			prepareListeners( FLUSH_ENTITY, new DefaultFlushEntityEventListener() );

			// load listeners
			prepareListeners( LOAD, new DefaultLoadEventListener() );

			// resolve natural-id listeners
			prepareListeners( RESOLVE_NATURAL_ID, new DefaultResolveNaturalIdEventListener() );

			// load-collection listeners
			prepareListeners( INIT_COLLECTION, new DefaultInitializeCollectionEventListener() );

			// lock listeners
			prepareListeners( LOCK, new DefaultLockEventListener() );

			// merge listeners
			prepareListeners( MERGE, new DefaultMergeEventListener() );

			// pre-collection-recreate listeners
			prepareListeners( PRE_COLLECTION_RECREATE );

			// pre-collection-remove listeners
			prepareListeners( PRE_COLLECTION_REMOVE );

			// pre-collection-update listeners
			prepareListeners( PRE_COLLECTION_UPDATE );

			// pre-delete listeners
			prepareListeners( PRE_DELETE );

			// pre-insert listeners
			prepareListeners( PRE_INSERT );

			// pre-load listeners
			prepareListeners( PRE_LOAD, new DefaultPreLoadEventListener() );

			// pre-update listeners
			prepareListeners( PRE_UPDATE );

			// post-collection-recreate listeners
			prepareListeners( POST_COLLECTION_RECREATE );

			// post-collection-remove listeners
			prepareListeners( POST_COLLECTION_REMOVE );

			// post-collection-update listeners
			prepareListeners( POST_COLLECTION_UPDATE );

			// post-commit-delete listeners
			prepareListeners( POST_COMMIT_DELETE );

			// post-commit-insert listeners
			prepareListeners( POST_COMMIT_INSERT );

			// post-commit-update listeners
			prepareListeners( POST_COMMIT_UPDATE );

			// post-delete listeners
			prepareListeners( POST_DELETE, new PostDeleteEventListenerStandardImpl() );

			// post-insert listeners
			prepareListeners( POST_INSERT, new PostInsertEventListenerStandardImpl() );

			// post-load listeners
			prepareListeners( POST_LOAD, new DefaultPostLoadEventListener() );

			// post-update listeners
			prepareListeners( POST_UPDATE, new PostUpdateEventListenerStandardImpl() );

			// update listeners
			prepareListeners( UPDATE, new DefaultUpdateEventListener() );

			// refresh listeners
			prepareListeners( REFRESH, new DefaultRefreshEventListener() );

			// replicate listeners
			prepareListeners( REPLICATE, new DefaultReplicateEventListener() );

			// save listeners
			prepareListeners( SAVE, new DefaultSaveEventListener() );

			// save-update listeners
			prepareListeners( SAVE_UPDATE, new DefaultSaveOrUpdateEventListener() );
		}

		public <T> void prepareListeners(EventType<T> eventType) {
			prepareListeners( eventType, null );
		}

		public <T> void prepareListeners(EventType<T> type, T defaultListener) {
			prepareListeners(
					type,
					defaultListener,
					t -> {
						if ( type == EventType.POST_COMMIT_DELETE
								|| type == EventType.POST_COMMIT_INSERT
								|| type == EventType.POST_COMMIT_UPDATE ) {
							return new PostCommitEventListenerGroupImpl<>( type, callbackRegistry, jpaBootstrap );
						}
						else {
							return new EventListenerGroupImpl<>( type, callbackRegistry, jpaBootstrap );
						}
					}
			);
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		public <T> void prepareListeners(
				EventType<T> type,
				T defaultListener,
				Function<EventType<T>,EventListenerGroupImpl<T>> groupCreator) {
			final EventListenerGroupImpl listenerGroup = groupCreator.apply( type );

			if ( defaultListener != null ) {
				listenerGroup.appendListener( defaultListener );
			}

			listenerGroupMap.put( type, listenerGroup );
		}

		public <T> EventListenerGroup<T> getListenerGroup(EventType<T> eventType) {
			//noinspection unchecked
			return (EventListenerGroup<T>) listenerGroupMap.get( eventType );
		}

		@SuppressWarnings("rawtypes")
		public EventListenerRegistry buildRegistry(Map<String, EventType> registeredEventTypes) {
			// validate contiguity of the event-type ordinals and build the EventListenerGroups array

			final ArrayList<EventType> eventTypeList = new ArrayList<>( registeredEventTypes.values() );
			eventTypeList.sort( Comparator.comparing( EventType::ordinal ) );

			final EventListenerGroup[] eventListeners = new EventListenerGroup[ eventTypeList.size() ];

			int previous = -1;
			for ( int i = 0; i < eventTypeList.size(); i++ ) {
				final EventType eventType = eventTypeList.get( i );

				assert i == eventType.ordinal();
				assert i - 1 == previous;

				eventListeners[i] = listenerGroupMap.get( eventType );

				previous = i;
			}

			return new EventListenerRegistryImpl( eventListeners );
		}
	}
}
