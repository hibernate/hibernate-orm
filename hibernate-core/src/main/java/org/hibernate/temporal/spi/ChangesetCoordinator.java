/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.spi;

import org.hibernate.Incubating;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.service.Service;


/**
 * A source of changeset identifiers or timestamps for use
 * with {@linkplain org.hibernate.annotations.Temporal temporal}
 * or {@linkplain org.hibernate.annotations.Audited audited} data.
 * <p>
 * Changeset ids produced by the {@linkplain #getIdentifierSupplier
 * supplier} must be distinct and monotonically increasing. Note,
 * however, that unless the database is in serializable isolation
 * mode, transactions themselves do not have a well-defined total
 * order. Therefore, the changeset ids are ordered by the moment
 * at which they are obtained; approximately, by the instant at
 * which the transaction <em>started</em>. This must be taken into
 * account when interpreting the results of historical queries
 * against a temporal table or audit log. The historical changes
 * cannot be linearized by transaction completion time, since the
 * completion time of a transaction is never known when the records
 * are written to the history table or audit log.
 *
 * @apiNote A changeset id or timestamp is assumed to be constant
 * during a transaction. This service is not aware of transaction
 * contexts, and so it is the responsibility of the client to ensure
 * that the {@linkplain #getIdentifierSupplier supplier} is called
 * no more than once in a transaction.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Incubating
public interface ChangesetCoordinator extends Service {
	/**
	 * The Java type of the transaction identifiers or timestamps.
	 */
	Class<?> getIdentifierType();

	/**
	 * A supplier of changeset identifiers or timestamps.
	 * The current session is passed to the supplier.
	 *
	 * @see StateManagementSettings#CHANGESET_ID_SUPPLIER
	 */
	ChangesetIdentifierSupplier<?> getIdentifierSupplier();

	/**
	 * Whether the stable timestamps assigned by the database server
	 * so that multiple calls to {@code current_timestamp} in the
	 * same transaction return the same value. When this is true,
	 * we don't need to pass the transaction timestamp via a JDBC
	 * parameter, and we don't need to cache its value in the session.
	 *
	 * @see StateManagementSettings#USE_SERVER_TRANSACTION_TIMESTAMPS
	 * @see Dialect#isCurrentTimestampStable()
	 */
	boolean useServerTimestamp(Dialect dialect);

	/**
	 * Whether the transaction identifiers are actually timestamps.
	 */
	boolean isIdentifierTypeInstant();

	/**
	 * Programmatically contribute a {@link ChangesetIdentifierSupplier},
	 * overriding any previously configured supplier.
	 * <p>
	 * Called during bootstrap (e.g. from the session factory) when
	 * a supplier is derived from metadata rather than from explicit
	 * configuration.
	 *
	 * @param supplier the supplier to use
	 * @param identifierType the Java type of transaction identifiers
	 *        produced by the supplier
	 */
	default <T> void contributeIdentifierSupplier(ChangesetIdentifierSupplier<T> supplier, Class<T> identifierType) {
	}
}
