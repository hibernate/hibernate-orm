/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * Reassociates uninitialized proxies with the session
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {


	public ProxyVisitor(EventSource session) {
		super(session);
	}

	@Override
	Object processEntity(Object value, EntityTypeDescriptor descriptor) throws HibernateException {

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
			final PersistentCollectionDescriptor descriptor,
			final Serializable id
	) {
		return isCollectionSnapshotValid(snapshot) &&
				descriptor.getNavigableRole().getFullPath().equals( snapshot.getRole() ) &&
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
	protected void reattachCollection(PersistentCollection collection, NavigableRole role)
	throws HibernateException {
		if ( collection.wasInitialized() ) {
			PersistentCollectionDescriptor collectionDescriptor = getSession().getFactory().getMetamodel()
			.findCollectionDescriptor( role.getFullPath() );
			getSession().getPersistenceContext()
				.addInitializedDetachedCollection( collectionDescriptor, collection );
		}
		else {
			if ( !isCollectionSnapshotValid(collection) ) {
				throw new HibernateException( "could not reassociate uninitialized transient collection" );
			}
			PersistentCollectionDescriptor collectionDescriptor = getSession().getFactory().getMetamodel()
					.findCollectionDescriptor( collection.getRole() );
			getSession().getPersistenceContext()
				.addUninitializedDetachedCollection( collectionDescriptor, collection );
		}
	}

}
