/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.ChangesetEntity;


/**
 * A built-in changeset entity with entity change tracking.
 * Drop-in replacement for {@link DefaultChangesetEntity} that
 * additionally creates a {@code REVCHANGES} table recording
 * which entity types were modified in each changeset.
 * <p>
 * Use this entity instead of {@link DefaultChangesetEntity}
 * when cross-type revision queries are needed.
 *
 * @author Marco Belladelli
 * @see TrackingModifiedEntitiesChangesetMapping
 * @see ChangesetEntity.ModifiedEntities
 * @since 7.4
 */
@ChangesetEntity
@Entity(name = "DefaultTrackingModifiedEntitiesChangesetEntity")
@Table(name = "REVINFO")
public final class DefaultTrackingModifiedEntitiesChangesetEntity extends TrackingModifiedEntitiesChangesetMapping {
}
