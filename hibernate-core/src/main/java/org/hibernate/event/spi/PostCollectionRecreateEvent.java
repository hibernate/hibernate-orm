/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * An event that occurs after a collection is recreated
 *
 * @author Gail Badner
 */
public class PostCollectionRecreateEvent extends AbstractCollectionEvent {

	public PostCollectionRecreateEvent(
			PersistentCollectionDescriptor collectionDescriptor,
			PersistentCollection collection,
			EventSource source ) {
		super( collectionDescriptor, collection, source,
				collection.getOwner(),
				getOwnerIdOrNull( collection.getOwner(), source ) );
	}
}
