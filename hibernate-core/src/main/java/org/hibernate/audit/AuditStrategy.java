/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

/**
 * Enumerates the possible strategies for querying
 * {@linkplain org.hibernate.annotations.Audited audited}
 * entities and collections.
 *
 * @see org.hibernate.annotations.Audited
 * @see org.hibernate.cfg.StateManagementSettings#AUDIT_STRATEGY
 *
 * @author Gavin King
 *
 * @since 7.4
 */
public enum AuditStrategy {
	/**
	 * Use a {@code MAX(REV)} subquery to find the current audit row for
	 * point-in-time queries. This strategy does not require additional
	 * audit table columns.
	 */
	DEFAULT,
	/**
	 * Store an end changeset column on each audit row, allowing
	 * point-in-time queries to use a validity range predicate.
	 */
	VALIDITY;

}
