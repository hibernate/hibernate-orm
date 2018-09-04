/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.internal;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.event.service.spi.EventListenerRegistrationException;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.jpa.event.internal.CallbackRegistryImplementor;
import org.hibernate.jpa.event.internal.CallbacksFactory;
import org.hibernate.jpa.event.spi.CallbackBuilder;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.spi.ServiceRegistryImplementor;
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
 * @author Steve Ebersole
 */
public class EventListenerRegistryImpl implements EventListenerRegistry, Stoppable {
	private Map<Class,Object> listenerClassToInstanceMap = new HashMap<>();

	private final SessionFactoryImplementor sessionFactory;
	private final CallbackRegistryImplementor callbackRegistry;
	private final EventListenerGroupImpl[] registeredEventListeners;
	private CallbackBuilder callbackBuilder;

	/**
	 * @deprecated Use {@link EventListenerRegistryImpl#EventListenerRegistryImpl(BootstrapContext, SessionFactoryImplementor)} instead
	 */
	@Deprecated
	EventListenerRegistryImpl(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		this.sessionFactory = sessionFactory;

		this.callbackRegistry = CallbacksFactory.buildCallbackRegistry( sessionFactory );

		this.registeredEventListeners = buildListenerGroups();
	}

	EventListenerRegistryImpl(BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.callbackRegistry = CallbacksFactory.buildCallbackRegistry( sessionFactory );
		this.callbackBuilder = CallbacksFactory.buildCallbackBuilder(
				sessionFactory, bootstrapContext.getReflectionManager() );
		this.registeredEventListeners = buildListenerGroups();
	}

	SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	CallbackRegistry getCallbackRegistry() {
		return callbackRegistry;
	}

