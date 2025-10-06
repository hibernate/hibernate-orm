/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;


import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.loader.ast.internal.LoaderHelper.upgradeLock;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * @author Jan Schatteman
 */
public abstract class AbstractMultiNaturalIdLoader<E> implements MultiNaturalIdLoader<E> {
	private final EntityMappingType entityDescriptor;

	public AbstractMultiNaturalIdLoader(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public <K> List<E> multiLoad(K[] naturalIds, MultiNaturalIdLoadOptions options, SharedSessionContractImplementor session) {
		assert naturalIds != null;
		if ( naturalIds.length == 0 ) {
			return emptyList();
		}
		else {
			return options.isOrderReturnEnabled()
					? performOrderedMultiLoad( naturalIds, options, session )
					: performUnorderedMultiLoad( naturalIds, options, session );
		}
	}

	private <K> List<E> performUnorderedMultiLoad(
			K[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.unorderedBatchLoadStarting( getEntityDescriptor().getEntityName() );
		}
		return unorderedMultiLoad( naturalIds, loadOptions, session );
	}

	private static LockOptions lockOptions(MultiNaturalIdLoadOptions loadOptions) {
		final var lockOptions = loadOptions.getLockOptions();
		return lockOptions == null ? new LockOptions() : lockOptions;
	}

	private <K> List<E> unorderedMultiLoad(
			K[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final List<E> results = arrayList( naturalIds.length );
		final var lockOptions = lockOptions( loadOptions );
		final var unresolvedIds =
				checkPersistenceContextForCachedResults( naturalIds, loadOptions, session, lockOptions, results::add );
		if ( !isEmpty( unresolvedIds ) ) {
			results.addAll( loadEntitiesWithUnresolvedIds( unresolvedIds, loadOptions, lockOptions, session ) );
		}
		return results;
	}

	protected abstract List<E> loadEntitiesWithUnresolvedIds(
			Object[] unresolvedIds,
			MultiNaturalIdLoadOptions loadOptions,
			LockOptions lockOptions,
			SharedSessionContractImplementor session);

	private <K> List<E> performOrderedMultiLoad(
			K[] naturalIds,
			MultiNaturalIdLoadOptions options,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.orderedMultiLoadStarting( getEntityDescriptor().getEntityName() );
		}
		return orderedMultiLoad( naturalIds, options, session );
	}

	private <K> List<E> orderedMultiLoad(
			K[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final var lockOptions = lockOptions( loadOptions );
		final var unresolvedIds =
				checkPersistenceContextForCachedResults( naturalIds, loadOptions, session, lockOptions, result -> {} );
		if ( !isEmpty( unresolvedIds ) ) {
			loadEntitiesWithUnresolvedIds( unresolvedIds, loadOptions, lockOptions, session );
		}
		return sortResults( naturalIds, loadOptions, session );
	}

	private <K> List<E> sortResults(
			K[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final var context = session.getPersistenceContextInternal();
		final List<E> results = arrayList( naturalIds.length );
		for ( K naturalId : naturalIds ) {
			final Object entity = entityForNaturalId( context, naturalId );
			final Object result;
			if ( entity == null
				// the entity is locally deleted, and the options ask that we not return such entities
				|| !loadOptions.isReturnOfDeletedEntitiesEnabled()
					&& context.getEntry( entity ).getStatus().isDeletedOrGone() ) {
				result = null;
			}
			else {
				result = context.proxyFor( entity );
			}
			results.add( (E) result );
		}
		return results;
	}

	private <K> Object entityForNaturalId(PersistenceContext context, K naturalId) {
		final var descriptor = getEntityDescriptor();
		final Object id = context.getNaturalIdResolutions().findCachedIdByNaturalId( naturalId, descriptor );
		// id can be null if a non-existent natural id is requested, or a mutable natural id was changed and then deleted
		return id == null ? null : context.getEntity( new EntityKey( id, descriptor.getEntityPersister() ) );
	}

	private <K> Object[] checkPersistenceContextForCachedResults(
			K[] naturalIds,
			MultiNaturalIdLoadOptions loadOptions,
			SharedSessionContractImplementor session,
			LockOptions lockOptions,
			Consumer<E> results ) {
		final List<K> unresolvedIds = arrayList( naturalIds.length );
		final var context = session.getPersistenceContextInternal();
		final var naturalIdMapping = getEntityDescriptor().getNaturalIdMapping();
		for ( K naturalId : naturalIds ) {
			final Object entity = entityForNaturalId( context, naturalIdMapping.normalizeInput( naturalId ) );
			if ( entity != null ) {
				// Entity is already in the persistence context
				final var entry = context.getEntry( entity );
				if ( loadOptions.isReturnOfDeletedEntitiesEnabled()
						|| !entry.getStatus().isDeletedOrGone() ) {
					// either a managed entry, or a deleted one with returnDeleted enabled
					upgradeLock( entity, entry, lockOptions, session );
					final Object result = context.proxyFor( entity );
					results.accept( (E) result );
				}
			}
			else {
				// entity either doesn't exist or hasn't been loaded in the PC yet, in both cases we add
				// the natural id to the ids that still need to be recovered; in case the id corresponds
				// to a nonexistent instance, nothing will be in the results for it, which is OK in an
				// unordered multiload
				unresolvedIds.add( naturalId );
			}
		}
		return unresolvedIds.toArray();
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	protected final EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}
}
