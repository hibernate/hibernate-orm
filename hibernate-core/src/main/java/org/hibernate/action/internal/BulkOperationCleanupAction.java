/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.action.spi.Executable;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;

import static java.util.Collections.addAll;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

/**
 * An {@link org.hibernate.engine.spi.ActionQueue} {@link Executable} for
 * ensuring shared cache cleanup in relation to performed bulk HQL queries.
 *
 * @implNote Currently this executes for {@code INSERT} queries as well as
 * {@code UPDATE} and {@code DELETE} queries.  For {@code INSERT} it is
 * really not needed as we'd have no invalid entity/collection data to
 * clean up (we'd still need to invalidate the appropriate update-timestamps
 * regions) as a result of this query.
 *
 * @author Steve Ebersole
 */
public class BulkOperationCleanupAction implements Executable, Serializable {

	private final String[] affectedTableSpaces;

	private final Set<EntityCleanup> entityCleanups = new HashSet<>();
	private final Set<CollectionCleanup> collectionCleanups = new HashSet<>();
	private final Set<NaturalIdCleanup> naturalIdCleanups = new HashSet<>();

	/**
	 * Constructs an action to cleanup "affected cache regions" based on the
	 * affected entity persisters.  The affected regions are defined as the
	 * region (if any) of the entity persisters themselves, plus the
	 * collection regions for any collection in which those entity
	 * persisters participate as elements/keys/etc.
	 *
	 * @param session The session to which this request is tied.
	 * @param affectedQueryables The affected entity persisters.
	 */
	public BulkOperationCleanupAction(SharedSessionContractImplementor session, EntityPersister... affectedQueryables) {
		final LinkedHashSet<String> spacesList = new LinkedHashSet<>();
		for ( var persister : affectedQueryables ) {
			addAll( spacesList, (String[]) persister.getQuerySpaces() );

			if ( persister.canWriteToCache() ) {
				final var entityDataAccess = persister.getCacheAccessStrategy();
				if ( entityDataAccess != null ) {
					entityCleanups.add( new EntityCleanup( entityDataAccess, session ) );
				}
			}

			if ( persister.hasNaturalIdentifier() && persister.hasNaturalIdCache() ) {
				naturalIdCleanups.add( new NaturalIdCleanup( persister.getNaturalIdCacheAccessStrategy(), session ) );
			}

			final var mappingMetamodel = session.getFactory().getMappingMetamodel();
			final var roles = mappingMetamodel.getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( roles != null ) {
				for ( String role : roles ) {
					final var collectionPersister = mappingMetamodel.getCollectionDescriptor( role );
					if ( collectionPersister.hasCache() ) {
						collectionCleanups.add(
								new CollectionCleanup( collectionPersister.getCacheAccessStrategy(), session )
						);
					}
				}
			}
		}

		affectedTableSpaces = spacesList.toArray( EMPTY_STRING_ARRAY );
	}

	/**
	 * Constructs an action to cleanup "affected cache regions" based on a
	 * set of affected table spaces. This differs from
	 * {@link #BulkOperationCleanupAction(SharedSessionContractImplementor, EntityPersister[])}
	 * in that here we have the affected <strong>table names</strong>. From
	 * those we deduce the entity persisters which are affected based on the
	 * defined {@link EntityPersister#getQuerySpaces() table spaces}. Finally,
	 * we determine the affected collection regions based on any collections
	 * in which those entity persisters participate as elements/keys/etc.
	 *
	 * @param session The session to which this request is tied.
	 * @param tableSpaces The table spaces.
	 */
	public BulkOperationCleanupAction(SharedSessionContractImplementor session, Set<String> tableSpaces) {
		final LinkedHashSet<String> spacesList = new LinkedHashSet<>( tableSpaces );

		final var metamodel = session.getFactory().getMappingMetamodel();
		metamodel.forEachEntityDescriptor( entityDescriptor -> {
			final String[] entitySpaces = (String[]) entityDescriptor.getQuerySpaces();
			if ( affectedEntity( tableSpaces, entitySpaces ) ) {
				addAll( spacesList, entitySpaces );

				if ( entityDescriptor.canWriteToCache() ) {
					entityCleanups.add( new EntityCleanup( entityDescriptor.getCacheAccessStrategy(), session ) );
				}
				if ( entityDescriptor.hasNaturalIdentifier() && entityDescriptor.hasNaturalIdCache() ) {
					naturalIdCleanups.add( new NaturalIdCleanup( entityDescriptor.getNaturalIdCacheAccessStrategy(), session ) );
				}

				final var roles = metamodel.getCollectionRolesByEntityParticipant( entityDescriptor.getEntityName() );
				if ( roles != null ) {
					for ( String role : roles ) {
						final var collectionDescriptor = metamodel.getCollectionDescriptor( role );
						if ( collectionDescriptor.hasCache() ) {
							collectionCleanups.add(
									new CollectionCleanup( collectionDescriptor.getCacheAccessStrategy(), session )
							);
						}
					}
				}
			}
		} );

		affectedTableSpaces = spacesList.toArray( EMPTY_STRING_ARRAY );
	}

