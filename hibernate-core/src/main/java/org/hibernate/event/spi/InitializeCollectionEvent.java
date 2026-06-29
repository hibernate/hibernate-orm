/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import jakarta.annotation.Nonnull;

/**
 * An event that occurs when a collection wants to be
 * initialized
 *
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractCollectionEvent {

	@Internal
	public InitializeCollectionEvent(@Nonnull PersistentCollection<?> collection, @Nonnull EventSource source ) {
		super(
				getLoadedCollectionPersister( collection, source ),
				collection,
				source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source )
		);
	}
}
