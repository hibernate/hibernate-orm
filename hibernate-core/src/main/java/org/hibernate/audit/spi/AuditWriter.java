/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.spi;

import org.hibernate.audit.ModificationType;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for writing a single entity audit row to the
 * audit table at transaction completion.
 *
 * @see AuditWorkQueue
 * @since 7.4
 */
@FunctionalInterface
public interface AuditWriter {
	/**
	 * Write an audit row for the given entity state and modification type.
	 * Called by the {@link AuditWorkQueue} at transaction completion.
	 *
	 * @param entityKey the entity key
	 * @param entity the entity instance (may be null)
	 * @param values the entity state
	 * @param modificationType the modification type (ADD/MOD/DEL)
	 * @param session the current session
	 */
	void writeAuditRow(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			SharedSessionContractImplementor session);
}
