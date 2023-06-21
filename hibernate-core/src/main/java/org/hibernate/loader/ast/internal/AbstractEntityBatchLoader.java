/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_DEBUG_ENABLED;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

public abstract class AbstractEntityBatchLoader<T>
		extends SingleIdEntityLoaderSupport<T>
		implements EntityBatchLoader<T> {

	private final SingleIdEntityLoaderStandardImpl<T> singleIdLoader;

	public AbstractEntityBatchLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		singleIdLoader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, sessionFactory );
	}

	protected abstract List<Object> initializeEntities(
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
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Batch fetching entity `%s#%s`", getLoadable().getEntityName(), id );
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
		if ( MULTI_KEY_LOAD_DEBUG_ENABLED ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Batch fetching entity `%s#%s`", getLoadable().getEntityName(), id );
		}

		final Object[] ids = resolveIdsToInitialize( id, session );
		final boolean hasSingleId = hasSingleId( ids );

		final T entity = load( id, ids, hasSingleId, entityInstance, lockOptions, null, session );;

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
		if ( hasSingleId ) {
			return singleIdLoader.load( id, entityInstance, lockOptions, readOnly, session );
		}

		final List<Object> entities = initializeEntities( ids, id, entityInstance, lockOptions, readOnly, session );
		final EntityPersister entityPersister = singleIdLoader.getLoadable().getEntityPersister();
		for ( Object entity : entities ) {
			if ( entityPersister.getIdentifierType().isEqual(
					id,
					session.getContextEntityIdentifier( entity ),
					sessionFactory
			) ) {
				return (T) entity;
			}
		}
		return null;
	}
}
