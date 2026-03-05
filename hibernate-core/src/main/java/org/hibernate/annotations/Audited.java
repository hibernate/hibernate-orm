/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.cfg.StateManagementSettings;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated entity class is an audited entity
 * or audited collection. An audited entity or collection keeps
 * a historical record of changes over time. Unlike a
 * {@linkplain Temporal temporal} entity, it explicitly records
 * the nature of each change, such as creation, modification, or
 * deletion. An audited entity or collection maps to two tables,
 * a table holding the current state of the entity or collection,
 * and an <em>audit log</em> table with a record of each change.
 * <p>
 * The audit log contains the following columns:
 * <ul>
 * <li>columns holding the state of the entity or collection at
 *     the moment of creation, modification, or deletion, except
 *     for state held by {@linkplain Excluded excluded attributes},
 * <li>a {@linkplain StateManagementSettings#TRANSACTION_ID_SUPPLIER
 *     transaction id} recording the unit of work in which the
 *     change occurred, and
 * <li>a column indicating the type of change, encoded as 0 for
 *     creation, 1 for modification, and 2 for deletion.
 * </ul>
 * <p>
 * Audited entities are typically used when a supplier of
 * transaction identifiers is available to Hibernate. A supplier
 * may be specified via the configuration property
 * {@value StateManagementSettings#TRANSACTION_ID_SUPPLIER}.
 * Transactions ids must be unique and comparable and must
 * increase monotonically. Typically, such an id is obtained by
 * persisting an instance of an application-defined entity class
 * with a generated id which represents the current unit of work.
 * This entity associates the transaction id with other information
 * about the work being performed, such as the current timestamp,
 * current application user, and so on. If no supplier is provided,
 * the {@linkplain java.time.Instant#now() current JVM instant} is
 * used as the transaction identifier, but relying on this default
 * behavior is not recommended.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Documented
@Target({PACKAGE, TYPE, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Incubating
public @interface Audited {
	/**
	 * The name of the audit log table. Defaults to the
	 * name of the main table holding currently effective
	 * data, with the suffix {@code _aud}.
	 */
	String tableName() default "";

	/**
	 * The name of the column holding the transaction identifier.
	 * @see org.hibernate.engine.spi.SharedSessionContractImplementor#getCurrentTransactionIdentifier()
	 */
	String transactionId() default "REV";

	/**
	 * The name of the column holding the modification type,
	 * encoded as 0 for creation, 1 for modification, and 2
	 * for deletion
	 */
	String modificationType() default "REVTYPE";

	/**
	 * Excludes the annotated attribute from auditing.
	 * Updates to an excluded attribute modify the current
	 * row directly without creating a new revision of the
	 * entity instance. The audit log table does not contain
	 * columns mapped by excluded attributes.
	 */
	@Documented
	@Target({FIELD, METHOD})
	@Retention(RUNTIME)
	@interface Excluded {
	}
}
