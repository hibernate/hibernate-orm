/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.service.spi;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PostUpsertEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreFlushEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.PreUpsertEventListener;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.service.ServiceRegistry;

import java.util.Objects;

import static org.hibernate.event.spi.EventType.*;

/**
 * Holds the {@link org.hibernate.event.spi event} listener groups for the various event types.
 *
 * @author Sanne Grinovero
 * @author Gavin King
 *
 * @since 7.0
 */
@Internal @Incubating
public final class EventListenerGroups {

	// All session events need to be iterated frequently;
	// CollectionAction and EventAction also need most of these very frequently:
	public final EventListenerGroup<AutoFlushEventListener> eventListenerGroup_AUTO_FLUSH;
	public final EventListenerGroup<PreFlushEventListener> eventListenerGroup_PRE_FLUSH;
	public final EventListenerGroup<ClearEventListener> eventListenerGroup_CLEAR;
	public final EventListenerGroup<DeleteEventListener> eventListenerGroup_DELETE;
	public final EventListenerGroup<DirtyCheckEventListener> eventListenerGroup_DIRTY_CHECK;
	public final EventListenerGroup<EvictEventListener> eventListenerGroup_EVICT;
	public final EventListenerGroup<FlushEntityEventListener> eventListenerGroup_FLUSH_ENTITY;
	public final EventListenerGroup<FlushEventListener> eventListenerGroup_FLUSH;
	public final EventListenerGroup<InitializeCollectionEventListener> eventListenerGroup_INIT_COLLECTION;
	public final EventListenerGroup<LoadEventListener> eventListenerGroup_LOAD;
	public final EventListenerGroup<LockEventListener> eventListenerGroup_LOCK;
	public final EventListenerGroup<MergeEventListener> eventListenerGroup_MERGE;
	public final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST;
	public final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST_ONFLUSH;
	public final EventListenerGroup<PostCollectionRecreateEventListener> eventListenerGroup_POST_COLLECTION_RECREATE;
	public final EventListenerGroup<PostCollectionRemoveEventListener> eventListenerGroup_POST_COLLECTION_REMOVE;
	public final EventListenerGroup<PostCollectionUpdateEventListener> eventListenerGroup_POST_COLLECTION_UPDATE;
	public final EventListenerGroup<PostDeleteEventListener> eventListenerGroup_POST_COMMIT_DELETE;
	public final EventListenerGroup<PostDeleteEventListener> eventListenerGroup_POST_DELETE;
	public final EventListenerGroup<PostInsertEventListener> eventListenerGroup_POST_COMMIT_INSERT;
	public final EventListenerGroup<PostInsertEventListener> eventListenerGroup_POST_INSERT;
	public final EventListenerGroup<PostLoadEventListener> eventListenerGroup_POST_LOAD; //Frequently used by 2LC initialization:
	public final EventListenerGroup<PostUpdateEventListener> eventListenerGroup_POST_COMMIT_UPDATE;
	public final EventListenerGroup<PostUpdateEventListener> eventListenerGroup_POST_UPDATE;
	public final EventListenerGroup<PostUpsertEventListener> eventListenerGroup_POST_UPSERT;
	public final EventListenerGroup<PreCollectionRecreateEventListener> eventListenerGroup_PRE_COLLECTION_RECREATE;
	public final EventListenerGroup<PreCollectionRemoveEventListener> eventListenerGroup_PRE_COLLECTION_REMOVE;
	public final EventListenerGroup<PreCollectionUpdateEventListener> eventListenerGroup_PRE_COLLECTION_UPDATE;
	public final EventListenerGroup<PreDeleteEventListener> eventListenerGroup_PRE_DELETE;
	public final EventListenerGroup<PreInsertEventListener> eventListenerGroup_PRE_INSERT;
	public final EventListenerGroup<PreLoadEventListener> eventListenerGroup_PRE_LOAD;
	public final EventListenerGroup<PreUpdateEventListener> eventListenerGroup_PRE_UPDATE;
	public final EventListenerGroup<PreUpsertEventListener> eventListenerGroup_PRE_UPSERT;
	public final EventListenerGroup<RefreshEventListener> eventListenerGroup_REFRESH;
	public final EventListenerGroup<ReplicateEventListener> eventListenerGroup_REPLICATE;

	private static <T> EventListenerGroup<T> listeners(EventListenerRegistry listenerRegistry, EventType<T> type) {
		return listenerRegistry.getEventListenerGroup( type );
	}

