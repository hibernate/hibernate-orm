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
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.util.Collections;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.loader.ast.internal.LoaderHelper.upgradeLock;

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
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		assert naturalIds != null;

		this.options = options;

		if ( naturalIds.length == 0 ) {
			return Collections.emptyList();
		}

		return options.isOrderReturnEnabled()
				? performOrderedMultiLoad( naturalIds, options, session )
				: performUnorderedMultiLoad( naturalIds, options, session );
	}

	private <K> List<E> performUnorderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "Unordered MultiLoad Starting - `%s`", getEntityDescriptor().getEntityName() );
		}

		return unorderedMultiLoad(
				naturalIds,
				session,
				options.getLockOptions() == null ? LockOptions.NONE : options.getLockOptions()
		);
	}

	protected <K> List<E> unorderedMultiLoad(K[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions) {
		final List<E> results = arrayList( naturalIds.length );
		final Object[] unresolvedIds =
				checkPersistenceContextForCachedResults( naturalIds, session, lockOptions, results );
		if ( !isEmpty( unresolvedIds ) ) {
			results.addAll( loadEntitiesWithUnresolvedIds(unresolvedIds, session, lockOptions) );
		}

		return results;
	}

	protected abstract List<E> loadEntitiesWithUnresolvedIds(Object[] unresolvedIds, SharedSessionContractImplementor session, LockOptions lockOptions);

	private <K> List<E> performOrderedMultiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef( "Ordered MultiLoad Starting - `%s`", getEntityDescriptor().getEntityName() );
		}

		return orderedMultiLoad(
				naturalIds,
				session,
				options.getLockOptions() == null ? LockOptions.NONE : options.getLockOptions()
		);
	}

	protected <K> List<E> orderedMultiLoad( K[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions ) {

		unorderedMultiLoad( naturalIds, session, lockOptions );

		return sortResults( naturalIds, session, lockOptions );
	}

	protected <K> List<E> sortResults( K[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions ) {
		List<E> results = arrayList(naturalIds.length);
		for ( int i = 0; i < naturalIds.length; i++ ) {
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

			Object id = persistenceContext.getNaturalIdResolutions().findCachedIdByNaturalId( naturalIds[i], getEntityDescriptor() );

			// Id can be null if a non-existent natural id is requested
			Object entity = (id == null) ? null : persistenceContext.getEntity( new EntityKey( id, getEntityDescriptor().getEntityPersister() ) );
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

	private <K> Object[] checkPersistenceContextForCachedResults( K[] naturalIds, SharedSessionContractImplementor session, LockOptions lockOptions, List<E> results ) {
		List<K> unresolvedIds = arrayList(naturalIds.length);

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		for ( int i = 0; i < naturalIds.length; i++ ) {

			final Object normalizedNaturalId = getEntityDescriptor().getNaturalIdMapping().normalizeInput( naturalIds[i] );
			Object id = persistenceContext.getNaturalIdResolutions().findCachedIdByNaturalId( normalizedNaturalId, getEntityDescriptor() );

			// Id can be null if a non-existent natural id is requested, or a mutable natural id was changed and then deleted
			Object entity = id == null ? null : persistenceContext.getEntity( new EntityKey( id, getEntityDescriptor().getEntityPersister() ) );

			if ( entity != null ) {
				// Entity is already in the persistence context
				final EntityEntry entry = persistenceContext.getEntry( entity );
				// either a managed entry, or a deleted one with returnDeleted enabled
				if ( !entry.getStatus().isDeletedOrGone() || options.isReturnOfDeletedEntitiesEnabled() ) {
					results.add( (E) persistenceContext.proxyFor(entity) );
					upgradeLock( entity, entry, lockOptions, session.getSession().asEventSource() );
				}
			}
			else {
				// entity either doesn't exist or hasn't been loaded in the PC yet, in both cases we add the natural id
				// to the ids that still need to be recovered, in case the id corresponds to a non-existent
				// instance nothing will be in the results for it, which is ok in unordered multiload
				unresolvedIds.add(naturalIds[i]);
			}
		}

		return unresolvedIds.toArray( new Object[0] );
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}
}
