/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.event.internal.EventUtil.getLoggableName;

public enum EntityState {
	PERSISTENT, TRANSIENT, DETACHED, DELETED;

	static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityState.class );

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
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Persistent instance of: " + getLoggableName( entityName, entity ) );
				}
				return PERSISTENT;
			}
			// ie. e.status==DELETED
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Deleted instance of: " + getLoggableName( entityName, entity ) );
			}
			return DELETED;
		}
		// the object is transient or detached

		// the entity is not associated with the session, so
		// try interceptor and unsaved-value

		if ( ForeignKeys.isTransient( entityName, entity, assumedUnsaved, source ) ) {
			if ( LOG.isTraceEnabled() ) {
				LOG.trace( "Transient instance of: " + getLoggableName( entityName, entity ) );
			}
			return TRANSIENT;
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Detached instance of: " + getLoggableName( entityName, entity ) );
		}

		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		if ( persistenceContext.containsDeletedUnloadedEntityKeys() ) {
			final EntityPersister entityPersister = source.getEntityPersister( entityName, entity );
			final Object identifier = entityPersister.getIdentifier( entity, source );
			final EntityKey entityKey = source.generateEntityKey( identifier, entityPersister );
			if ( persistenceContext.containsDeletedUnloadedEntityKey( entityKey ) ) {
				return EntityState.DELETED;
			}
		}
		return DETACHED;
	}

}
