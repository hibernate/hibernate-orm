/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * An {@link org.hibernate.engine.ActionQueue} {@link Executable} for ensuring
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

	private final Set entityCleanups = new HashSet();
	private final Set collectionCleanups = new HashSet();

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
	public BulkOperationCleanupAction(SessionImplementor session, Queryable[] affectedQueryables) {
		SessionFactoryImplementor factory = session.getFactory();
		ArrayList tmpSpaces = new ArrayList();
		for ( int i = 0; i < affectedQueryables.length; i++ ) {
			tmpSpaces.addAll( Arrays.asList( affectedQueryables[i].getQuerySpaces() ) );
			if ( affectedQueryables[i].hasCache() ) {
				entityCleanups.add( new EntityCleanup( affectedQueryables[i].getCacheAccessStrategy() ) );
			}
			Set roles = factory.getCollectionRolesByEntityParticipant( affectedQueryables[i].getEntityName() );
			if ( roles != null ) {
				Iterator itr = roles.iterator();
				while ( itr.hasNext() ) {
					String role = ( String ) itr.next();
					CollectionPersister collectionPersister = factory.getCollectionPersister( role );
					if ( collectionPersister.hasCache() ) {
						collectionCleanups.add( new CollectionCleanup( collectionPersister.getCacheAccessStrategy() ) );
					}
				}
			}
		}

		this.affectedTableSpaces = ( Serializable[] ) tmpSpaces.toArray( new Serializable[ tmpSpaces.size() ] );
	}

	/**
	 * Constructs an action to cleanup "affected cache regions" based on a
	 * set of affected table spaces.  This differs from {@link #BulkOperationCleanupAction(SessionImplementor, Queryable[])}
	 * in that here we have the affected <strong>table names</strong>.  From those
	 * we deduce the entity persisters whcih are affected based on the defined
	 * {@link EntityPersister#getQuerySpaces() table spaces}; and from there, we
	 * determine the affected collection regions based on any collections
	 * in which those entity persisters participate as elements/keys/etc.
	 *
	 * @param session The session to which this request is tied.
	 * @param tableSpaces The table spaces.
	 */
	public BulkOperationCleanupAction(SessionImplementor session, Set tableSpaces) {
		Set tmpSpaces = new HashSet(tableSpaces);
		SessionFactoryImplementor factory = session.getFactory();
		Iterator iterator = factory.getAllClassMetadata().entrySet().iterator();
		while ( iterator.hasNext() ) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String entityName = (String) entry.getKey();
			EntityPersister persister = factory.getEntityPersister( entityName );
			Serializable[] entitySpaces = persister.getQuerySpaces();

			if ( affectedEntity( tableSpaces, entitySpaces ) ) {
				tmpSpaces.addAll( Arrays.asList( entitySpaces ) );
				if ( persister.hasCache() ) {
					entityCleanups.add( new EntityCleanup( persister.getCacheAccessStrategy() ) );
				}
				Set roles = session.getFactory().getCollectionRolesByEntityParticipant( persister.getEntityName() );
				if ( roles != null ) {
					Iterator itr = roles.iterator();
					while ( itr.hasNext() ) {
						String role = ( String ) itr.next();
						CollectionPersister collectionPersister = factory.getCollectionPersister( role );
						if ( collectionPersister.hasCache() ) {
							collectionCleanups.add(
									new CollectionCleanup( collectionPersister.getCacheAccessStrategy() )
							);
						}
					}
				}
			}
		}

		this.affectedTableSpaces = ( Serializable[] ) tmpSpaces.toArray( new Serializable[ tmpSpaces.size() ] );
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

		for ( int i = 0; i < checkTableSpaces.length; i++ ) {
			if ( affectedTableSpaces.contains( checkTableSpaces[i] ) ) {
				return true;
			}
		}
		return false;
	}

	public Serializable[] getPropertySpaces() {
		return affectedTableSpaces;
	}

	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return new AfterTransactionCompletionProcess() {
			public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
				Iterator itr = entityCleanups.iterator();
				while ( itr.hasNext() ) {
					final EntityCleanup cleanup = ( EntityCleanup ) itr.next();
					cleanup.release();
				}

				itr = collectionCleanups.iterator();
				while ( itr.hasNext() ) {
					final CollectionCleanup cleanup = ( CollectionCleanup ) itr.next();
					cleanup.release();
				}
			}
		};
	}

	public void beforeExecutions() throws HibernateException {
		// nothing to do
	}

	public void execute() throws HibernateException {
		// nothing to do		
	}

	private static class EntityCleanup {
		private final EntityRegionAccessStrategy cacheAccess;
		private final SoftLock cacheLock;

		private EntityCleanup(EntityRegionAccessStrategy cacheAccess) {
			this.cacheAccess = cacheAccess;
			this.cacheLock = cacheAccess.lockRegion();
			cacheAccess.removeAll();
		}

		private void release() {
			cacheAccess.unlockRegion( cacheLock );
		}
	}

	private static class CollectionCleanup {
		private final CollectionRegionAccessStrategy cacheAccess;
		private final SoftLock cacheLock;

		private CollectionCleanup(CollectionRegionAccessStrategy cacheAccess) {
			this.cacheAccess = cacheAccess;
			this.cacheLock = cacheAccess.lockRegion();
			cacheAccess.removeAll();
		}

		private void release() {
			cacheAccess.unlockRegion( cacheLock );
		}
	}
}
