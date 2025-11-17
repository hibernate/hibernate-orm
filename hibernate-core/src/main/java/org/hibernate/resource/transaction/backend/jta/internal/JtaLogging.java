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
import static org.jboss.logging.Logger.Level.DEBUG;

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

	@LogMessage(level = TRACE)
	@Message(
			value = "JTA platform says we cannot currently register synchronization; skipping",
			id = NAMESPACE + 32
	)
	void cannotRegisterSynchronization();

	@LogMessage(level = TRACE)
	@Message(
			value = "Hibernate RegisteredSynchronization successfully registered with JTA platform",
			id = NAMESPACE + 33
	)
	void registeredSynchronization();

	@LogMessage(level = TRACE)
	@Message(
			value = "JTA transaction was already joined (RegisteredSynchronization already registered)",
			id = NAMESPACE + 34
	)
	void alreadyJoinedJtaTransaction();

	@LogMessage(level = TRACE)
	@Message(
			value = "Notifying JTA transaction observers before completion",
			id = NAMESPACE + 35
	)
	void notifyingJtaObserversBeforeCompletion();

	@LogMessage(level = TRACE)
	@Message(
			value = "Notifying JTA transaction observers after completion",
			id = NAMESPACE + 36
	)
	void notifyingJtaObserversAfterCompletion();

	@LogMessage(level = TRACE)
	@Message(
			value = "Registered JTA Synchronization: beforeCompletion()",
			id = NAMESPACE + 37
	)
	void registeredSynchronizationBeforeCompletion();

	@LogMessage(level = TRACE)
	@Message(
			value = "Registered JTA Synchronization: afterCompletion(%s)",
			id = NAMESPACE + 38
	)
	void registeredSynchronizationAfterCompletion(int status);

	@LogMessage(level = TRACE)
	@Message(
			value = "Synchronization coordinator: beforeCompletion()",
			id = NAMESPACE + 39
	)
	void synchronizationCoordinatorBeforeCompletion();

	@LogMessage(level = TRACE)
	@Message(
			value = "Synchronization coordinator: afterCompletion(status=%s)",
			id = NAMESPACE + 40
	)
	void synchronizationCoordinatorAfterCompletion(int status);

	@LogMessage(level = TRACE)
	@Message(
			value = "Synchronization coordinator: doAfterCompletion(successful=%s, delayed=%s)",
			id = NAMESPACE + 41
	)
	void synchronizationCoordinatorDoAfterCompletion(boolean successful, boolean delayed);
	@LogMessage(level = DEBUG)
	@Message(
			value = "Unable to access TransactionManager, attempting to use UserTransaction instead",
			id = NAMESPACE + 42
	)
	void unableToAccessTransactionManagerTryingUserTransaction();

	@LogMessage(level = DEBUG)
	@Message(
			value = "Unable to access UserTransaction, attempting to use TransactionManager instead",
			id = NAMESPACE + 43
	)
	void unableToAccessUserTransactionTryingTransactionManager();

	@LogMessage(level = DEBUG)
	@Message(
			value = "JtaPlatform.retrieveUserTransaction() returned null",
			id = NAMESPACE + 44
	)
	void userTransactionReturnedNull();

	@LogMessage(level = DEBUG)
	@Message(
			value = "JtaPlatform.retrieveUserTransaction() threw an exception [%s]",
			id = NAMESPACE + 45
	)
	void exceptionRetrievingUserTransaction(String message, @Cause Exception cause);

	@LogMessage(level = DEBUG)
	@Message(
			value = "JtaPlatform.retrieveTransactionManager() returned null",
			id = NAMESPACE + 46
	)
	void transactionManagerReturnedNull();

	@LogMessage(level = DEBUG)
	@Message(
			value = "JtaPlatform.retrieveTransactionManager() threw an exception [%s]",
			id = NAMESPACE + 47
	)
	void exceptionRetrievingTransactionManager(String message, @Cause Exception cause);
}
