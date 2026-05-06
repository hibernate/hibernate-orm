/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;


/**
 * A tuple representing an entity at a specific changeset in
 * the audit history.
 * <p>
 * The {@link #changeset} field holds:
 * <ul>
 *   <li>the changelog entity instance
 *       (e.g. {@link DefaultChangelog}), if one is
 *       configured, or
 *   <li>the plain changeset identifier
 *       (e.g. {@code Instant}, {@code Integer}) otherwise.
 * </ul>
 *
 * @param entity the entity snapshot after application of the changeset
 * @param changeset the changelog entity (if configured) or changeset identifier
 * @param modificationType the type of modification (ADD/MOD/DEL)
 * @param <T> the entity type
 * @author Marco Belladelli
 * @since 7.4
 */
public record AuditEntry<T>(T entity, Object changeset, ModificationType modificationType) {
}
