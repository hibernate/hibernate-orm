/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		if ( collection != null && collection != CollectionType.UNFETCHED_COLLECTION ) {
			final var session = getSession();
			final var persistentCollection = persistentCollection( collection, type, session );
			if ( persistentCollection != null ) {
				processReachableCollection( persistentCollection, type, owner, session );
			}
		}
		return null;
	}

	private PersistentCollection<?> persistentCollection(Object collection, CollectionType type, EventSource session) {
		if ( type.hasHolder() ) {
			return session.getPersistenceContextInternal().getCollectionHolder( collection );
		}
		else if ( collection == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			final Object keyOfOwner = type.getKeyOfOwner( owner, session );
			return (PersistentCollection<?>) type.getCollection( keyOfOwner, session, owner, Boolean.FALSE );
		}
		else if ( collection instanceof PersistentCollection<?> wrapper ) {
			return wrapper;
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
