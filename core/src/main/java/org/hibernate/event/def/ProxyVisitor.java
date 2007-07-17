//$Id: ProxyVisitor.java 7181 2005-06-17 19:36:08Z oneovthafew $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;

/**
 * Reassociates uninitialized proxies with the session
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {


	public ProxyVisitor(EventSource session) {
		super(session);
	}

	Object processEntity(Object value, EntityType entityType) throws HibernateException {

		if (value!=null) {
			getSession().getPersistenceContext().reassociateIfUninitializedProxy(value);
			// if it is an initialized proxy, let cascade
			// handle it later on
		}

		return null;
	}

	/**
	 * Has the owner of the collection changed since the collection
	 * was snapshotted and detached?
	 */
	protected static boolean isOwnerUnchanged(
			final PersistentCollection snapshot, 
			final CollectionPersister persister, 
			final Serializable id
	) {
		return isCollectionSnapshotValid(snapshot) &&
				persister.getRole().equals( snapshot.getRole() ) &&
				id.equals( snapshot.getKey() );
	}

	private static boolean isCollectionSnapshotValid(PersistentCollection snapshot) {
		return snapshot != null &&
				snapshot.getRole() != null &&
				snapshot.getKey() != null;
	}
	
	/**
	 * Reattach a detached (disassociated) initialized or uninitialized
	 * collection wrapper, using a snapshot carried with the collection
	 * wrapper
	 */
	protected void reattachCollection(PersistentCollection collection, CollectionType type)
	throws HibernateException {
		if ( collection.wasInitialized() ) {
			CollectionPersister collectionPersister = getSession().getFactory()
			.getCollectionPersister( type.getRole() );
			getSession().getPersistenceContext()
				.addInitializedDetachedCollection( collectionPersister, collection );
		}
		else {
			if ( !isCollectionSnapshotValid(collection) ) {
				throw new HibernateException( "could not reassociate uninitialized transient collection" );
			}
			CollectionPersister collectionPersister = getSession().getFactory()
					.getCollectionPersister( collection.getRole() );
			getSession().getPersistenceContext()
				.addUninitializedDetachedCollection( collectionPersister, collection );
		}
	}

}
