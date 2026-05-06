/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.ChangesetEntity;


/**
 * A built-in changeset entity which maps to the {@code REVINFO} table.
 * <p>
 * Maps to the {@code REVINFO} table with columns:
 * <ul>
 *   <li>{@code REV}: auto-generated integer primary key</li>
 *   <li>{@code REVTSTMP}: Unix epoch timestamp in milliseconds</li>
 * </ul>
 * <p>
 * To use this entity, add it to the domain model of your application.
 * <p>
 * For entity change tracking (cross-type revision queries),
 * use {@link DefaultTrackingModifiedEntitiesChangesetEntity} instead.
 *
 * @author Marco Belladelli
 * @see ChangesetEntity
 * @see DefaultTrackingModifiedEntitiesChangesetEntity
 * @since 7.4
 */
@ChangesetEntity
@Entity(name = "DefaultChangesetEntity")
@Table(name = "REVINFO")
public final class DefaultChangesetEntity extends ChangesetMapping {
}
