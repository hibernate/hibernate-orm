/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import java.util.function.Supplier;


/**
 * A source of transaction identifiers or timestamps for use
 * with {@linkplain org.hibernate.annotations.Temporal temporal}
 * or {@linkplain org.hibernate.annotations.Audited audited} data.
 * <p>
 * Transaction ids produced by the {@linkplain #getIdentifierSupplier
 * supplier} must be distinct and monotonically increasing. Note,
 * however, that unless the database is in serializable isolation
 * mode, transactions themselves do not have a well-defined total
 * order. Therefore, the transaction ids are ordered by the moment
 * at which they are obtained; approximately, by the instant at
 * which the transaction <em>started</em>. This must be taken into
 * account when interpreting the results of historical queries
 * against a temporal table or audit log. The historical changes
 * cannot be linearized by transaction completion time, since the
 * completion time of a transaction is never known when the records
 * are written to the history table or audit log.
 *
 * @apiNote A transaction id or timestamp is assumed to be constant
 * during a transaction. This service is not aware of transaction
 * contexts, and so it is the responsibility of the client to ensure
 * that the {@linkplain #getIdentifierSupplier supplier} is called
 * no more than once in a transaction.
 *
 * @author Gavin King
 */
public interface TransactionIdentifierService extends Service {
	/**
	 * The Java type of the transaction identifiers or timestamps.
	 */
	Class<?> getIdentifierType();

	/**
	 * A supplier of transaction identifiers or timestamps.
	 *
	 * @see org.hibernate.cfg.MappingSettings#TRANSACTION_ID_SUPPLIER
	 */
	Supplier<?> getIdentifierSupplier();

	/**
	 * Whether the timestamps or identifiers are assigned by the database server.
	 *
	 * @see org.hibernate.cfg.MappingSettings#USE_SERVER_TRANSACTION_TIMESTAMPS
	 */
	boolean isDisabled();

	/**
	 * Whether the transaction identifiers are actually timestamps.
	 */
	boolean isIdentifierTypeInstant();
}
