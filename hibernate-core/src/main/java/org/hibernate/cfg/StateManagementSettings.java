/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.temporal.TemporalTableStrategy;

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
	 * {@value #TRANSACTION_ID_SUPPLIER}.
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
	 * Specify a {@link java.util.function.Supplier SUpplier} which
	 * provides unique, monotonically increasing transaction IDs for
	 * {@linkplain org.hibernate.annotations.Temporal temporal data}
	 * when using the {@link TemporalTableStrategy#SINGLE_TABLE} or
	 * {@link TemporalTableStrategy#HISTORY_TABLE} mapping strategy
	 * or for {@linkplain org.hibernate.annotations.Audited audited
	 * data}.
	 * <p>
	 * The Java type of the transaction id is inferred from the type
	 * argument {@code T} in the instantiation of {@code Supplier<T>}
	 * implemented by the supplier class, and is used instead of
	 * {@link java.time.Instant} for the effectivity column mappings.
	 * <p>
	 * By default, transaction IDs are timestamps generated using
	 * {@link java.time.Instant#now()}.
	 * <p>
	 * This option cannot be used together with
	 * {@value #USE_SERVER_TRANSACTION_TIMESTAMPS}.
	 *
	 * @see org.hibernate.annotations.Temporal
	 * @see org.hibernate.annotations.Audited
	 * @see org.hibernate.temporal.spi.TransactionIdentifierService
	 *
	 * @since 7.4
	 */
	String TRANSACTION_ID_SUPPLIER = "hibernate.temporal.transaction_id_supplier";
}