	public EventListenerGroups(ServiceRegistry serviceRegistry) {
		Objects.requireNonNull( serviceRegistry );

		final var eventListenerRegistry = serviceRegistry.requireService( EventListenerRegistry.class );

		// Pre-compute all iterators on Event listeners:

		eventListenerGroup_AUTO_FLUSH = listeners( eventListenerRegistry, AUTO_FLUSH );
		eventListenerGroup_PRE_FLUSH = listeners( eventListenerRegistry, PRE_FLUSH );
		eventListenerGroup_CLEAR = listeners( eventListenerRegistry, CLEAR );
		eventListenerGroup_DELETE = listeners( eventListenerRegistry, DELETE );
		eventListenerGroup_DIRTY_CHECK = listeners( eventListenerRegistry, DIRTY_CHECK );
		eventListenerGroup_EVICT = listeners( eventListenerRegistry, EVICT );
		eventListenerGroup_FLUSH = listeners( eventListenerRegistry, FLUSH );
		eventListenerGroup_FLUSH_ENTITY = listeners( eventListenerRegistry, FLUSH_ENTITY );
		eventListenerGroup_INIT_COLLECTION = listeners( eventListenerRegistry, INIT_COLLECTION );
		eventListenerGroup_LOAD = listeners( eventListenerRegistry, LOAD );
		eventListenerGroup_LOCK = listeners( eventListenerRegistry, LOCK );
		eventListenerGroup_MERGE = listeners( eventListenerRegistry, MERGE );
		eventListenerGroup_PERSIST = listeners( eventListenerRegistry, PERSIST );
		eventListenerGroup_PERSIST_ONFLUSH = listeners( eventListenerRegistry, PERSIST_ONFLUSH );
		eventListenerGroup_POST_COLLECTION_RECREATE = listeners( eventListenerRegistry, POST_COLLECTION_RECREATE );
		eventListenerGroup_POST_COLLECTION_REMOVE = listeners( eventListenerRegistry, POST_COLLECTION_REMOVE );
		eventListenerGroup_POST_COLLECTION_UPDATE = listeners( eventListenerRegistry, POST_COLLECTION_UPDATE );
		eventListenerGroup_POST_COMMIT_DELETE = listeners( eventListenerRegistry, POST_COMMIT_DELETE );
		eventListenerGroup_POST_COMMIT_INSERT = listeners( eventListenerRegistry, POST_COMMIT_INSERT );
		eventListenerGroup_POST_COMMIT_UPDATE = listeners( eventListenerRegistry, POST_COMMIT_UPDATE );
		eventListenerGroup_POST_DELETE = listeners( eventListenerRegistry, POST_DELETE );
		eventListenerGroup_POST_INSERT = listeners( eventListenerRegistry, POST_INSERT );
		eventListenerGroup_POST_LOAD = listeners( eventListenerRegistry, POST_LOAD );
		eventListenerGroup_POST_UPDATE = listeners( eventListenerRegistry, POST_UPDATE );
		eventListenerGroup_POST_UPSERT = listeners( eventListenerRegistry, POST_UPSERT );
		eventListenerGroup_PRE_COLLECTION_RECREATE = listeners( eventListenerRegistry, PRE_COLLECTION_RECREATE );
		eventListenerGroup_PRE_COLLECTION_REMOVE = listeners( eventListenerRegistry, PRE_COLLECTION_REMOVE );
		eventListenerGroup_PRE_COLLECTION_UPDATE = listeners( eventListenerRegistry, PRE_COLLECTION_UPDATE );
		eventListenerGroup_PRE_DELETE = listeners( eventListenerRegistry, PRE_DELETE );
		eventListenerGroup_PRE_INSERT = listeners( eventListenerRegistry, PRE_INSERT );
		eventListenerGroup_PRE_LOAD = listeners( eventListenerRegistry, PRE_LOAD );
		eventListenerGroup_PRE_UPDATE = listeners( eventListenerRegistry, PRE_UPDATE );
		eventListenerGroup_PRE_UPSERT = listeners( eventListenerRegistry, PRE_UPSERT );
		eventListenerGroup_REFRESH = listeners( eventListenerRegistry, REFRESH );
		eventListenerGroup_REPLICATE = listeners( eventListenerRegistry, REPLICATE );
	}
}
