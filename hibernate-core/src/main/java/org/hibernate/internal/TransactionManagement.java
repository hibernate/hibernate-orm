/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.Transaction;
import org.hibernate.TransactionManagementException;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Methods used by {@link org.hibernate.SessionFactory} to manage transactions.
 */
public class TransactionManagement {

	public static <S> void manageTransaction(S session, Transaction transaction, Consumer<S> consumer) {
		try {
			consumer.accept( session );
			commit( transaction );
		}
		catch ( RuntimeException exception ) {
			rollback( transaction, exception );
			throw exception;
		}
	}

	public static <S,R> R manageTransaction(S session, Transaction transaction, Function<S,R> function) {
		try {
			R result = function.apply( session );
			commit( transaction );
			return result;
		}
		catch ( RuntimeException exception ) {
			// an error happened in the action or during commit()
			rollback( transaction, exception );
			throw exception;
		}
	}

	private static void rollback(Transaction transaction, RuntimeException exception) {
		// an error happened in the action or during commit()
		if ( transaction.isActive() ) {
			try {
				transaction.rollback();
			}
			catch ( RuntimeException e ) {
				exception.addSuppressed( e );
			}
		}
	}

	private static void commit(Transaction transaction) {
		if ( !transaction.isActive() ) {
			throw new TransactionManagementException(
					"Execution of action caused managed transaction to be completed" );
		}
		// The action completed without throwing an exception,
		// so we attempt to commit the transaction, allowing
		// any RollbackException to propagate. Note that when
		// we get here we know that the transaction is active
		transaction.commit();
	}

}
