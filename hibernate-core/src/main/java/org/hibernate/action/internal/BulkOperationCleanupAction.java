/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;
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
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * An {@link org.hibernate.engine.spi.ActionQueue} {@link org.hibernate.action.spi.Executable} for ensuring
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
	private final Set<String> affectedTableSpaces;

	private final Set<EntityCleanup> entityCleanups = new HashSet<>();
	private final Set<CollectionCleanup> collectionCleanups = new HashSet<>();
	private final Set<NaturalIdCleanup> naturalIdCleanups = new HashSet<>();

	/**
	 * Constructs an action to cleanup "affected cache regions" based on the
	 * affected entity persisters.  The affected regions are defined as the
	 * region (if any) of the entity persisters themselves, plus the
	 * collection regions for any collection in which those entity
	 * persisters participate as elements/keys/etc.
	 */
	public BulkOperationCleanupAction(SharedSessionContractImplementor session, List<EntityTypeDescriptor> affectedEntities) {
		final SessionFactoryImplementor factory = session.getFactory();
		final LinkedHashSet<String> spacesList = new LinkedHashSet<>();
		for ( EntityTypeDescriptor entityDescriptor : affectedEntities ) {
			spacesList.addAll( entityDescriptor.getAffectedTableNames() );

			final EntityTypeDescriptor rootEntityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();
			spacesList.addAll(  rootEntityDescriptor.getAffectedTableNames() );

			if ( entityDescriptor.canWriteToCache() ) {
				final EntityDataAccess entityDataAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
				if ( entityDataAccess != null ) {
					entityCleanups.add( new EntityCleanup( entityDataAccess, session ) );
				}
			}

			if ( entityDescriptor.hasNaturalIdentifier() && entityDescriptor.getHierarchy().getNaturalIdDescriptor().getCacheAccess() != null ) {
				naturalIdCleanups.add(
						new NaturalIdCleanup( entityDescriptor.getHierarchy().getNaturalIdDescriptor().getCacheAccess(), session )
				);
			}

			for ( PersistentCollectionDescriptor<?, ?, ?> collectionDescriptor : factory.getMetamodel()
					.findCollectionsByEntityParticipant( entityDescriptor ) ) {
				if ( collectionDescriptor.hasCache() ) {
					collectionCleanups.add(
							new CollectionCleanup(
									collectionDescriptor.getCacheAccess(),
									session
							)
					);
				}
			}
		}

		this.affectedTableSpaces = new HashSet<>( spacesList );
	}

	/**
	 * Constructs an action to cleanup "affected cache regions" based on a
	 * set of affected table spaces.  This differs from
	 * {@link BulkOperationCleanupAction#BulkOperationCleanupAction(org.hibernate.engine.spi.SharedSessionContractImplementor, java.util.List)}
	 * in that here we have the affected <strong>table names</strong>.  From those
	 * we deduce the entity persisters which are affected based on the defined
	 * {@link EntityTypeDescriptor#getAffectedTableNames()}; and from there, we
	 * determine the affected collection regions based on any collections
	 * in which those entity persisters participate as elements/keys/etc.
	 *
	 * @param session The session to which this request is tied.
	 * @param tableSpaces The table spaces.
	 */
	@SuppressWarnings({ "unchecked" })
	public BulkOperationCleanupAction(SharedSessionContractImplementor session, Set tableSpaces) {
		final LinkedHashSet<String> spacesList = new LinkedHashSet<>();
		spacesList.addAll( tableSpaces );

		final SessionFactoryImplementor factory = session.getFactory();

		factory.getMetamodel().visitEntityHierarchies(
				entityHierarchy -> {
					final EntityTypeDescriptor rootEntityDescriptor = entityHierarchy.getRootEntityType();
					final Set<String> affectedTableNames = rootEntityDescriptor.getAffectedTableNames();
					if ( affectedEntity( tableSpaces, affectedTableNames ) ) {
						spacesList.addAll( affectedTableNames );

						if ( rootEntityDescriptor.canWriteToCache() ) {
							entityCleanups.add( new EntityCleanup( entityHierarchy.getEntityCacheAccess(), session ) );
						}
						if ( rootEntityDescriptor.hasNaturalIdentifier() && entityHierarchy.getNaturalIdDescriptor().getCacheAccess() != null ) {
							naturalIdCleanups.add( new NaturalIdCleanup( entityHierarchy.getNaturalIdDescriptor().getCacheAccess(), session ) );
						}

						for ( PersistentCollectionDescriptor<?, ?, ?> collectionDescriptor : session.getFactory()
								.getMetamodel()
								.findCollectionsByEntityParticipant( rootEntityDescriptor ) ) {
							if ( collectionDescriptor.hasCache() ) {
								collectionCleanups.add(
										new CollectionCleanup( collectionDescriptor.getCacheAccess(), session )
								);
							}
						}
					}
				}
		);

		this.affectedTableSpaces = new HashSet<>( spacesList );
	}


	/**
	 * Check to determine whether the table spaces reported by an entity
	 * persister match against the defined affected table spaces.
	 *
	 * @param affectedTableSpaces The table spaces reported to be affected by
	 * the query.
	 * @param checkTableSpaces The table spaces (from the entity persister)
	 * to check against the affected table spaces.
	 *
	 * @return True if there are affected table spaces and any of the incoming
	 * check table spaces occur in that set.
	 */
	private boolean affectedEntity(Set affectedTableSpaces, Serializable[] checkTableSpaces) {
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

	private boolean affectedEntity(Set affectedTableSpaces, Set checkTableSpaces) {
		if ( affectedTableSpaces == null || affectedTableSpaces.isEmpty() ) {
			return true;
		}

		for ( Object checkTableSpace : checkTableSpaces ) {
			if ( affectedTableSpaces.contains( checkTableSpace ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<String> getPropertySpaces() {
		return affectedTableSpaces;
	}

	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return new AfterTransactionCompletionProcess() {
			@Override
			public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
				for ( EntityCleanup cleanup : entityCleanups ) {
					cleanup.release();
				}
				entityCleanups.clear();

				for ( NaturalIdCleanup cleanup : naturalIdCleanups ) {
					cleanup.release();

				}
				entityCleanups.clear();

				for ( CollectionCleanup cleanup : collectionCleanups ) {
					cleanup.release();
				}
				collectionCleanups.clear();
			}
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

		private EntityCleanup(EntityDataAccess cacheAccess,
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

		private CollectionCleanup(CollectionDataAccess cacheAccess,
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

		public NaturalIdCleanup(NaturalIdDataAccess naturalIdCacheAccessStrategy,
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
