/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Changelog;


/**
 * A built-in changelog entity with entity change tracking.
 * Drop-in replacement for {@link DefaultChangelog} that
 * additionally creates a {@code REVCHANGES} table recording
 * which entity types were modified in each changeset.
 * <p>
 * Use this entity instead of {@link DefaultChangelog}
 * when cross-type revision queries are needed.
 *
 * @author Marco Belladelli
 * @see TrackingModifiedEntitiesChangelogMapping
 * @see Changelog.ModifiedEntities
 * @since 7.4
 */
@Changelog
@Entity
@Table(name = "REVINFO")
public final class DefaultTrackingModifiedEntitiesChangelog extends TrackingModifiedEntitiesChangelogMapping {
}
