/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * An {@link org.hibernate.engine.spi.ActionQueue} {@link Executable} for ensuring
 * shared cache cleanup in relation to performed bulk HQL queries.
 * <p/>
 * NOTE: currently this executes for <tt>INSERT</tt> queries as well as
 * <tt>UPDATE</tt> and <tt>DELETE</tt> queries.  For <tt>INSERT</tt> it is
 * really not needed as we'd have no invalid entity/collection data to
 * cleanup (we'd still nee to invalidate the appropriate update-timestamps
 * regions) as a result of this query.
 *
 * @author Steve Ebersole
 */
public class BulkOperationCleanupAction implements Executable, Serializable {
	private final Serializable[] affectedTableSpaces;

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
		final SessionFactoryImplementor factory = session.getFactory();
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

			final Set<String> roles = factory.getMetamodel().getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( roles != null ) {
				for ( String role : roles ) {
					final CollectionPersister collectionPersister = factory.getMetamodel().collectionPersister( role );
					if ( collectionPersister.hasCache() ) {
						collectionCleanups.add(
								new CollectionCleanup(
										collectionPersister.getCacheAccessStrategy(),
										session
								)
						);
					}
				}
			}
		}

		this.affectedTableSpaces = spacesList.toArray( new String[ 0 ] );
	}

	/**
	 * Constructs an action to cleanup "affected cache regions" based on a
	 * set of affected table spaces.  This differs from {@link #BulkOperationCleanupAction(SharedSessionContractImplementor, EntityPersister[])}
	 * in that here we have the affected <strong>table names</strong>.  From those
	 * we deduce the entity persisters which are affected based on the defined
	 * {@link EntityPersister#getQuerySpaces() table spaces}; and from there, we
	 * determine the affected collection regions based on any collections
	 * in which those entity persisters participate as elements/keys/etc.
	 *
	 * @param session The session to which this request is tied.
	 * @param tableSpaces The table spaces.
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public BulkOperationCleanupAction(SharedSessionContractImplementor session, Set tableSpaces) {
		final LinkedHashSet<String> spacesList = new LinkedHashSet<>( tableSpaces );

		final SessionFactoryImplementor factory = session.getFactory();
		final MetamodelImplementor metamodel = factory.getMetamodel();
		for ( EntityPersister persister : metamodel.entityPersisters().values() ) {
			final String[] entitySpaces = (String[]) persister.getQuerySpaces();
			if ( affectedEntity( tableSpaces, entitySpaces ) ) {
				Collections.addAll( spacesList, entitySpaces );

				if ( persister.canWriteToCache() ) {
					entityCleanups.add( new EntityCleanup( persister.getCacheAccessStrategy(), session ) );
				}
				if ( persister.hasNaturalIdentifier() && persister.hasNaturalIdCache() ) {
					naturalIdCleanups.add( new NaturalIdCleanup( persister.getNaturalIdCacheAccessStrategy(), session ) );
				}

				final Set<String> roles = metamodel.getCollectionRolesByEntityParticipant( persister.getEntityName() );
				if ( roles != null ) {
					for ( String role : roles ) {
						final CollectionPersister collectionPersister = metamodel.collectionPersister( role );
						if ( collectionPersister.hasCache() ) {
							collectionCleanups.add(
									new CollectionCleanup( collectionPersister.getCacheAccessStrategy(), session )
							);
						}
					}
				}
			}
		}

		this.affectedTableSpaces = spacesList.toArray( new String[ 0 ] );
	}

	public static void schedule(ExecutionContext executionContext, SqmDmlStatement<?> statement) {
		final List<EntityPersister> entityPersisters = new ArrayList<>( 1 );
		final MetamodelImplementor metamodel = executionContext.getSession()
				.getFactory()
				.getMetamodel();
		if ( !( statement instanceof InsertStatement ) ) {
			entityPersisters.add( metamodel.entityPersister( statement.getTarget().getEntityName() ) );
		}
		for ( SqmCteStatement<?> cteStatement : statement.getCteStatements() ) {
			final SqmStatement<?> cteDefinition = cteStatement.getCteDefinition();
			if ( cteDefinition instanceof SqmDmlStatement<?> && !( cteDefinition instanceof InsertStatement ) ) {
				entityPersisters.add(
						metamodel.entityPersister( ( (SqmDmlStatement<?>) cteDefinition ).getTarget().getEntityName() )
				);
			}
		}

		schedule( executionContext, entityPersisters.toArray( new EntityPersister[0] ) );
	}

	public static void schedule(ExecutionContext executionContext, EntityPersister... affectedQueryables) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
		}
		else {
			action.getAfterTransactionCompletionProcess().doAfterTransactionCompletion( true, session );
		}
	}

	public static void schedule(ExecutionContext executionContext, Set<String> affectedQueryables) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final BulkOperationCleanupAction action = new BulkOperationCleanupAction( session, affectedQueryables );
		if ( session.isEventSource() ) {
			( (EventSource) session ).getActionQueue().addAction( action );
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
	public Serializable[] getPropertySpaces() {
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
	public void afterDeserialize(SharedSessionContractImplementor session) {
		// nop
	}
}