	public static void schedule(SharedSessionContractImplementor session, SqmDmlStatement<?> statement) {
		final List<EntityPersister> entityPersisters = new ArrayList<>( 1 );
		final var metamodel = session.getFactory().getMappingMetamodel();
		if ( !( statement instanceof InsertSelectStatement ) ) {
			entityPersisters.add( metamodel.getEntityDescriptor( statement.getTarget().getEntityName() ) );
		}
		for ( var cteStatement : statement.getCteStatements() ) {
			if ( cteStatement.getCteDefinition() instanceof SqmDmlStatement<?> dmlStatement ) {
				entityPersisters.add( metamodel.getEntityDescriptor( dmlStatement.getTarget().getEntityName() ) );
			}
		}

		schedule( session, entityPersisters.toArray( new EntityPersister[0] ) );
	}

	public static void schedule(SharedSessionContractImplementor session, EntityPersister... affectedQueryables) {
		final var action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() ) {
			session.asEventSource().getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}

	public static void schedule(SharedSessionContractImplementor session, Set<String> affectedQueryables) {
		final var action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() ) {
			session.asEventSource().getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}


	/**
	 * Check whether we should consider an entity as affected by the query.  This
	 * defines inclusion of the entity in the clean-up.
	 *
	 * @param affectedTableSpaces The table spaces reported to be affected by
	 * the query.
	 * @param checkTableSpaces The table spaces (from the entity persister)
	 * to check against the affected table spaces.
	 *
	 * @return Whether the entity should be considered affected
	 *
	 * @implNote An entity is considered to be affected if either (1) the affected table
	 * spaces are not known or (2) any of the incoming check table spaces occur
	 * in that set.
	 */
	private boolean affectedEntity(Set<?> affectedTableSpaces, Serializable[] checkTableSpaces) {
		if ( affectedTableSpaces == null || affectedTableSpaces.isEmpty() ) {
			return true;
		}
		else {
			for ( Serializable checkTableSpace : checkTableSpaces ) {
				if ( affectedTableSpaces.contains( checkTableSpace ) ) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public String[] getPropertySpaces() {
		return affectedTableSpaces;
	}

	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return (success, session) -> {
			for ( var cleanup : entityCleanups ) {
				cleanup.release();
			}
			entityCleanups.clear();

			for ( var cleanup : naturalIdCleanups ) {
				cleanup.release();
			}
			naturalIdCleanups.clear();

			for ( var cleanup : collectionCleanups ) {
				cleanup.release();
			}
			collectionCleanups.clear();
		};
	}

	@Override
	public void beforeExecutions() throws HibernateException {
		// nothing to do
	}

	@Override
	public void execute() throws HibernateException {
		// nothing to do
	}

	private static class EntityCleanup implements Serializable {
		private final EntityDataAccess cacheAccess;
		private final SoftLock cacheLock;

		private EntityCleanup(
				EntityDataAccess cacheAccess,
				SharedSessionContractImplementor session) {
			this.cacheAccess = cacheAccess;
			this.cacheLock = cacheAccess.lockRegion();
			cacheAccess.removeAll( session );
		}

		private void release() {
			cacheAccess.unlockRegion( cacheLock );
		}
	}

	private static class CollectionCleanup implements Serializable {
		private final CollectionDataAccess cacheAccess;
		private final SoftLock cacheLock;

		private CollectionCleanup(
				CollectionDataAccess cacheAccess,
				SharedSessionContractImplementor session) {
			this.cacheAccess = cacheAccess;
			this.cacheLock = cacheAccess.lockRegion();
			cacheAccess.removeAll( session );
		}

		private void release() {
			cacheAccess.unlockRegion( cacheLock );
		}
	}

	private static class NaturalIdCleanup implements Serializable {
		private final NaturalIdDataAccess naturalIdCacheAccessStrategy;
		private final SoftLock cacheLock;

		public NaturalIdCleanup(
				NaturalIdDataAccess naturalIdCacheAccessStrategy,
				SharedSessionContractImplementor session) {
			this.naturalIdCacheAccessStrategy = naturalIdCacheAccessStrategy;
			this.cacheLock = naturalIdCacheAccessStrategy.lockRegion();
			naturalIdCacheAccessStrategy.removeAll( session );
		}

		private void release() {
			naturalIdCacheAccessStrategy.unlockRegion( cacheLock );
		}
	}

	@Override
	public void afterDeserialize(EventSource session) {
		// nop
	}
}
