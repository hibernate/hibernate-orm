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
 *
 * @apiNote A transaction id or timestamp is assumed to be constant
 * during a transaction. This service is not aware of transaction
 * scopes, and so it is the responsibility of the client to ensure
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
