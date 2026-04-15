/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.audit.RevisionListener;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an entity as the revision entity for audit logging.
 * The annotated class must also be annotated
 * {@link jakarta.persistence.Entity @Entity} and must have:
 * <ul>
 *   <li>a field annotated with {@link TransactionId} (typically
 *       the {@code @Id} with {@code @GeneratedValue})</li>
 *   <li>a field annotated with {@link Timestamp}</li>
 * </ul>
 * <p>
 * The revision entity is responsible for initializing its own
 * {@link Timestamp @Timestamp} field, for example, via a field
 * initializer, in the constructor, or in a
 * {@link org.hibernate.audit.RevisionListener}.
 * <p>
 * When a class annotated with {@code @RevisionEntity} is found
 * in the domain model, it is automatically configured as the
 * {@link org.hibernate.temporal.spi.TransactionIdentifierSupplier},
 * and no {@code hibernate.temporal.transaction_id_supplier} setting
 * is required.
 * <p>
 * Only one entity may be annotated with {@code @RevisionEntity}.
 *
 * @see Audited
 * @see TransactionId
 * @see Timestamp
 * @see RevisionListener
 * @since 7.4
 */
@Documented
@Incubating
@Retention(RUNTIME)
@Target(TYPE)
public @interface RevisionEntity {
	/**
	 * An optional {@link RevisionListener} implementation that
	 * will be called after the revision entity is created, to
	 * populate custom fields (e.g. user, comment).
	 */
	Class<? extends RevisionListener> listener() default RevisionListener.class;

	/**
	 * Marks the property that holds the transaction identifier
	 * in a {@link RevisionEntity @RevisionEntity}. This should
	 * typically be the auto-generated primary key
	 * ({@code @Id @GeneratedValue}).
	 * <p>
	 * The value is set by the persistence layer when the
	 * revision entity is inserted.
	 *
	 * @see RevisionEntity
	 * @since 7.4
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({ METHOD, FIELD })
	@interface TransactionId {
	}

	/**
	 * Marks the property that holds the revision timestamp in a
	 * {@link RevisionEntity @RevisionEntity}. The value must be
	 * initialized by the revision entity itself, for example,
	 * via a field initializer, in the constructor, or in a
	 * {@link org.hibernate.audit.RevisionListener}.
	 *
	 * @see RevisionEntity
	 * @since 7.4
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({ METHOD, FIELD })
	@interface Timestamp {
	}

	/**
	 * Marks a {@code Set<String>} property on a
	 * {@link RevisionEntity @RevisionEntity} that holds the
	 * names of entity types modified in each revision. The
	 * property is typically mapped as an
	 * {@code @ElementCollection}.
	 * <p>
	 * When this annotation is present on a revision entity,
	 * cross-type revision queries are automatically enabled
	 * via {@link org.hibernate.audit.AuditLog}.
	 *
	 * @see RevisionEntity
	 * @since 7.4
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({ METHOD, FIELD })
	@interface ModifiedEntities {
	}
}
