/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal;

import jakarta.transaction.Transaction;
import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging interface for JTA transaction operations.
 *
 * @author Gavin King
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90007001, max = 90008000)
@SubSystemLogging(
		name = JtaLogging.LOGGER_NAME,
		description = "Logging related to JTA transaction management"
)
@Internal
public interface JtaLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".jta";

	JtaLogging JTA_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JtaLogging.class, LOGGER_NAME );

	int NAMESPACE = 90007000;

	// TransactionManager methods

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling TransactionManager.begin() to start a new JTA transaction",
			id = NAMESPACE + 1
	)
	void callingTransactionManagerBegin();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called TransactionManager.begin()",
			id = NAMESPACE + 2
	)
	void calledTransactionManagerBegin();

	@LogMessage(level = TRACE)
	@Message(
			value = "Skipping TransactionManager.begin() since there is an active transaction",
			id = NAMESPACE + 3
	)
	void skippingTransactionManagerBegin();

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling TransactionManager.commit() to commit the JTA transaction",
			id = NAMESPACE + 4
	)
	void callingTransactionManagerCommit();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called TransactionManager.commit()",
			id = NAMESPACE + 5
	)
	void calledTransactionManagerCommit();

	@LogMessage(level = TRACE)
	@Message(
			value = "Skipping TransactionManager.commit() since the transaction was not initiated here",
			id = NAMESPACE + 6
	)
	void skippingTransactionManagerCommit();

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling TransactionManager.rollback() to roll back the JTA transaction",
			id = NAMESPACE + 7
	)
	void callingTransactionManagerRollback();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called TransactionManager.rollback()",
			id = NAMESPACE + 8
	)
	void calledTransactionManagerRollback();

	// UserTransaction methods

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling UserTransaction.begin() to start a new JTA transaction",
			id = NAMESPACE + 9
	)
	void callingUserTransactionBegin();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called UserTransaction.begin()",
			id = NAMESPACE + 10
	)
	void calledUserTransactionBegin();

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling UserTransaction.commit() to commit the JTA transaction",
			id = NAMESPACE + 11
	)
	void callingUserTransactionCommit();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called UserTransaction.commit()",
			id = NAMESPACE + 12
	)
	void calledUserTransactionCommit();

	@LogMessage(level = TRACE)
	@Message(
			value = "Calling UserTransaction.rollback() to roll back the JTA transaction",
			id = NAMESPACE + 13
	)
	void callingUserTransactionRollback();

	@LogMessage(level = TRACE)
	@Message(
			value = "Successfully called UserTransaction.rollback()",
			id = NAMESPACE + 14
	)
	void calledUserTransactionRollback();

	@LogMessage(level = TRACE)
	@Message(
			value = "Surrounding JTA transaction suspended [%s]",
			id = NAMESPACE + 15
	)
	void transactionSuspended(Object transaction);

	@LogMessage(level = TRACE)
	@Message(
			value = "Surrounding JTA transaction resumed [%s]",
			id = NAMESPACE + 16
	)
	void transactionResumed(Object transaction);

	@LogMessage(level = Logger.Level.INFO)
	@Message(
			value = "Unable to roll back isolated transaction on error [%s]",
			id = NAMESPACE + 17
	)
	void unableToRollBackIsolatedTransaction(Exception original, @Cause Exception ignore);

	@LogMessage(level = Logger.Level.INFO)
	@Message(
			value = "Unable to release isolated connection",
			id = NAMESPACE + 18
	)
	void unableToReleaseIsolatedConnection(@Cause Throwable ignore);

	@LogMessage(level = WARN)
	@Message(
			id = NAMESPACE + 20,
			value = "Transaction afterCompletion called by a background thread; " +
					"delaying afterCompletion processing until the original thread can handle it. [status=%s]"
	)
	void rollbackFromBackgroundThread(int status);

	@LogMessage(level = TRACE)
	@Message(
			value = "Suspended transaction to isolate DDL execution [%s]",
			id = NAMESPACE + 30
	)
	void suspendedTransactionForDdlIsolation(Transaction suspendedTransaction);

	@LogMessage(level = TRACE)
	@Message(
			value = "Resumed transaction after isolated DDL execution",
			id = NAMESPACE + 31
	)
	void resumedTransactionForDdlIsolation();
}
