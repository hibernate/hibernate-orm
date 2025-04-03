/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;


import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jan Schatteman
 */
public abstract class AbstractMultiNaturalIdLoader<E> implements MultiNaturalIdLoader<E> {
	private final EntityMappingType entityDescriptor;

	protected MultiNaturalIdLoadOptions options;

	public AbstractMultiNaturalIdLoader(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, EventSource eventSource) {
		assert naturalIds != null;

		this.options = options;

		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		return options.isOrderReturnEnabled()
				? performOrderedMultiLoad( naturalIds, options, eventSource )
				: performUnorderedMultiLoad( naturalIds, options, eventSource );
	}

	private <K> List<E> performUnorderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, EventSource eventSource) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "Unordered MultiLoad Starting - `%s`", getEntityDescriptor().getEntityName() );
		}

		return unorderedMultiLoad(
				naturalIds,
				eventSource,
				options.getLockOptions() == null ? LockOptions.NONE : options.getLockOptions()
		);
	}

	protected abstract <K> List<E> unorderedMultiLoad(K[] naturalIds, EventSource eventSource, LockOptions lockOptions);

	private <K> List<E> performOrderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, EventSource eventSource) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "Ordered MultiLoad Starting - `%s`", getEntityDescriptor().getEntityName() );
		}

		return orderedMultiLoad(
				naturalIds,
				eventSource,
				options.getLockOptions() == null ? LockOptions.NONE : options.getLockOptions()
		);
	}

	protected <K> List<E> orderedMultiLoad( K[] naturalIds, EventSource eventSource, LockOptions lockOptions ) {

		unorderedMultiLoad( naturalIds, eventSource, lockOptions );

		return handleResults( naturalIds, eventSource, lockOptions );
	}

	protected <K> List<E> handleResults( K[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions ) {
		List<E> results = new ArrayList<>(naturalIds.length);
		for ( int i = 0; i < naturalIds.length; i++ ) {
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

			Object id = persistenceContext.getNaturalIdResolutions().findCachedIdByNaturalId( naturalIds[i], getEntityDescriptor() );

			// Id can be null if a non-existent natural id is requested
			Object entity = id == null ? null
					: persistenceContext.getEntity( new EntityKey( id, getEntityDescriptor().getEntityPersister() ) );
			if ( entity != null && !options.isReturnOfDeletedEntitiesEnabled() ) {
				// make sure it is not DELETED
				final EntityEntry entry = persistenceContext.getEntry( entity );
				if ( entry.getStatus().isDeletedOrGone() ) {
					// the entity is locally deleted, and the options ask that we not return such entities...
					entity = null;
				}
				else {
					entity = persistenceContext.proxyFor( entity );
				}
			}
			results.add( (E) entity );
		}

		return results;
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}
}
