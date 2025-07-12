/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

public abstract class AbstractEntityBatchLoader<T>
		extends SingleIdEntityLoaderSupport<T>
		implements EntityBatchLoader<T> {

	private final SingleIdEntityLoaderStandardImpl<T> singleIdLoader;

	public AbstractEntityBatchLoader(EntityMappingType entityDescriptor, LoadQueryInfluencers influencers) {
		super( entityDescriptor, influencers.getSessionFactory() );
		this.singleIdLoader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, influencers );
	}

	protected abstract void initializeEntities(
			Object[] idsToInitialize,
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session);

	protected abstract Object[] resolveIdsToInitialize(Object id, SharedSessionContractImplementor session);

	@Override
	public final T load(
			Object id,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "Batch fetching entity `%s#%s`", getLoadable().getEntityName(), id );
		}

		final Object[] ids = resolveIdsToInitialize( id, session );
		return load( id, ids, hasSingleId( ids ), entityInstance, lockOptions, readOnly, session );
	}

	@Override
	public T load(
			Object id,
			Object entityInstance,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "Batch fetching entity `%s#%s`", getLoadable().getEntityName(), id );
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

	private T load(
			Object id,
			Object[] ids,
			boolean hasSingleId,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		// We disable batching if lockMode != NONE
		if ( hasSingleId || lockOptions.getLockMode() != LockMode.NONE ) {
			return singleIdLoader.load( id, entityInstance, lockOptions, readOnly, session );
		}

		initializeEntities( ids, id, entityInstance, lockOptions, readOnly, session );

		final EntityKey entityKey = session.generateEntityKey( id, getLoadable().getEntityPersister() );
		//noinspection unchecked
		return (T) session.getPersistenceContext().getEntity( entityKey );
	}
}
