/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.action.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Marco Belladelli
 */
public class OrphanCollectionRemoveAction extends CollectionRemoveAction {

	public OrphanCollectionRemoveAction(
			PersistentCollection<?> collection,
			CollectionPersister persister,
			Object id,
			boolean emptySnapshot,
			EventSource session) {
		super( collection, persister, id, emptySnapshot, session );
	}
}
