/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectDeletedException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class LoaderHelper {

	/**
	 * Ensure the {@linkplain LockMode} associated with the entity in relation to a
	 * persistence context is {@linkplain LockMode#greaterThan great or equal} to the
	 * requested mode, performing a pessimistic lock upgrade on a given entity, if needed.
	 *
	 * @param object The entity for which to upgrade the lock.
	 * @param entry The entity's {@link EntityEntry} instance.
	 * @param lockOptions Contains the requested lock mode.
	 * @param session The session which is the source of the event being processed.
	 */
	public static void upgradeLock(
			Object object, EntityEntry entry, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final LockMode requestedLockMode = lockOptions.getLockMode();
		if ( requestedLockMode.greaterThan( entry.getLockMode() ) ) {
			// Request is for a more restrictive lock than the lock already held
			final EntityPersister persister = entry.getPersister();

			if ( entry.getStatus().isDeletedOrGone()) {
				throw new ObjectDeletedException(
						"attempted to lock a deleted instance",
						entry.getId(),
						persister.getEntityName()
				);
			}

			if ( LoaderLogging.LOADER_LOGGER.isTraceEnabled() ) {
				LoaderLogging.LOADER_LOGGER.tracef(
						"Locking `%s( %s )` in `%s` lock-mode",
						persister.getEntityName(),
						entry.getId(),
						requestedLockMode
				);
			}

			final boolean cachingEnabled = persister.canWriteToCache();
			SoftLock lock = null;
			Object ck = null;
			try {
				if ( cachingEnabled ) {
					final EntityDataAccess cache = persister.getCacheAccessStrategy();
					ck = cache.generateCacheKey( entry.getId(), persister, session.getFactory(), session.getTenantIdentifier() );
					lock = cache.lockItem( session, ck, entry.getVersion() );
				}

				if ( persister.isVersioned() && entry.getVersion() == null ) {
					// This should be an empty entry created for an uninitialized bytecode proxy
					if ( !Hibernate.isPropertyInitialized( object, persister.getVersionMapping().getPartName() ) ) {
						Hibernate.initialize( object );
						entry = session.getPersistenceContextInternal().getEntry( object );
						assert entry.getVersion() != null;
					}
					else {
						throw new IllegalStateException( String.format(
								"Trying to lock versioned entity %s but found null version",
								MessageHelper.infoString( persister.getEntityName(), entry.getId() )
						) );
					}
				}

				if ( persister.isVersioned() && requestedLockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT  ) {
					// todo : should we check the current isolation mode explicitly?
					OptimisticLockHelper.forceVersionIncrement( object, entry, session );
				}
				else if ( entry.isExistsInDatabase() ) {
					final EventMonitor eventMonitor = session.getEventMonitor();
					final DiagnosticEvent entityLockEvent = eventMonitor.beginEntityLockEvent();
					boolean success = false;
					try {
						persister.lock( entry.getId(), entry.getVersion(), object, lockOptions, session );
						success = true;
					}
					finally {
						eventMonitor.completeEntityLockEvent( entityLockEvent, entry.getId(),
								persister.getEntityName(), lockOptions.getLockMode(), success, session );
					}
				}
				else {
					// should only be possible for a stateful session
					if ( session instanceof EventSource eventSource ) {
						eventSource.forceFlush( entry );
					}
				}
				entry.setLockMode(requestedLockMode);
			}
			finally {
				// the database now holds a lock + the object is flushed from the cache,
				// so release the soft lock
				if ( cachingEnabled ) {
					persister.getCacheAccessStrategy().unlockItem( session, ck, lock );
				}
			}
		}
	}

	/**
	 * Determine if the influencers associated with the given Session indicate read-only
	 */
	public static Boolean getReadOnlyFromLoadQueryInfluencers(SharedSessionContractImplementor session) {
		return getReadOnlyFromLoadQueryInfluencers( session.getLoadQueryInfluencers() );
	}

	/**
	 * Determine if given influencers indicate read-only
	 */
	public static Boolean getReadOnlyFromLoadQueryInfluencers(LoadQueryInfluencers influencers) {
		return influencers == null ? null : influencers.getReadOnly();
	}

	/**
	 * Normalize an array of keys (primary, foreign or natural).
	 * <p/>
	 * If the array is already typed as the key type, {@code keys} is simply returned.
	 * <p/>
	 * Otherwise, a new typed array is created and the contents copied from {@code keys} to this new array.  If
	 * key {@linkplain org.hibernate.cfg.AvailableSettings#JPA_LOAD_BY_ID_COMPLIANCE coercion} is enabled, the
	 * values will be coerced to the key type.
	 *
	 * @param keys The keys to normalize
	 * @param keyPart The ModelPart describing the key
	 *
	 * @param <K> The key type
	 */
	public static <K> K[] normalizeKeys(
			K[] keys,
			BasicValuedModelPart keyPart,
			SharedSessionContractImplementor session,
			SessionFactoryImplementor sessionFactory) {
		assert keys.getClass().isArray();

		//noinspection unchecked
		final JavaType<K> keyJavaType = (JavaType<K>) keyPart.getJavaType();
		final Class<K> keyClass = keyJavaType.getJavaTypeClass();

		if ( keys.getClass().getComponentType().equals( keyClass ) ) {
			return keys;
		}

		final K[] typedArray = createTypedArray( keyClass, keys.length );
		final boolean coerce = !sessionFactory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
		if ( !coerce ) {
			System.arraycopy( keys, 0, typedArray, 0, keys.length );
		}
		else {
			for ( int i = 0; i < keys.length; i++ ) {
				typedArray[i] = keyJavaType.coerce( keys[i], session );
			}
		}
		return typedArray;
	}

	/**
	 * Creates a typed array, as opposed to a generic {@code Object[]} that holds the typed values
	 *
	 * @param elementClass The type of the array elements.  See {@link Class#getComponentType()}
	 * @param length The length to which the array should be created.  This is usually zero for Hibernate uses
	 */
	@AllowReflection
	public static <X> X[] createTypedArray(Class<X> elementClass, @SuppressWarnings("SameParameterValue") int length) {
		//noinspection unchecked
		return (X[]) Array.newInstance( elementClass, length );
	}

	/**
	 * Load one or more instances of a model part (an entity or collection)
	 * based on a SQL ARRAY parameter to specify the keys (as opposed to the
	 * more traditional SQL IN predicate approach).
	 *
	 * @param <R> The type of the model part to load
	 * @param <K> The type of the keys
	 */
	public static <R,K> List<R> loadByArrayParameter(
			K[] idsToInitialize,
			SelectStatement sqlAst,
			JdbcOperationQuerySelect jdbcOperation,
			JdbcParameter jdbcParameter,
			JdbcMapping arrayJdbcMapping,
			Object entityId,
			Object entityInstance,
			EntityMappingType rootEntityDescriptor,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		assert jdbcOperation != null;
		assert jdbcParameter != null;

		final JdbcParameterBindings bindings = new JdbcParameterBindingsImpl( 1);
		bindings.addBinding( jdbcParameter, new JdbcParameterBindingImpl( arrayJdbcMapping, idsToInitialize ) );
		return session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcOperation,
				bindings,
				new SingleIdExecutionContext(
						entityId,
						entityInstance,
						rootEntityDescriptor,
						readOnly,
						lockOptions,
						SubselectFetch.createRegistrationHandler(
								session.getPersistenceContext().getBatchFetchQueue(),
								sqlAst,
								JdbcParametersList.singleton( jdbcParameter ),
								bindings
						),
						session
				),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				idsToInitialize.length
		);
	}
}
