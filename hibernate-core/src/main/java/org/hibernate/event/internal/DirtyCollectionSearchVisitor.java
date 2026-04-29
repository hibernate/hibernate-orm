/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;

/**
 * Do we have a dirty collection here?
 * <ol>
 * <li>If it's a new application-instantiated collection, return true. (Does not occur anymore!)
 * <li>If it's an embeddable, recurse.
 * <li>If it is a wrappered collection, ask the collection entry.
 * </ol>
 *
 * @author Gavin King
 */
public class DirtyCollectionSearchVisitor extends AbstractVisitor {

	private final EnhancementAsProxyLazinessInterceptor interceptor;
	private final boolean[] propertyVersionability;
	private final boolean includeNonVersionedCollections;
	private boolean currentPropertyVersionable;
	private boolean dirty;

	public DirtyCollectionSearchVisitor(Object entity, EventSource session, boolean[] propertyVersionability) {
		this( entity, session, propertyVersionability, false );
	}

	public DirtyCollectionSearchVisitor(
			Object entity,
			EventSource session,
			boolean[] propertyVersionability,
			boolean includeNonVersionedCollections) {
		super( session );
		this.propertyVersionability = propertyVersionability;
		this.includeNonVersionedCollections = includeNonVersionedCollections;
		this.interceptor =
				isPersistentAttributeInterceptable( entity )
					&& asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor()
							instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor
						? lazinessInterceptor
						: null;
	}

	public boolean wasDirtyCollectionFound() {
		return dirty;
	}

	@Override
	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( includeNonVersionedCollections
				&& !currentPropertyVersionable
				&& type.isInverse( getSession().getFactory() ) ) {
			return null;
		}
		if ( collection != null ) {
			final var session = getSession();
			if ( type.isArrayType() ) {
				// if no array holder we found an unwrapped array, it's dirty
				// (this can't occur, because we now always call wrap() before getting to here)

				// we need to check even if it was not initialized, because of delayed adds!
				if ( session.getPersistenceContextInternal().getCollectionHolder( collection ).isDirty() ) {
					dirty = true;
				}
			}
			else if ( interceptor == null || interceptor.isAttributeLoaded( type.getName() ) ) {
				// if not wrapped yet, it's dirty
				// (this can't occur, because we now always call wrap() before getting here)

				// we need to check even if it was not initialized, because of delayed adds!
				if ( ((PersistentCollection<?>) collection).isDirty() ) {
					dirty = true;
				}
			}
		}
		return null;
	}

	@Override
	public void processEntityPropertyValues(Object[] values, Type[] types) throws HibernateException {
		for ( int i = 0; i < types.length; i++ ) {
			if ( includeEntityProperty( values, i ) ) {
				currentPropertyVersionable = propertyVersionability[i];
				processValue( i, values, types );
			}
		}
	}

	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return ( includeNonVersionedCollections || propertyVersionability[i] )
			&& super.includeEntityProperty( values, i );
	}
}
