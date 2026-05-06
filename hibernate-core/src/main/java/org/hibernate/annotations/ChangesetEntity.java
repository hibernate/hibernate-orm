/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.audit.ChangesetListener;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an entity as the changeset entity for audit logging.
 * The annotated class must also be annotated
 * {@link jakarta.persistence.Entity @Entity} and must have:
 * <ul>
 * <li>a field annotated {@link ChangesetId}, typically the
 *     {@code @Id} with {@code @GeneratedValue}, and
 * <li>a field annotated {@link Timestamp}.
 * </ul>
 * Optionally, a {@link ModifiedEntities @ModifiedEntities}
 * property may be declared to enable cross-type changeset
 * queries via {@link org.hibernate.audit.AuditLog}.
 * <p>
 * The changeset entity is responsible for initializing its own
 * {@link Timestamp @Timestamp} field, for example, via
 * {@link CreationTimestamp}, in the constructor, or in a
 * {@link ChangesetListener}.
 * <p>
 * When a class annotated with {@code @ChangesetEntity} is found
 * in the domain model, it is automatically configured as the
 * {@link org.hibernate.temporal.spi.ChangesetIdentifierSupplier
 * changeset id supplier}, and it is not necessary to explicitly
 * set the property {@value org.hibernate.cfg.StateManagementSettings#CHANGESET_ID_SUPPLIER}.
 * <p>
 * Only one entity may be annotated with {@code @ChangesetEntity}.
 *
 * @see Audited
 * @see ChangesetId
 * @see Timestamp
 * @see ModifiedEntities
 * @see ChangesetListener
 * @since 7.4
 */
@Documented
@Incubating
@Retention(RUNTIME)
@Target(TYPE)
public @interface ChangesetEntity {
	/**
	 * An optional {@link ChangesetListener} implementation that
	 * will be called after the changeset entity is created, to
	 * populate custom fields (e.g. user, comment).
	 */
	Class<? extends ChangesetListener> listener() default ChangesetListener.class;

	/**
	 * Marks the property that holds the changeset identifier
	 * in a {@link ChangesetEntity @ChangesetEntity}. This
	 * should typically be the auto-generated primary key
	 * ({@code @Id @GeneratedValue}).
	 * <p>
	 * The value is set by the persistence layer when the
	 * changeset entity is inserted.
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({METHOD, FIELD})
	@interface ChangesetId {
	}

	/**
	 * Marks the property that holds the changeset timestamp in a
	 * {@link ChangesetEntity @ChangesetEntity}. The value must be
	 * initialized by the changeset entity itself, for example,
	 * via a field initializer, in the constructor, or in a
	 * {@link ChangesetListener}.
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({METHOD, FIELD})
	@interface Timestamp {
	}

	/**
	 * Marks a {@code Set<String>} property of a
	 * {@link ChangesetEntity @ChangesetEntity} that holds the
	 * names of entity types modified in each changeset. The
	 * property is typically mapped as an
	 * {@code @ElementCollection}.
	 * <p>
	 * When this annotation is present on a changeset entity,
	 * cross-type changeset queries are automatically enabled
	 * via {@link org.hibernate.audit.AuditLog}.
	 *
	 * @see ChangesetEntity
	 * @see org.hibernate.audit.AuditLog#getEntityTypesModifiedAt
	 * @see org.hibernate.audit.AuditLog#findAllEntitiesModifiedAt
	 * @see org.hibernate.audit.AuditLog#findAllEntitiesGroupedByModificationType
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({METHOD, FIELD})
	@interface ModifiedEntities {
	}
}