	@Override
	public void prepare(MetadataImplementor metadata) {
		if ( callbackBuilder == null ) {
			// TODO : not needed anymore when the deprecate constructor will be removed
			this.callbackBuilder = CallbacksFactory.buildCallbackBuilder( sessionFactory, metadata.getMetadataBuildingOptions().getReflectionManager()
			);
		}
		for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
			if ( persistentClass.getClassName() == null ) {
				// we can have non java class persisted by hibernate
				continue;
			}
			callbackBuilder.buildCallbacksForEntity( persistentClass.getClassName(), callbackRegistry );

			for ( Iterator propertyIterator = persistentClass.getDeclaredPropertyIterator();
					propertyIterator.hasNext(); ) {
				Property property = (Property) propertyIterator.next();

				if ( property.getType().isComponentType() ) {
					callbackBuilder.buildCallbacksForEmbeddable(
							property,
							persistentClass.getClassName(),
							callbackRegistry
					);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public <T> EventListenerGroupImpl<T> getEventListenerGroup(EventType<T> eventType) {
		EventListenerGroupImpl<T> listeners = registeredEventListeners[ eventType.ordinal() ];
		if ( listeners == null ) {
			throw new HibernateException( "Unable to find listeners for type [" + eventType.eventName() + "]" );
		}
		return listeners;
	}

	@Override
	public void addDuplicationStrategy(DuplicationStrategy strategy) {
		for ( EventListenerGroupImpl group : registeredEventListeners ) {
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
		EventListenerGroupImpl<T> registeredListeners = getEventListenerGroup( type );
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

	private EventListenerGroupImpl[] buildListenerGroups() {
		EventListenerGroupImpl[] listenerArray = new EventListenerGroupImpl[ EventType.values().size() ];

		// auto-flush listeners
		prepareListeners(
				AUTO_FLUSH,
				new DefaultAutoFlushEventListener(),
				listenerArray
		);

		// create listeners
		prepareListeners(
				PERSIST,
				new DefaultPersistEventListener(),
				listenerArray
		);

		// create-onflush listeners
		prepareListeners(
				PERSIST_ONFLUSH,
				new DefaultPersistOnFlushEventListener(),
				listenerArray
		);

		// delete listeners
		prepareListeners(
				DELETE,
				new DefaultDeleteEventListener(),
				listenerArray
		);

		// dirty-check listeners
		prepareListeners(
				DIRTY_CHECK,
				new DefaultDirtyCheckEventListener(),
				listenerArray
		);

		// evict listeners
		prepareListeners(
				EVICT,
				new DefaultEvictEventListener(),
				listenerArray
		);

		prepareListeners(
				CLEAR,
				listenerArray
		);

		// flush listeners
		prepareListeners(
				FLUSH,
				new DefaultFlushEventListener(),
				listenerArray
		);

		// flush-entity listeners
		prepareListeners(
				FLUSH_ENTITY,
				new DefaultFlushEntityEventListener(),
				listenerArray
		);

		// load listeners
		prepareListeners(
				LOAD,
				new DefaultLoadEventListener(),
				listenerArray
		);

		// resolve natural-id listeners
		prepareListeners( 
				RESOLVE_NATURAL_ID, 
				new DefaultResolveNaturalIdEventListener(), 
				listenerArray
		);

		// load-collection listeners
		prepareListeners(
				INIT_COLLECTION,
				new DefaultInitializeCollectionEventListener(),
				listenerArray
		);

		// lock listeners
		prepareListeners(
				LOCK,
				new DefaultLockEventListener(),
				listenerArray
		);

		// merge listeners
		prepareListeners(
				MERGE,
				new DefaultMergeEventListener(),
				listenerArray
		);

		// pre-collection-recreate listeners
		prepareListeners(
				PRE_COLLECTION_RECREATE,
				listenerArray
		);

		// pre-collection-remove listeners
		prepareListeners(
				PRE_COLLECTION_REMOVE,
				listenerArray
		);

		// pre-collection-update listeners
		prepareListeners(
				PRE_COLLECTION_UPDATE,
				listenerArray
		);

		// pre-delete listeners
		prepareListeners(
				PRE_DELETE,
				listenerArray
		);

		// pre-insert listeners
		prepareListeners(
				PRE_INSERT,
				listenerArray
		);

		// pre-load listeners
		prepareListeners(
				PRE_LOAD,
				new DefaultPreLoadEventListener(),
				listenerArray
		);

		// pre-update listeners
		prepareListeners(
				PRE_UPDATE,
				listenerArray
		);

		// post-collection-recreate listeners
		prepareListeners(
				POST_COLLECTION_RECREATE,
				listenerArray
		);

		// post-collection-remove listeners
		prepareListeners(
				POST_COLLECTION_REMOVE,
				listenerArray
		);

		// post-collection-update listeners
		prepareListeners(
				POST_COLLECTION_UPDATE,
				listenerArray
		);

		// post-commit-delete listeners
		prepareListeners(
				POST_COMMIT_DELETE,
				listenerArray
		);

		// post-commit-insert listeners
		prepareListeners(
				POST_COMMIT_INSERT,
				listenerArray
		);

		// post-commit-update listeners
		prepareListeners(
				POST_COMMIT_UPDATE,
				listenerArray
		);

		// post-delete listeners
		prepareListeners(
				POST_DELETE,
				new PostDeleteEventListenerStandardImpl(),
				listenerArray
		);

		// post-insert listeners
		prepareListeners(
				POST_INSERT,
				new PostInsertEventListenerStandardImpl(),
				listenerArray
		);

		// post-load listeners
		prepareListeners(
				POST_LOAD,
				new DefaultPostLoadEventListener(),
				listenerArray
		);

		// post-update listeners
		prepareListeners(
				POST_UPDATE,
				new PostUpdateEventListenerStandardImpl(),
				listenerArray
		);

		// update listeners
		prepareListeners(
				UPDATE,
				new DefaultUpdateEventListener(),
				listenerArray
		);

		// refresh listeners
		prepareListeners(
				REFRESH,
				new DefaultRefreshEventListener(),
				listenerArray
		);

		// replicate listeners
		prepareListeners(
				REPLICATE,
				new DefaultReplicateEventListener(),
				listenerArray
		);

		// save listeners
		prepareListeners(
				SAVE,
				new DefaultSaveEventListener(),
				listenerArray
		);

		// save-update listeners
		prepareListeners(
				SAVE_UPDATE,
				new DefaultSaveOrUpdateEventListener(),
				listenerArray
		);

		return listenerArray;
	}

	private <T> void prepareListeners(EventType<T> type, EventListenerGroupImpl[] listenerArray) {
		prepareListeners( type, null, listenerArray );
	}

	private <T> void prepareListeners(EventType<T> type, T defaultListener, EventListenerGroupImpl[] listenerArray) {
		final EventListenerGroupImpl<T> listenerGroup;
		if ( type == EventType.POST_COMMIT_DELETE
				|| type == EventType.POST_COMMIT_INSERT
				|| type == EventType.POST_COMMIT_UPDATE ) {
			listenerGroup = new PostCommitEventListenerGroupImpl<T>( type, this );
		}
		else {
			listenerGroup = new EventListenerGroupImpl<T>( type, this );
		}

		if ( defaultListener != null ) {
			listenerGroup.appendListener( defaultListener );
		}
		listenerArray[ type.ordinal() ] = listenerGroup;
	}

	@Override
	public void stop() {
		if ( callbackRegistry != null ) {
			callbackRegistry.release();
		}
		if ( callbackBuilder != null ) {
			callbackBuilder.release();
		}
	}
}
