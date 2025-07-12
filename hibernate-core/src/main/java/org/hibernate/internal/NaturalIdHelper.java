/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.util.Collection;

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

		if ( !synchronizationEnabled ) {
			// synchronization (this process) was disabled
			return;
		}

		final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();

		if ( !naturalIdMapping.isMutable() ) {
			// only mutable natural-ids need this processing
			return;
		}

		if ( ! session.isTransactionInProgress() ) {
			// not in a transaction so skip synchronization
			return;
		}

		final EntityPersister entityPersister = entityMappingType.getEntityPersister();

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final Collection<?> cachedPkResolutions =
				persistenceContext.getNaturalIdResolutions()
						.getCachedPkResolutions( entityPersister );
		final boolean loggerDebugEnabled = LoaderLogging.LOADER_LOGGER.isDebugEnabled();
		for ( Object pk : cachedPkResolutions ) {
			final EntityKey entityKey = session.generateEntityKey( pk, entityPersister );
			final Object entity = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( entity );

			if ( entry == null ) {
				if ( loggerDebugEnabled ) {
					LoaderLogging.LOADER_LOGGER.debugf(
							"Cached natural-id/pk resolution linked to null EntityEntry in persistence context: %s#%s",
							entityMappingType.getEntityName(),
							pk
					);
				}
				continue;
			}

			if ( !entry.requiresDirtyCheck( entity ) ) {
				continue;
			}

			// MANAGED is the only status we care about here...
			if ( entry.getStatus() != Status.MANAGED ) {
				continue;
			}

			persistenceContext.getNaturalIdResolutions().handleSynchronization( pk, entity, entityPersister );
		}
	}

}
