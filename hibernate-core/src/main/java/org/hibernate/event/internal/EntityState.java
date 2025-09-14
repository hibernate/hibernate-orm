/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;

public enum EntityState {
	PERSISTENT, TRANSIENT, DETACHED, DELETED;

	/**
	 * Determine whether the entity is persistent, detached, or transient
	 *
	 * @param entity The entity to check
	 * @param entityName The name of the entity
	 * @param entry The entity's entry in the persistence context
	 * @param source The originating session.
	 *
	 * @return The state.
	 */
	public static EntityState getEntityState(
			Object entity,
			String entityName,
			EntityEntry entry, //pass this as an argument only to avoid double looking
			SessionImplementor source,
			Boolean assumedUnsaved) {

		if ( entry != null ) { // the object is persistent
			//the entity is associated with the session, so check its status
			if ( entry.getStatus() != Status.DELETED ) {
				// do nothing for persistent instances
//				if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
//					EVENT_LISTENER_LOGGER.persistentInstance( getLoggableName( entityName, entity ) );
//				}
				return PERSISTENT;
			}
			else {
				// must be deleted instance
//				if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
//					EVENT_LISTENER_LOGGER.deletedInstance( getLoggableName( entityName, entity ) );
//				}
				return DELETED;
			}
		}
		// the object is transient or detached

		// the entity is not associated with the session, so
		// try interceptor and unsaved-value

		else if ( ForeignKeys.isTransient( entityName, entity, assumedUnsaved, source ) ) {
//			if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
//				EVENT_LISTENER_LOGGER.transientInstance( getLoggableName( entityName, entity ) );
//			}
			return TRANSIENT;
		}
		else {
//			if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
//				EVENT_LISTENER_LOGGER.detachedInstance( getLoggableName( entityName, entity ) );
//			}
			final var persistenceContext = source.getPersistenceContextInternal();
			if ( persistenceContext.containsDeletedUnloadedEntityKeys() ) {
				final var entityPersister = source.getEntityPersister( entityName, entity );
				final Object identifier = entityPersister.getIdentifier( entity, source );
				final var entityKey = source.generateEntityKey( identifier, entityPersister );
				if ( persistenceContext.containsDeletedUnloadedEntityKey( entityKey ) ) {
					return EntityState.DELETED;
				}
			}
			return DETACHED;
		}
	}

}
