/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

import static org.hibernate.engine.internal.Collections.processReachableCollection;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	private final Object owner;

	public FlushVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
			return null;
		}
		else if ( collection != null ) {
			final EventSource session = getSession();
			final PersistentCollection<?> persistentCollection;
			if ( type.hasHolder() ) {
				persistentCollection = session.getPersistenceContextInternal().getCollectionHolder( collection );
			}
			else if ( collection == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				final Object keyOfOwner = type.getKeyOfOwner( owner, session );
				persistentCollection = (PersistentCollection<?>)
						type.getCollection( keyOfOwner, session, owner, Boolean.FALSE );
			}
			else if ( collection instanceof PersistentCollection<?> wrapper ) {
				persistentCollection = wrapper;
			}
			else {
				return null;
			}
			processReachableCollection( persistentCollection, type, owner, session );
			return null;
		}
		else {
			return null;
		}
	}

	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return true;
	}

}
