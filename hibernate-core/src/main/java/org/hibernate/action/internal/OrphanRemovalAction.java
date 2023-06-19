/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

public final class OrphanRemovalAction extends EntityDeleteAction {

	public OrphanRemovalAction(
			Object id, Object[] state, Object version, Object instance,
			EntityPersister persister, boolean isCascadeDeleteEnabled, EventSource session) {
		super( id, state, version, instance, persister, isCascadeDeleteEnabled, session );
		markOwnedCollectionsForEarlyRemoval( instance, persister, session );
	}

	private void markOwnedCollectionsForEarlyRemoval(Object owner, EntityPersister persister, EventSource session) {
		if ( persister.hasOwnedCollections() ) {
			for ( Type type : persister.getPropertyTypes() ) {
				deleteOwnedCollections( type, owner, session );
			}
		}
	}

	private static void deleteOwnedCollections(Type type, Object owner, EventSource session) {
		if ( type.isCollectionType() ) {
			final CollectionType collectionType = (CollectionType) type;
			final String role = collectionType.getRole();
			final CollectionPersister collectionPersister = session.getFactory()
					.getMappingMetamodel()
					.getCollectionDescriptor( role );
			if ( !collectionPersister.isInverse() ) {
				final CollectionKey collectionKey = new CollectionKey(
						collectionPersister,
						collectionType.getKeyOfOwner( owner, session )
				);
				final PersistentCollection<?> coll = session.getPersistenceContext().getCollection( collectionKey );
				final CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( coll );
				session.getInterceptor().onCollectionRemove( coll, ce.getLoadedKey() );
				session.getActionQueue().addAction(
						new OrphanCollectionRemoveAction(
								coll,
								ce.getLoadedPersister(),
								ce.getLoadedKey(),
								ce.isSnapshotEmpty( coll ),
								session
						)
				);
				ce.setIgnore( true );
			}
		}
		else if ( type.isComponentType() ) {
			final Type[] subtypes = ( (CompositeType) type ).getSubtypes();
			for ( Type subtype : subtypes ) {
				deleteOwnedCollections( subtype, owner, session );
			}
		}
	}
}
