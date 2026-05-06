/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.cfg.StateManagementSettings;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
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
 * deletion, and associates each change with a <em>changeset</em>.
 * An audited entity or collection maps to two tables, a table
 * holding the current state of the entity or collection, and an
 * <em>audit log</em> table with a record of each change.
 * <p>
 * The audit log contains the following columns:
 * <ul>
 * <li>columns holding the state of the entity or collection at
 *     the moment of creation, modification, or deletion, except
 *     for state held by {@linkplain Excluded excluded attributes},
 * <li>a {@linkplain StateManagementSettings#CHANGESET_ID_SUPPLIER
 *     changeset id} recording the changeset in which the change
 *     occurred, and
 * <li>a column indicating the type of change, encoded as 0 for
 *     creation, 1 for modification, and 2 for deletion.
 * </ul>
 * <p>
 * Audited entities are typically used when a supplier of changeset
 * identifiers is available to Hibernate. A custom supplier may be
 * specified via the configuration property
 * {@value StateManagementSettings#CHANGESET_ID_SUPPLIER}.
 * Changeset ids must be unique and comparable and must increase
 * monotonically. Typically, such an id is obtained by persisting
 * an instance of an application-defined entity class with a
 * generated id which represents the current changeset. This
 * entity associates the changeset id with other information about
 * the work being performed, such as the current timestamp, current
 * application user, and so on. If no supplier is provided, the
 * {@linkplain java.time.Instant#now() current JVM instant} is used
 * as the changeset identifier, but relying on this default behavior
 * is not recommended.
 * <p>
 * To query historical revisions from the audit log:
 * <ul>
 * <li>specify a changeset id via
 *     {@link org.hibernate.SessionBuilder#atChangeset atChangeset}
 *     when opening a session, or
 * <li>use an {@link org.hibernate.audit.AuditLog AuditLog}.
 * </ul>
 * <p>
 * Use the nested {@link Table @Audited.Table} annotation to
 * customize the audit log's table name, schema, catalog, or
 * column names for the audit log.
 *
 * @see org.hibernate.audit.AuditLog
 *
 * @author Gavin King
 * @author Marco Belladelli
 * @since 7.4
 */
@Documented
@Target({PACKAGE, TYPE, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Incubating
public @interface Audited {

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

	/**
	 * Specifies the audit log's table mapping for an audited
	 * entity. Placed on the entity class alongside
	 * {@link Audited @Audited} to customize the audit table
	 * name, schema, catalog, and column names.
	 * <p>
	 * This annotation may also be placed on a subclass entity
	 * in a JOINED or TABLE_PER_CLASS hierarchy to override
	 * the audit table name, schema, or catalog for that
	 * subclass. The subclass inherits auditing from the root
	 * entity; the annotation on the subclass only customizes
	 * table mapping.
	 *
	 * @since 7.4
	 */
	@Documented
	@Target({TYPE, PACKAGE, ANNOTATION_TYPE})
	@Retention(RUNTIME)
	@interface Table {
		String DEFAULT_CHANGESET_ID_COLUMN_NAME = "REV";
		String DEFAULT_MODIFICATION_TYPE_COLUMN_NAME = "REVTYPE";
		String DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME = "REVEND";

		/**
		 * The name of the audit log table. Defaults to the
		 * name of the main table with the suffix {@code _AUD}.
		 */
		String name() default "";

		/**
		 * The schema of the audit log table. Defaults to the
		 * schema of the main table.
		 */
		String schema() default "";

		/**
		 * The catalog of the audit log table. Defaults to the
		 * catalog of the main table.
		 */
		String catalog() default "";

		/**
		 * The name of the column holding the identifier of the
		 * changeset in which a revision was introduced.
		 *
		 * @see org.hibernate.engine.spi.SharedSessionContractImplementor#getCurrentChangesetIdentifier()
		 */
		String changesetIdColumn()
				default DEFAULT_CHANGESET_ID_COLUMN_NAME;

		/**
		 * The name of the column holding the modification type,
		 * encoded as 0 for creation, 1 for modification, and 2
		 * for deletion.
		 *
		 * @see org.hibernate.audit.ModificationType
		 */
		String modificationTypeColumn()
				default DEFAULT_MODIFICATION_TYPE_COLUMN_NAME;

		/**
		 * The name of the column holding the identifier of
		 * the changeset which invalidates a revision when the
		 * {@linkplain StateManagementSettings#AUDIT_STRATEGY
		 * audit strategy} is set to {@code "validity"}. When
		 * a new audit row is written, the previous row's
		 * invalidating changeset id column is updated with the
		 * current changeset id, marking it as superseded. A
		 * {@code null} value indicates the row is current
		 * (not yet superseded).
		 */
		String invalidatingChangesetIdColumn()
				default DEFAULT_INVALIDATING_CHANGESET_ID_COLUMN_NAME;

		/**
		 * The name of the column holding the timestamp of the
		 * instant at which a revision was superseded when the
		 * {@linkplain StateManagementSettings#AUDIT_STRATEGY
		 * audit strategy} is set to {@code "validity"} and this
		 * attribute is set to a non-empty value.
		 */
		String invalidationTimestampColumn() default "";
	}

	/**
	 * Specifies a custom audit table name for a
	 * {@link jakarta.persistence.SecondaryTable @SecondaryTable}.
	 * Placed on the entity class alongside
	 * {@link Audited @Audited}.
	 *
	 * @since 7.4
	 */
	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@Repeatable(SecondaryTables.class)
	@interface SecondaryTable {
		/**
		 * The name of the secondary table being overridden.
		 */
		String secondaryTableName();

		/**
		 * The custom audit table name for this secondary table.
		 */
		String secondaryAuditTableName();
	}

	/**
	 * Container for repeatable {@link SecondaryTable} annotations.
	 *
	 * @see SecondaryTable
	 * @since 7.4
	 */
	@Documented
	@Target(TYPE)
	@Retention(RUNTIME)
	@interface SecondaryTables {
		SecondaryTable[] value();
	}

	/**
	 * Specifies a custom audit table name (and optionally schema
	 * and catalog) for an audited collection.
	 * Placed on the collection field or property.
	 *
	 * @since 7.4
	 */
	@Documented
	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	@interface CollectionTable {
		/**
		 * The name of the collection audit table.
		 */
		String name();

		/**
		 * The schema of the collection audit table.
		 * Defaults to the schema of the owning entity's
		 * audit table.
		 */
		String schema() default "";

		/**
		 * The catalog of the collection audit table.
		 * Defaults to the catalog of the owning entity's
		 * audit table.
		 */
		String catalog() default "";
	}
}
