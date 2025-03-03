/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;


/**
 * Determines if the current session holds modified state which
 * would be synchronized with the database if the session were
 * flushed. Approximately reproduces the logic that is executed
 * on an {@link org.hibernate.event.spi.AutoFlushEvent}.
 *
 * @implNote Historically, {@link org.hibernate.Session#isDirty}
 * was implemented to actually execute a partial flush and then
 * return {@code true} if there were any resulting operations to
 * be executed. This was extremely inefficient and side-effecty.
 * The new implementation below is non-perfect and probably
 * misses some subtleties, but it's I guess OK to view it as an
 * approximation.
 *
 * @author Gavin King
 */
public class DefaultDirtyCheckEventListener implements DirtyCheckEventListener {

	@Override
	public void onDirtyCheck(DirtyCheckEvent event) throws HibernateException {
		final EventSource session = event.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final var holdersByKey = persistenceContext.getEntityHoldersByKey();
		if ( holdersByKey != null ) {
			for ( var entry : holdersByKey.entrySet() ) {
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
		final var entriesByCollection = persistenceContext.getCollectionEntries();
		if ( entriesByCollection != null ) {
			for ( var entry : entriesByCollection.entrySet() ) {
				if ( isCollectionDirty( entry.getKey(), entry.getValue().getLoadedPersister() ) ) {
					event.setDirty( true );
					return;
				}
			}
		}
	}

	private static boolean isEntityDirty(
			Object entity, EntityPersister descriptor, EntityEntry entityEntry, EventSource session) {
		if ( entityEntry.requiresDirtyCheck( entity ) ) { // takes into account CustomEntityDirtinessStrategy
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
