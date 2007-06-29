// $Id: BulkOperationCleanupAction.java 9897 2006-05-05 20:50:27Z max.andersen@jboss.com $
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Implementation of BulkOperationCleanupAction.
 *
 * @author Steve Ebersole
 */
public class BulkOperationCleanupAction implements Executable, Serializable {

	private final SessionImplementor session;

	private final Set affectedEntityNames = new HashSet();
	private final Set affectedCollectionRoles = new HashSet();
	private final Serializable[] spaces;

	public BulkOperationCleanupAction(SessionImplementor session, Queryable[] affectedQueryables) {
		this.session = session;
		// TODO : probably better to calculate these and pass them in, as it'll be more performant
		ArrayList tmpSpaces = new ArrayList();
		for ( int i = 0; i < affectedQueryables.length; i++ ) {
			if ( affectedQueryables[i].hasCache() ) {
				affectedEntityNames.add( affectedQueryables[i].getEntityName() );
			}
			Set roles = session.getFactory().getCollectionRolesByEntityParticipant( affectedQueryables[i].getEntityName() );
			if ( roles != null ) {
				affectedCollectionRoles.addAll( roles );
			}
			for ( int y = 0; y < affectedQueryables[i].getQuerySpaces().length; y++ ) {
				tmpSpaces.add( affectedQueryables[i].getQuerySpaces()[y] );
			}
		}
		this.spaces = new Serializable[ tmpSpaces.size() ];
		for ( int i = 0; i < tmpSpaces.size(); i++ ) {
			this.spaces[i] = ( Serializable ) tmpSpaces.get( i );
		}
	}
	
	/** Create an action that will evict collection and entity regions based on queryspaces (table names).
	 *  TODO: cache the autodetected information and pass it in instead.
	 **/
	public BulkOperationCleanupAction(SessionImplementor session, Set querySpaces) {
		this.session = session;

		Set tmpSpaces = new HashSet(querySpaces);
		SessionFactoryImplementor factory = session.getFactory();
		Iterator iterator = factory.getAllClassMetadata().entrySet().iterator();
		while ( iterator.hasNext() ) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String entityName = (String) entry.getKey();
			EntityPersister persister = factory.getEntityPersister( entityName );
			Serializable[] entitySpaces = persister.getQuerySpaces();

			if (affectedEntity( querySpaces, entitySpaces )) {
				if ( persister.hasCache() ) {
					affectedEntityNames.add( persister.getEntityName() );
				}
				Set roles = session.getFactory().getCollectionRolesByEntityParticipant( persister.getEntityName() );
				if ( roles != null ) {
					affectedCollectionRoles.addAll( roles );
				}
				for ( int y = 0; y < entitySpaces.length; y++ ) {
					tmpSpaces.add( entitySpaces[y] );
				}
			}

		}
		this.spaces = (Serializable[]) tmpSpaces.toArray( new Serializable[tmpSpaces.size()] );		
	}


	/** returns true if no queryspaces or if there are a match */
	private boolean affectedEntity(Set querySpaces, Serializable[] entitySpaces) {
		if(querySpaces==null || querySpaces.isEmpty()) {
			return true;
		}
		
		for ( int i = 0; i < entitySpaces.length; i++ ) {
			if ( querySpaces.contains( entitySpaces[i] ) ) {
				return true;
			}
		}
		return false;
	}

	public void init() {
		evictEntityRegions();
		evictCollectionRegions();
	}

	public boolean hasAfterTransactionCompletion() {
		return true;
	}

	public void afterTransactionCompletion(boolean success) throws HibernateException {
		evictEntityRegions();
		evictCollectionRegions();
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	public void beforeExecutions() throws HibernateException {
		// nothing to do
	}

	public void execute() throws HibernateException {
		// nothing to do		
	}

	private void evictEntityRegions() {
		if ( affectedEntityNames != null ) {
			Iterator itr = affectedEntityNames.iterator();
			while ( itr.hasNext() ) {
				final String entityName = ( String ) itr.next();
				session.getFactory().evictEntity( entityName );
			}
		}
	}

	private void evictCollectionRegions() {
		if ( affectedCollectionRoles != null ) {
			Iterator itr = affectedCollectionRoles.iterator();
			while ( itr.hasNext() ) {
				final String roleName = ( String ) itr.next();
				session.getFactory().evictCollection( roleName );
			}
		}
	}
}
