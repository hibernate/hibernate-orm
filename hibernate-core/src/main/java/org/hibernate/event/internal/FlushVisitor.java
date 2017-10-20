/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Collections;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeCollection;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	
	private Object owner;

	@Override
	Object processCollection(Object collection, PluralAttributeCollection attributeCollection) throws HibernateException {

		if ( collection == PersistentCollectionDescriptor.UNFETCHED_COLLECTION ) {
			return null;
		}

		if (collection!=null) {
			final PersistentCollection coll;
			if ( attributeCollection.getPersistentCollectionDescriptor().getCollectionClassification() == CollectionClassification.ARRAY ) {
				coll = getSession().getPersistenceContext().getCollectionHolder(collection);
			}
			else {
				coll = (PersistentCollection) collection;
			}

			Collections.processReachableCollection( coll, attributeCollection, owner, getSession() );
		}

		return null;

	}

	FlushVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

}
