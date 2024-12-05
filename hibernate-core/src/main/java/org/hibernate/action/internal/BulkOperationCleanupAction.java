/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;

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
		for ( EntityPersister persister : affectedQueryables ) {
			Collections.addAll( spacesList, (String[]) persister.getQuerySpaces() );

			if ( persister.canWriteToCache() ) {
				final EntityDataAccess entityDataAccess = persister.getCacheAccessStrategy();
				if ( entityDataAccess != null ) {
					entityCleanups.add( new EntityCleanup( entityDataAccess, session ) );
				}
			}

			if ( persister.hasNaturalIdentifier() && persister.hasNaturalIdCache() ) {
				naturalIdCleanups.add(
						new NaturalIdCleanup( persister.getNaturalIdCacheAccessStrategy(), session )
				);
			}

			final MappingMetamodelImplementor mappingMetamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
			final Set<String> roles = mappingMetamodel.getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( roles != null ) {
				for ( String role : roles ) {
					final CollectionPersister collectionPersister = mappingMetamodel.getCollectionDescriptor( role );
					if ( collectionPersister.hasCache() ) {
						collectionCleanups.add(
								new CollectionCleanup( collectionPersister.getCacheAccessStrategy(), session )
						);
					}
				}
			}
		}

		this.affectedTableSpaces = spacesList.toArray( new String[ 0 ] );
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

		final MappingMetamodelImplementor metamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
		metamodel.forEachEntityDescriptor( (entityDescriptor) -> {
			final String[] entitySpaces = (String[]) entityDescriptor.getQuerySpaces();
			if ( affectedEntity( tableSpaces, entitySpaces ) ) {
				Collections.addAll( spacesList, entitySpaces );

				if ( entityDescriptor.canWriteToCache() ) {
					entityCleanups.add( new EntityCleanup( entityDescriptor.getCacheAccessStrategy(), session ) );
				}
				if ( entityDescriptor.hasNaturalIdentifier() && entityDescriptor.hasNaturalIdCache() ) {
					naturalIdCleanups.add( new NaturalIdCleanup( entityDescriptor.getNaturalIdCacheAccessStrategy(), session ) );
				}

				final Set<String> roles = metamodel.getCollectionRolesByEntityParticipant( entityDescriptor.getEntityName() );
				if ( roles != null ) {
					for ( String role : roles ) {
						final CollectionPersister collectionDescriptor = metamodel.getCollectionDescriptor( role );
						if ( collectionDescriptor.hasCache() ) {
							collectionCleanups.add(
									new CollectionCleanup( collectionDescriptor.getCacheAccessStrategy(), session )
							);
						}
					}
				}
			}
		} );

		this.affectedTableSpaces = spacesList.toArray( new String[ 0 ] );
	}

	public static void schedule(SharedSessionContractImplementor session, SqmDmlStatement<?> statement) {
		final List<EntityPersister> entityPersisters = new ArrayList<>( 1 );
		final MappingMetamodelImplementor metamodel = session.getFactory().getRuntimeMetamodels().getMappingMetamodel();
		if ( !( statement instanceof InsertSelectStatement ) ) {
			entityPersisters.add( metamodel.getEntityDescriptor( statement.getTarget().getEntityName() ) );
		}
		for ( SqmCteStatement<?> cteStatement : statement.getCteStatements() ) {
			final SqmQuery<?> cteDefinition = cteStatement.getCteDefinition();
			if ( cteDefinition instanceof SqmDmlStatement<?> ) {
				entityPersisters.add(
						metamodel.getEntityDescriptor( ( (SqmDmlStatement<?>) cteDefinition ).getTarget().getEntityName() )
				);
			}
		}

		schedule( session, entityPersisters.toArray( new EntityPersister[0] ) );
	}

	public static void schedule(SharedSessionContractImplementor session, EntityPersister... affectedQueryables) {
		final BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() && session.isTransactionInProgress() ) {
			session.asEventSource().getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}

	public static void schedule(SharedSessionContractImplementor session, Set<String> affectedQueryables) {
		final BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() && session.isTransactionInProgress() ) {
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

		for ( Serializable checkTableSpace : checkTableSpaces ) {
			if ( affectedTableSpaces.contains( checkTableSpace ) ) {
				return true;
			}
		}
		return false;
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
			for ( EntityCleanup cleanup : entityCleanups ) {
				cleanup.release();
			}
			entityCleanups.clear();

			for ( NaturalIdCleanup cleanup : naturalIdCleanups ) {
				cleanup.release();
			}
			naturalIdCleanups.clear();

			for ( CollectionCleanup cleanup : collectionCleanups ) {
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
