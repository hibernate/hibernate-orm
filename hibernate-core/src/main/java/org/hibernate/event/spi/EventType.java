/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;

/**
 * Enumeration of the recognized types of events, including meta-information about each.
 *
 * @author Steve Ebersole
 */
public final class EventType<T> {
	/**
	 * Used to assign ordinals for the standard event-types
	 */
	private static final AtomicInteger STANDARD_TYPE_COUNTER = new AtomicInteger();

	public static final EventType<LoadEventListener> LOAD = create( "load", LoadEventListener.class );

	public static final EventType<InitializeCollectionEventListener> INIT_COLLECTION = create( "load-collection", InitializeCollectionEventListener.class );

	public static final EventType<PersistEventListener> PERSIST = create( "create", PersistEventListener.class );
	public static final EventType<PersistEventListener> PERSIST_ONFLUSH = create( "create-onflush", PersistEventListener.class );

	public static final EventType<MergeEventListener> MERGE = create( "merge", MergeEventListener.class );

	public static final EventType<DeleteEventListener> DELETE = create( "delete", DeleteEventListener.class );

	public static final EventType<ReplicateEventListener> REPLICATE = create( "replicate", ReplicateEventListener.class );

	public static final EventType<FlushEventListener> FLUSH = create( "flush", FlushEventListener.class );
	public static final EventType<AutoFlushEventListener> AUTO_FLUSH = create( "auto-flush", AutoFlushEventListener.class );
	public static final EventType<DirtyCheckEventListener> DIRTY_CHECK = create( "dirty-check", DirtyCheckEventListener.class );
	public static final EventType<FlushEntityEventListener> FLUSH_ENTITY = create( "flush-entity", FlushEntityEventListener.class );

	public static final EventType<ClearEventListener> CLEAR = create( "clear", ClearEventListener.class );
	public static final EventType<EvictEventListener> EVICT = create( "evict", EvictEventListener.class );

	public static final EventType<LockEventListener> LOCK = create( "lock", LockEventListener.class );

	public static final EventType<RefreshEventListener> REFRESH = create( "refresh", RefreshEventListener.class );

	public static final EventType<PreLoadEventListener> PRE_LOAD = create( "pre-load", PreLoadEventListener.class );
	public static final EventType<PreDeleteEventListener> PRE_DELETE = create( "pre-delete", PreDeleteEventListener.class );
	public static final EventType<PreUpdateEventListener> PRE_UPDATE = create( "pre-update", PreUpdateEventListener.class );
	public static final EventType<PreInsertEventListener> PRE_INSERT = create( "pre-insert", PreInsertEventListener.class );
	public static final EventType<PreUpsertEventListener> PRE_UPSERT = create( "pre-upsert", PreUpsertEventListener.class );

	public static final EventType<PostLoadEventListener> POST_LOAD = create( "post-load", PostLoadEventListener.class );
	public static final EventType<PostDeleteEventListener> POST_DELETE = create( "post-delete", PostDeleteEventListener.class );
	public static final EventType<PostUpdateEventListener> POST_UPDATE = create( "post-update", PostUpdateEventListener.class );
	public static final EventType<PostInsertEventListener> POST_INSERT = create( "post-insert", PostInsertEventListener.class );
	public static final EventType<PostUpsertEventListener> POST_UPSERT = create( "post-upsert", PostUpsertEventListener.class );

	public static final EventType<PostDeleteEventListener> POST_COMMIT_DELETE = create( "post-commit-delete", PostDeleteEventListener.class );
	public static final EventType<PostUpdateEventListener> POST_COMMIT_UPDATE = create( "post-commit-update", PostUpdateEventListener.class );
	public static final EventType<PostInsertEventListener> POST_COMMIT_INSERT = create( "post-commit-insert", PostInsertEventListener.class );

	public static final EventType<PreCollectionRecreateEventListener> PRE_COLLECTION_RECREATE = create( "pre-collection-recreate", PreCollectionRecreateEventListener.class );
	public static final EventType<PreCollectionRemoveEventListener> PRE_COLLECTION_REMOVE = create( "pre-collection-remove", PreCollectionRemoveEventListener.class );
	public static final EventType<PreCollectionUpdateEventListener> PRE_COLLECTION_UPDATE = create( "pre-collection-update", PreCollectionUpdateEventListener.class );

