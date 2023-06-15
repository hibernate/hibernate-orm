/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.util.Arrays;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

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
		if ( lockOptions.getLockMode().greaterThan( LockMode.OPTIMISTIC_FORCE_INCREMENT ) ) {
			// we want to apply the lock only to the first entity, we need to generate 2 different queries
			ids = trimIds( 1, ids  );
			initializeEntities( ids, ids[0], entityInstance, new LockOptions( LockMode.NONE ), readOnly, session );
			return singleIdLoader.load( id, entityInstance, lockOptions, readOnly, session );
		}
		else {
			// if LockMode is different from NONE but less than OPTIMISTIC_FORCE_INCREMENT we still want to apply it only to the first entity but we can generate 1 query and then
			// do an upgrade lock
			ids = trimIds( 0, ids );
			initializeEntities( ids, id, entityInstance, new LockOptions( LockMode.NONE ), readOnly, session );

			final EntityKey entityKey = session.generateEntityKey( id, getLoadable().getEntityPersister() );
			final Object entity =  session.getPersistenceContext().getEntity( entityKey );
			if ( lockOptions.getLockMode() != LockMode.NONE ) {
				LoaderHelper.upgradeLock(
						entity,
						session.getPersistenceContext().getEntry( entity ),
						lockOptions,
						session.asEventSource()
				);
			}

			return (T) entity;
		}
	}

	protected abstract Object[] trimIds(int startPosition, Object[] keysToInitialize);
}
