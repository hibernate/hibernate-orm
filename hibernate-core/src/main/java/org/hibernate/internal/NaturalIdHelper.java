/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.internal.NaturalIdLogging.NATURAL_ID_LOGGER;

/**
 * @author Gavin King
 */
public class NaturalIdHelper {

	public static String[] getNaturalIdPropertyNames(EntityPersister persister) {
		final int[] naturalIdPropertyIndices = persister.getNaturalIdentifierProperties();
		if ( naturalIdPropertyIndices == null ) {
			throw new IdentifierGenerationException( "Entity '" + persister.getEntityName()
					+ "' has no '@NaturalId' property" );
		}
		if ( persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			throw new IdentifierGenerationException( "Entity '" + persister.getEntityName()
					+ "' has a '@NaturalId' property which is also defined as insert-generated" );
		}
		final String[] allPropertyNames = persister.getPropertyNames();
		final String[] propertyNames = new String[naturalIdPropertyIndices.length];
		for ( int i = 0; i < naturalIdPropertyIndices.length; i++ ) {
			propertyNames[i] = allPropertyNames[naturalIdPropertyIndices[i]];
		}
		return propertyNames;
	}

	public static void performAnyNeededCrossReferenceSynchronizations(
			boolean synchronizationEnabled,
			EntityMappingType entityMappingType,
			SharedSessionContractImplementor session) {
		// first check if synchronization (this process) was disabled
		if ( synchronizationEnabled
				// only mutable natural-ids need this processing
				&& entityMappingType.getNaturalIdMapping().isMutable()
				// skip synchronization when not in a transaction
				&& session.isTransactionInProgress() ) {
			final var persister = entityMappingType.getEntityPersister();
			final var persistenceContext = session.getPersistenceContextInternal();
			final var naturalIdResolutions = persistenceContext.getNaturalIdResolutions();
			final boolean loggerDebugEnabled = NATURAL_ID_LOGGER.isDebugEnabled();
			for ( Object id : naturalIdResolutions.getCachedPkResolutions( persister ) ) {
				final var entityKey = session.generateEntityKey( id, persister );
				final Object entity = persistenceContext.getEntity( entityKey );
				final var entry = persistenceContext.getEntry( entity );
				if ( entry != null ) {
					if ( entry.requiresDirtyCheck( entity )
							// MANAGED is the only status we care about here
							&& entry.getStatus() == Status.MANAGED ) {
						naturalIdResolutions.handleSynchronization( id, entity, persister );
					}
				}
				else {
					// no entry
					if ( loggerDebugEnabled ) {
						NATURAL_ID_LOGGER.debugf(
								"Cached natural-id/pk resolution linked to missing EntityEntry in persistence context: %s#%s",
								entityMappingType.getEntityName(),
								id
						);
					}
				}
			}
		}
	}
}
