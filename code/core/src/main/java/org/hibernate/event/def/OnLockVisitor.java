//$Id: OnLockVisitor.java 10948 2006-12-07 21:53:10Z steve.ebersole@jboss.com $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When a transient entity is passed to lock(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 * existing snapshot
 * 3. throw an exception for each "new" collection
 *
 * @author Gavin King
 */
public class OnLockVisitor extends ReattachVisitor {

	public OnLockVisitor(EventSource session, Serializable key, Object owner) {
		super( session, key, owner );
	}

	Object processCollection(Object collection, CollectionType type) throws HibernateException {

		SessionImplementor session = getSession();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		if ( collection == null ) {
			//do nothing
		}
		else if ( collection instanceof PersistentCollection ) {
			PersistentCollection persistentCollection = ( PersistentCollection ) collection;
			if ( persistentCollection.setCurrentSession( session ) ) {
				if ( isOwnerUnchanged( persistentCollection, persister, extractCollectionKeyFromOwner( persister ) ) ) {
					// a "detached" collection that originally belonged to the same entity
					if ( persistentCollection.isDirty() ) {
						throw new HibernateException( "reassociated object has dirty collection" );
					}
					reattachCollection( persistentCollection, type );
				}
				else {
					// a "detached" collection that belonged to a different entity
					throw new HibernateException( "reassociated object has dirty collection reference" );
				}
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				throw new HibernateException( "reassociated object has dirty collection reference" );
			}
		}
		else {
			// brand new collection
			//TODO: or an array!! we can't lock objects with arrays now??
			throw new HibernateException( "reassociated object has dirty collection reference (or an array)" );
		}

		return null;

	}

}