	public static final EventType<PostCollectionRecreateEventListener> POST_COLLECTION_RECREATE = create( "post-collection-recreate", PostCollectionRecreateEventListener.class );
	public static final EventType<PostCollectionRemoveEventListener> POST_COLLECTION_REMOVE = create( "post-collection-remove", PostCollectionRemoveEventListener.class );
	public static final EventType<PostCollectionUpdateEventListener> POST_COLLECTION_UPDATE = create( "post-collection-update", PostCollectionUpdateEventListener.class );

	/**
	 * Maintain a map of {@link EventType} instances keyed by name for lookup by name as well as {@link #values()}
	 * resolution.
	 */
	private static final Map<String,EventType<?>> STANDARD_TYPE_BY_NAME_MAP = initStandardTypeNameMap();

	private static Map<String, EventType<?>> initStandardTypeNameMap() {
		final Map<String, EventType<?>> typeByNameMap = new HashMap<>();
		for ( Field field : EventType.class.getDeclaredFields() ) {
			if ( EventType.class.isAssignableFrom( field.getType() ) ) {
				try {
					final EventType<?> typeField = (EventType<?>) field.get( null );
					typeByNameMap.put( typeField.eventName(), typeField );
				}
				catch ( Exception t ) {
					throw new HibernateException( "Unable to initialize EventType map", t );
				}
			}
		}
		return Collections.unmodifiableMap( typeByNameMap );
	}

	private static <T> EventType<T> create(String name, Class<T> listenerRole) {
		return new EventType<>( name, listenerRole, STANDARD_TYPE_COUNTER.getAndIncrement(), true );
	}

	public static <T> EventType<T> create(String name, Class<T> listenerRole, int ordinal) {
		return new EventType<>( name, listenerRole, ordinal, false );
	}

	/**
	 * Find an {@link EventType} by its name
	 *
	 * @param eventName The name
	 *
	 * @return The {@link EventType} instance.
	 *
	 * @throws HibernateException If eventName is null, or if eventName does not correlate to any known event type.
	 */
	public static EventType<?> resolveEventTypeByName(final String eventName) {
		if ( eventName == null ) {
			throw new HibernateException( "event name to resolve cannot be null" );
		}
		final EventType<?> eventType = STANDARD_TYPE_BY_NAME_MAP.get( eventName );
		if ( eventType == null ) {
			throw new HibernateException( "Unable to locate proper event type for event name [" + eventName + "]" );
		}
		return eventType;
	}

	/**
	 * Get a collection of all the standard {@link EventType} instances.
	 */
	public static Collection<EventType<?>> values() {
		return STANDARD_TYPE_BY_NAME_MAP.values();
	}

	/**
	 * Used from {@link EventEngine} to "prime" the registered event-type map.
	 *
	 * Simply copy the values into its (passed) Map
	 */
	static void registerStandardTypes(Map<String, EventType<?>> eventTypes) {
		eventTypes.putAll( STANDARD_TYPE_BY_NAME_MAP );
	}

	private final String eventName;
	private final Class<T> baseListenerInterface;
	private final int ordinal;
	private final boolean isStandardEvent;

	private EventType(String eventName, Class<T> baseListenerInterface, int ordinal, boolean isStandardEvent) {
		this.eventName = eventName;
		this.baseListenerInterface = baseListenerInterface;
		this.ordinal = ordinal;
		this.isStandardEvent = isStandardEvent;
	}

	public String eventName() {
		return eventName;
	}

	public Class<T> baseListenerInterface() {
		return baseListenerInterface;
	}

	/**
	 * EventType is effectively an enumeration. Since there is a known, limited number of possible types, we expose an
	 * ordinal for each in order to be able to efficiently do associations elsewhere in the codebase (array vs. Map)
	 *
	 * For the total number of types, see {@link #values()}
	 *
	 * @return A unique ordinal for this {@link EventType}, starting at 0 and up to the number of distinct events
	 */
	public int ordinal() {
		return ordinal;
	}

	/**
	 * Is this event-type one of the standard event-types?
	 */
	public boolean isStandardEvent() {
		return isStandardEvent;
	}

	@Override
	public String toString() {
		return eventName();
	}
}
