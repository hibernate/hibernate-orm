/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.dialect.Dialect;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

/**
 * Settings related to {@linkplain org.hibernate.persister.state.spi.StateManagement
 * customized state management}, {@linkplain org.hibernate.annotations.Temporal
 * temporal entities}, and {@linkplain org.hibernate.annotations.Audited audited
 * entities}.
 *
 * @author Gavin King
 */
@Incubating
public interface StateManagementSettings {
	/**
	 * Specifies the implementation strategy for
	 * {@linkplain org.hibernate.annotations.Temporal temporal tables}.
	 * <p>
	 * Accepts any of:
	 * <ul>
	 *     <li>an instance of {@link TemporalTableStrategy}, or
	 *     <li>the (case-insensitive) name of a {@code TemporalTableStrategy},
	 *     for example, {@code native}, {@code single_time}, or
	 *     {@code history_table}.
	 * </ul>
	 *
	 * @settingDefault {@link TemporalTableStrategy#SINGLE_TABLE}
	 * @see org.hibernate.annotations.Temporal
	 * @see TemporalTableStrategy
	 *
	 * @since 7.4
	 */
	String TEMPORAL_TABLE_STRATEGY = "hibernate.temporal.table_strategy";

	/**
	 * Use transaction timestamps supplied by the database server's
	 * {@link Dialect#currentTimestamp() current_timestamp} function
	 * instead of {@link java.time.Instant#now()} to initialize the
	 * effectivity columns for
	 * {@linkplain org.hibernate.annotations.Temporal temporal data}
	 * when using the {@link TemporalTableStrategy#SINGLE_TABLE} or
	 * {@link TemporalTableStrategy#HISTORY_TABLE} mapping strategy.
	 * <p>
	 * Not recommended on database platforms with no way to obtain
	 * the timestamp of the start of the current transaction (most
	 * database except PostgreSQL-like databases).
	 * <p>
	 * This option cannot be used together with
	 * {@value #CHANGESET_ID_SUPPLIER}.
	 * <p>
	 * By default, transaction timestamps are generated in memory.
	 *
	 * @settingDefault {@code false}
	 * @see org.hibernate.annotations.Temporal
	 *
	 * @since 7.4
	 */
	String USE_SERVER_TRANSACTION_TIMESTAMPS = "hibernate.temporal.use_server_transaction_timestamps";

	/**
	 * Specify a {@link ChangesetIdentifierSupplier} which
	 * provides unique, monotonically increasing changeset ids for
	 * {@linkplain org.hibernate.annotations.Temporal temporal data}
	 * when using the {@link TemporalTableStrategy#SINGLE_TABLE} or
	 * {@link TemporalTableStrategy#HISTORY_TABLE} mapping strategy
	 * or for {@linkplain org.hibernate.annotations.Audited audited
	 * data}. A plain {@link java.util.function.Supplier Supplier}
	 * is also accepted for backward compatibility.
	 * <p>
	 * The Java type of the changeset id is inferred from the type
	 * argument {@code T} in the instantiation of
	 * {@code ChangesetIdentifierSupplier<T>} implemented by the
	 * supplier class, and is used instead of {@link java.time.Instant}
	 * for the effectivity column mappings.
	 * <p>
	 * By default, changeset ids are timestamps generated using
	 * {@link java.time.Instant#now()}.
	 * <p>
	 * This option cannot be used together with
	 * {@value #USE_SERVER_TRANSACTION_TIMESTAMPS}.
	 *
	 * @see org.hibernate.annotations.Temporal
	 * @see org.hibernate.annotations.Audited
	 * @see ChangesetCoordinator
	 * @see ChangesetIdentifierSupplier
	 *
	 * @since 7.4
	 */
	String CHANGESET_ID_SUPPLIER = "hibernate.temporal.changeset_id_supplier";

	/**
	 * Specifies the audit strategy for
	 * {@linkplain org.hibernate.annotations.Audited audited} entities.
	 * <p>
	 * Accepts any of:
	 * <ul>
	 *     <li>an instance of {@link AuditStrategy}, or
	 *     <li>the (case-insensitive) name of an {@code AuditStrategy},
	 *     for example, {@code default} or {@code validity}.
	 * </ul>
	 * <p>
	 * The available strategies are:
	 * <ul>
	 * <li>{@link AuditStrategy#DEFAULT default}: each point-in-time query uses a
	 *     {@code MAX(REV)} subquery to find the current audit row
	 *     (no additional schema requirements), or
	 * <li>{@link AuditStrategy#VALIDITY validity}: each audit row carries a
	 *     {@code REVEND} column marking when it was superseded;
	 *     point-in-time queries use a simple range predicate
	 *     ({@code REV <= :changesetId AND (REVEND > :changesetId OR REVEND IS NULL)})
	 *     instead of a subquery, which is significantly faster for
	 *     large audit tables.
	 * </ul>
	 *
	 * @settingDefault {@link AuditStrategy#DEFAULT}
	 * @see org.hibernate.annotations.Audited
	 * @see AuditStrategy
	 *
	 * @since 7.4
	 */
	@Incubating
	String AUDIT_STRATEGY = "hibernate.audit.strategy";
}
