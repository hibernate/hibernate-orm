/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Changelog;


/**
 * A built-in changelog entity which maps to the {@code REVINFO} table.
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
 * use {@link DefaultTrackingModifiedEntitiesChangelog} instead.
 *
 * @author Marco Belladelli
 * @see Changelog
 * @see DefaultTrackingModifiedEntitiesChangelog
 * @since 7.4
 */
@Changelog
@Entity
@Table(name = "REVINFO")
public final class DefaultChangelog extends ChangelogMapping {
}
