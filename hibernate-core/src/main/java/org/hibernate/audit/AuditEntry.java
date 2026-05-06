/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


/**
 * A tuple representing an entity at a specific revision in
 * the audit history.
 * <p>
 * The {@link #revision} field is:
 * <ul>
 *   <li>the revision entity instance (e.g. {@link DefaultRevisionEntity}), if one is configured
 *   <li>the plain transaction identifier (e.g. {@code Instant}, {@code Integer}) otherwise</li>
 * </ul>
 *
 * @param entity the entity snapshot at this revision
 * @param revision the revision entity (if configured) or transaction identifier
 * @param modificationType the type of modification (ADD/MOD/DEL)
 * @param <T> the entity type
 * @author Marco Belladelli
 * @since 7.4
 */
public record AuditEntry<T>(T entity, Object revision, ModificationType modificationType) {
}
