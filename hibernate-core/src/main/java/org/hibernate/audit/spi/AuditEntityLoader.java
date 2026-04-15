/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for loading entity snapshots from audit tables at a specific transaction
 *
 * @author Marco Belladelli
 * @see org.hibernate.metamodel.mapping.AuditMapping#getEntityLoader
 * @since 7.4
 */
public interface AuditEntityLoader {

	/**
	 * Load an entity snapshot at the given transaction.
	 *
	 * @param id the entity identifier
	 * @param transactionId the transaction identifier
	 * @param includeDeletions whether to include DEL revisions
	 * @param session the session to use for loading
	 *
	 * @return the entity instance, or {@code null}
	 */
	<T> T find(Object id, Object transactionId, boolean includeDeletions, SharedSessionContractImplementor session);
}
