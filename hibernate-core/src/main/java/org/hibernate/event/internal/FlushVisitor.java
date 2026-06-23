/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.type.CollectionType;

import static org.hibernate.engine.internal.Collections.processReachableCollection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	private final Object owner;
	private final FlushProcessingContext flushProcessingContext;

	public FlushVisitor(
			@Nonnull EventSource session,
			@Nonnull Object owner,
			@Nonnull FlushProcessingContext flushProcessingContext) {
		super(session);
		this.owner = owner;
		this.flushProcessingContext = flushProcessingContext;
	}

	@Nullable
	Object processCollection(@Nullable Object collection, @Nonnull CollectionType type) {
		if ( collection != null ) {
			final var session = getSession();
				final var persistentCollection = persistentCollection( collection, type, session );
				if ( persistentCollection != null ) {
					processReachableCollection( persistentCollection, type, owner, session, flushProcessingContext );
				}
			}
		return null;
	}

	@Nullable
	private PersistentCollection<?> persistentCollection(
			@Nullable Object collection,
			@Nonnull CollectionType type,
			@Nonnull EventSource session) {
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
	boolean includeEntityProperty(@Nonnull Object[] values, int i) {
		return true;
	}

}
