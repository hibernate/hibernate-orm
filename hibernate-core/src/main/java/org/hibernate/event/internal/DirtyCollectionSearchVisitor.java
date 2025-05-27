/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

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
	private boolean dirty;

	public DirtyCollectionSearchVisitor(Object entity, EventSource session, boolean[] propertyVersionability) {
		super( session );
		EnhancementAsProxyLazinessInterceptor interceptor = null;
		if ( isPersistentAttributeInterceptable( entity ) ) {
			PersistentAttributeInterceptor attributeInterceptor =
					asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
			if ( attributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
				interceptor = lazinessInterceptor;
			}
		}
		this.interceptor = interceptor;
		this.propertyVersionability = propertyVersionability;
	}

	public boolean wasDirtyCollectionFound() {
		return dirty;
	}

	@Override
	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if ( collection != null ) {
			final SessionImplementor session = getSession();
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
	boolean includeEntityProperty(Object[] values, int i) {
		return propertyVersionability[i] && super.includeEntityProperty( values, i );
	}
}
