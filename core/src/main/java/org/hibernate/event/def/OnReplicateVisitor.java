//$Id: OnReplicateVisitor.java 10948 2006-12-07 21:53:10Z steve.ebersole@jboss.com $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When an entity is passed to replicate(), and there is an existing row, we must
 * inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 * existing snapshot
 * 3. execute a collection removal (SQL DELETE) for each null collection property
 * or "new" collection
 *
 * @author Gavin King
 */
public class OnReplicateVisitor extends ReattachVisitor {

	private boolean isUpdate;

	OnReplicateVisitor(EventSource session, Serializable key, Object owner, boolean isUpdate) {
		super( session, key, owner );
		this.isUpdate = isUpdate;
	}

	Object processCollection(Object collection, CollectionType type)
			throws HibernateException {

		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}

		EventSource session = getSession();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		if ( isUpdate ) {
			removeCollection( persister, extractCollectionKeyFromOwner( persister ), session );
		}
		if ( collection != null && ( collection instanceof PersistentCollection ) ) {
			PersistentCollection wrapper = ( PersistentCollection ) collection;
			wrapper.setCurrentSession( session );
			if ( wrapper.wasInitialized() ) {
				session.getPersistenceContext().addNewCollection( persister, wrapper );
			}
			else {
				reattachCollection( wrapper, type );
			}
		}
		else {
			// otherwise a null or brand new collection
			// this will also (inefficiently) handle arrays, which
			// have no snapshot, so we can't do any better
			//processArrayOrNewCollection(collection, type);
		}

		return null;

	}

}
