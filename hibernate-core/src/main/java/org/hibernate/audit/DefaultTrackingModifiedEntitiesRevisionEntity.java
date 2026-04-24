/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.RevisionEntity;


/**
 * A built-in revision entity with entity change tracking.
 * Drop-in replacement for {@link DefaultRevisionEntity} that
 * additionally creates a {@code REVCHANGES} table recording
 * which entity types were modified in each revision.
 * <p>
 * Use this entity instead of {@link DefaultRevisionEntity}
 * when cross-type revision queries are needed.
 *
 * @author Marco Belladelli
 * @see TrackingModifiedEntitiesRevisionMapping
 * @see RevisionEntity.ModifiedEntities
 * @since 7.4
 */
@RevisionEntity
@Entity(name = "DefaultTrackingModifiedEntitiesRevisionEntity")
@Table(name = "REVINFO")
public final class DefaultTrackingModifiedEntitiesRevisionEntity extends TrackingModifiedEntitiesRevisionMapping {
}
