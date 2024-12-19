/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Map;

/**
 * Determines if the current session holds modified state which
 * would be synchronized with the database if the session were
 * flushed. Approximately reproduces the logic that is executed
 * on an {@link org.hibernate.event.spi.AutoFlushEvent}.
 *
 * @author Gavin King
 */
public class DefaultDirtyCheckEventListener implements DirtyCheckEventListener {

	@Override
	public void onDirtyCheck(DirtyCheckEvent event) throws HibernateException {
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final Map<EntityKey, EntityHolder> entityHoldersByKey = persistenceContext.getEntityHoldersByKey();
		if ( entityHoldersByKey != null ) {
			for ( Map.Entry<EntityKey, EntityHolder> entry :
					entityHoldersByKey.entrySet() ) {
				final EntityHolder holder = entry.getValue();
				final EntityEntry entityEntry = holder.getEntityEntry();
				final Status status = entityEntry.getStatus();
				if ( status != Status.MANAGED && status != Status.GONE
					|| isEntityDirty( holder.getManagedObject(), holder.getDescriptor(), entityEntry, session ) ) {
					event.setDirty( true );
					return;
				}
			}
		}
		final Map<PersistentCollection<?>, CollectionEntry> entries =
				persistenceContext.getCollectionEntries();
		if ( entries != null ) {
			for ( Map.Entry<PersistentCollection<?>, CollectionEntry> entry :
					entries.entrySet() ) {
				if ( isCollectionDirty( entry.getKey(), entry.getValue().getLoadedPersister() ) ) {
					event.setDirty( true );
					return;
				}
			}
		}
	}

	private static boolean isEntityDirty(
			Object entity, EntityPersister descriptor, EntityEntry entityEntry, EventSource session) {
		if ( entityEntry.requiresDirtyCheck( entity ) ) {
			// the following implementation ignores Interceptor,
			// CustomEntityDirtinessStrategy, bytecode dirtiness
			final Object[] propertyValues =
					entityEntry.getStatus() == Status.DELETED
							? entityEntry.getDeletedState()
							: descriptor.getValues( entity );
			final int[] dirty =
					descriptor.findDirty( propertyValues, entityEntry.getLoadedState(), entity, session );
			return dirty != null;
		}
		else {
			return false;
		}
	}

	private static boolean isCollectionDirty(PersistentCollection<?> collection, CollectionPersister loadedPersister) {
		return collection.isDirty()
			|| collection.wasInitialized()
				&& loadedPersister != null
				&& loadedPersister.isMutable() //optimization
//				&& !loadedPersister.isInverse() // even if it's inverse, could still result in a cache update
				&& ( collection.isDirectlyAccessible() || loadedPersister.getElementType().isMutable() ) //optimization
				&& !collection.equalsSnapshot( loadedPersister );
	}
}
