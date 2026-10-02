package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;

public abstract class AbstractEntityBatchLoader<T>
		extends SingleIdEntityLoaderSupport<T>
		implements EntityBatchLoader<T> {

	private final SingleIdEntityLoaderStandardImpl<T> singleIdLoader;

	public AbstractEntityBatchLoader(@Nonnull EntityMappingType entityDescriptor, @Nonnull LoadQueryInfluencers influencers) {
		super( entityDescriptor, influencers.getSessionFactory() );
		singleIdLoader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, influencers );
	}

	protected abstract void initializeEntities(
			@Nonnull Object[] idsToInitialize,
			@Nonnull Object pkValue,
			@Nullable Object entityInstance,
			@Nonnull LockOptions lockOptions,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session);

	@Nonnull
	protected abstract Object[] resolveIdsToInitialize(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session);

@Nullable
@Override
	public final T load(
			@Nonnull Object id,
			@Nullable Object entityInstance,
			@Nonnull LockOptions lockOptions,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.batchFetchingEntity( infoString( getLoadable(), id ) );
		}

		final var ids = resolveIdsToInitialize( id, session );
		return load( id, ids, hasSingleId( ids ), entityInstance, lockOptions, readOnly, session );
	}

@Nullable
@Override
	public T load(
			@Nonnull Object id,
			@Nullable Object entityInstance,
			@Nonnull LockOptions lockOptions,
			@Nonnull SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.batchFetchingEntity( infoString( getLoadable(), id ) );
		}

		final Object[] ids = resolveIdsToInitialize( id, session );
		final boolean hasSingleId = hasSingleId( ids );
		final T entity = load( id, ids, hasSingleId, entityInstance, lockOptions, null, session );
		if ( hasSingleId ) {
			return entity;
		}
		else if ( Hibernate.isInitialized( entity ) ) {
			return entity;
		}
		else {
			return null;
		}
	}

	@Nullable
	private T load(
			@Nonnull Object id,
			@Nonnull Object[] ids,
			boolean hasSingleId,
			@Nullable Object entityInstance,
			@Nonnull LockOptions lockOptions,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session) {
		// We disable batching if lockMode != NONE
		if ( hasSingleId || lockOptions.getLockMode() != LockMode.NONE ) {
			return singleIdLoader.load( id, entityInstance, lockOptions, readOnly, session );
		}
		else {
			initializeEntities( ids, id, entityInstance, lockOptions, readOnly, session );
			final var entityKey = session.generateEntityKey( id, getLoadable().getEntityPersister() );
			//noinspection unchecked
			return (T) session.getPersistenceContext().getEntity( entityKey );
		}
	}
}
