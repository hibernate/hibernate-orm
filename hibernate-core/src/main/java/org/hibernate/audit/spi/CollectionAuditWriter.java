/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for writing collection audit rows to the audit
 * table at transaction completion.
 *
 * @see AuditWorkQueue
 * @since 7.4
 */
@FunctionalInterface
public interface CollectionAuditWriter {
	/**
	 * Write audit rows for the given collection.
	 *
	 * @param collection the persistent collection
	 * @param ownerId the owning entity's identifier
	 * @param originalSnapshot the collection snapshot before the first flush,
	 * or {@code null} for new collections
	 * @param session the current session
	 */
	void writeCollectionAuditRows(
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			SharedSessionContractImplementor session);
}
