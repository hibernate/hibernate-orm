/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.cfg.TemporalTableStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Instant;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated entity class is a temporal entity
 * or temporal collection. A temporal entity or collection keeps a
 * historical record of changes over time. Each row of the mapped
 * table represents a single revision of a single instance of the
 * entity, or an element of the collection whose existence is
 * bounded in time, with:
 * <ul>
 * <li>a {@link #rowStart} timestamp representing the instant at
 *     which the revision became effective, and
 * <li>a {@link #rowEnd} timestamp representing the instant at
 *     which the revision was superseded by a newer revision.
 * </ul>
 * The revision which is currently effective has a null value for
 * its {@linkplain #rowEnd} timestamp.
 * <p>
 * Given the identifier of an instance of a temporal entity, along
 * with an instant, which may be represented by an instance if
 * {@link java.time.Instant}, the <em>effective</em> revision of the
 * temporal entity with the given identifier at the given instant is
 * the revision with:
 * <ul>
 * <li>{@link #rowStart} timestamp less than or equal to the given
 *     instant, and
 * <li>{@link #rowEnd} timestamp null or greater than the given
 *     instant.
 * </ul>
 * <p>
 * There are three {@linkplain TemporalTableStrategy strategies}
 * for mapping a temporal entity or collection to a table or tables.
 * <ul>
 *     <li>In the {@linkplain TemporalTableStrategy#SINGLE_TABLE
 *         single table} strategy, current and historical data
 *         is stored together in the same table. Foreign keys
 *         referencing this table have no constraints, and so
 *         the database cannot enforce referential integrity.
 *         The table may be {@linkplain HistoryPartitioning
 *         partitioned} into current and historical partitions
 *     <li>In the {@linkplain TemporalTableStrategy#HISTORY_TABLE
 *         separate history table} strategy, current data is stored
 *         in one table, and historical data is stored in a second
 *         table. Referential integrity may be enforced for current
 *         data, but not for historical data. The {@code rowStart}
 *         and {@code rowEnd} columns belong to the history table.
 *     <li>In the {@linkplain TemporalTableStrategy#NATIVE native}
 *         strategy, temporal data is stored in a temporal table
 *         when temporal tables are supported natively by the
 *         database. The {@code rowStart} and {@code rowEnd} columns
 *         are managed by the database itself. Depending on the
 *         capabilities of the database, referential integrity might
 *         be enforced.
 * </ul>
 * <p>
 * The configuration property
 * {@value StateManagementSettings#TEMPORAL_TABLE_STRATEGY}
 * controls the temporal table mapping strategy.
 * <p>
 * By default, a session or stateless session reads revisions of
 * temporal data which are currently effective. To read historical
 * revisions effective at a given {@linkplain Instant instant}, set
 * {@linkplain org.hibernate.engine.creation.CommonBuilder#asOf
 * the temporal data instant} when creating the session or stateless
 * session.
 * <p>
 * The following recommendations do not apply to the native mapping
 * strategy:
 * <ul>
 * <li>It is recommended that every temporal entity declare a
 *     {@linkplain jakarta.persistence.Version version} attribute.
 *     The primary key of a table mapped by a temporal entity
 *     includes the columns mapped by the identifier of the entity,
 *     along with the version column, if there is one, or the
 *     {@link #rowStart} column if there is no version.
 * <li>When working with temporal entities, it is important to
 *     ensure that referential integrity is maintained by the
 *     application and validated by triggers or offline processes.
 * </ul>
 * <p>
 * By default, timestamps for the {@code rowStart} and
 * {@code rowEnd} columns are generated in Java, unless native
 * temporal tables are used. If timestamps should be generated on
 * the database server, enable the configuration property
 * {@value StateManagementSettings#USE_SERVER_TRANSACTION_TIMESTAMPS}.
 * <p>
 * An alternative approach, which is not compatible with the
 * {@linkplain TemporalTableStrategy#NATIVE native} mapping strategy,
 * is to provide a custom {@link java.util.function.Supplier} of
 * transaction ids by specifying the configuration property
 * {@value StateManagementSettings#TRANSACTION_ID_SUPPLIER}.
 * Transactions ids must be unique and comparable and must increase
 * monotonically. Typically, such an id is obtained by persisting
 * an instance of an application-defined entity class with a
 * generated id which represents the current unit of work. This
 * entity associates the transaction id with other information about
 * the work being performed, such as the current timestamp, current
 * application user, and so on.
 *
 * @apiNote
 * {@linkplain jakarta.persistence.SecondaryTable Secondary tables} and
 * {@linkplain org.hibernate.boot.model.source.spi.InheritanceType#JOINED
 * joined inheritance mappings} are not supported for temporal entities.
 *
 * @see org.hibernate.engine.creation.CommonBuilder#asOf(Instant)
 * @see org.hibernate.engine.creation.CommonBuilder#atTransaction(Object)
 * @see StateManagementSettings#TEMPORAL_TABLE_STRATEGY
 * @see StateManagementSettings#TRANSACTION_ID_SUPPLIER
 * @see StateManagementSettings#USE_SERVER_TRANSACTION_TIMESTAMPS
 *
 * @author Gavin King
 *
 */
@Documented
@Target({PACKAGE, TYPE, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Incubating
public @interface Temporal {
	/**
	 * The name of the column holding the starting timestamp
	 * or transaction id of a revision; the timestamp or id
	 * representing the moment the revision became effective;
	 * that is, the "effective from" timestamp.
	 */
	String rowStart() default "effective";
	/**
	 * The name of the column holding the ending timestamp
	 * or transaction id of a revision; the timestamp or id
	 * representing the moment the revision was superseded;
	 * that is, the "effective to" timestamp.
	 */
	String rowEnd() default "superseded";

	/**
	 * The fractional seconds precision for both temporal columns.
	 *
	 * @see jakarta.persistence.Column#secondPrecision()
	 */
	int secondPrecision() default -1;

	/**
	 * Enables partitioning for a temporal table mapped by
	 * a {@linkplain Temporal temporal entity or collection}
	 * in the {@linkplain TemporalTableStrategy#SINGLE_TABLE
	 * single table} temporal mapping strategy. In other
	 * mapping strategies this annotation has no effect.
	 */
	@Documented
	@Target({TYPE, FIELD, METHOD})
	@Retention(RUNTIME)
	@interface HistoryPartitioning {
		/**
		 * The name of the partition holding currently
		 * effective data. Defaults to the temporal table
		 * name with the suffix {@code _current}.
		 */
		String currentPartition() default "";
		/**
		 * The name of the partition holding historical
		 * data. Defaults to the temporal table name with
		 * the suffix {@code _history}.
		 */
		String historyPartition() default "";
	}

	/**
	 * Specifies the name of the separate history table for
	 * a {@linkplain Temporal temporal entity or collection}
	 * when the history table strategy is used.
	 *
	 * @see TemporalTableStrategy#HISTORY_TABLE
	 */
	@Documented
	@Target({TYPE, FIELD, METHOD})
	@Retention(RUNTIME)
	@interface HistoryTable {
		/**
		 * The name of the history table. Defaults to the
		 * name of the main table holding currently effective
		 * data, with the suffix {@code _history}.
		 */
		String name() default "";
	}

	/**
	 * Excludes the annotated attribute from temporal
	 * versioning. Updates to an excluded attribute
	 * modify the current row directly without creating
	 * a new revision of the entity instance.
	 * <p>For {@linkplain TemporalTableStrategy#NATIVE
	 * native temporal tables}, this is only supported
	 * if the database itself provides a way to exclude
	 * a column from temporal versioning.
	 */
	@Documented
	@Target({FIELD, METHOD})
	@Retention(RUNTIME)
	@interface Excluded {
	}
}
