/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

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

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT;
import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to logical JDBC connection handling in the resource.jdbc.internal package
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 10003001, max = 10003100)
@SubSystemLogging(
		name = LogicalConnectionLogging.LOGGER_NAME,
		description = "Logging related to logical JDBC connection handling"
)
@Internal
public interface LogicalConnectionLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".resource.jdbc";
	LogicalConnectionLogging CONNECTION_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(), LogicalConnectionLogging.class, LOGGER_NAME
	);

	// ---- TRACE: lifecycle around JDBC transactions on logical connection ----
	@LogMessage(level = TRACE)
	@Message(id = 10003001, value = "Preparing to begin transaction via JDBC Connection.setAutoCommit(false)")
	void preparingToBeginViaSetAutoCommitFalse();

	@LogMessage(level = TRACE)
	@Message(id = 10003002, value = "Transaction begun via JDBC Connection.setAutoCommit(false)")
	void transactionBegunViaSetAutoCommitFalse();

	@LogMessage(level = TRACE)
	@Message(id = 10003003, value = "Preparing to commit transaction via JDBC Connection.commit()")
	void preparingToCommitViaConnectionCommit();

	@LogMessage(level = TRACE)
	@Message(id = 10003004, value = "Transaction committed via JDBC Connection.commit()")
	void transactionCommittedViaConnectionCommit();

	@LogMessage(level = TRACE)
	@Message(id = 10003005, value = "Preparing to roll back transaction via JDBC Connection.rollback()")
	void preparingToRollbackViaConnectionRollback();

	@LogMessage(level = TRACE)
	@Message(id = 10003006, value = "Transaction rolled back via JDBC Connection.rollback()")
	void transactionRolledBackViaConnectionRollback();

	@LogMessage(level = TRACE)
	@Message(id = 10003007, value = "Re-enabling auto-commit on JDBC Connection after completion of JDBC-based transaction")
	void reenablingAutoCommitAfterJdbcTransaction();

	// ---- DEBUG/TRACE for auto-commit reset issues ----
	@LogMessage(level = DEBUG)
	@Message(id = 10003008, value = "Could not re-enable auto-commit on JDBC Connection after completion of JDBC-based transaction")
	void couldNotReEnableAutoCommit(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 10003009, value = "Unable to ascertain initial auto-commit state of provided connection; assuming auto-commit")
	void unableToAscertainInitialAutoCommit();

	// ---- TRACE around manual reconnect in provided logical connection ----
	@LogMessage(level = TRACE)
	@Message(id = 10003010, value = "Reconnecting the same connection that is already connected; should this connection have been disconnected?")
	void reconnectingSameConnectionAlreadyConnected();

	@LogMessage(level = TRACE)
	@Message(id = 10003011, value = "Manually reconnected logical connection")
	void manuallyReconnectedLogicalConnection();

	// ---- TRACE around connection release timing in managed logical connection ----
	@LogMessage(level = TRACE)
	@Message(id = 10003012, value = "Skipping aggressive release of JDBC connection @%s from 'afterStatement' due to held resources")
	void skipConnectionReleaseAfterStatementDueToResources(int hashCode);

	@LogMessage(level = TRACE)
	@Message(id = 10003013, value = "Initiating release of JDBC connection @%s from 'afterStatement'")
	void initiatingConnectionReleaseAfterStatement(int hashCode);

	@LogMessage(level = TRACE)
	@Message(id = 10003014, value = "Initiating release of JDBC connection @%s from 'beforeTransactionCompletion'")
	void initiatingConnectionReleaseBeforeTransactionCompletion(int hashCode);

	@LogMessage(level = TRACE)
	@Message(id = 10003015, value = "Initiating release of JDBC connection @%s from 'afterTransaction'")
	void initiatingConnectionReleaseAfterTransaction(int hashCode);

	@LogMessage(level = WARN)
	@Message(id = 10003016, value = "Error before releasing JDBC connection @%s")
	void errorBeforeReleasingJdbcConnection(int hashCode, @Cause Throwable e);

	@LogMessage(level = TRACE)
	@Message(id = 10003017, value = "Closing logical connection @%s")
	void closingLogicalConnection(int hashCode);

	@LogMessage(level = TRACE)
	@Message(id = 10003018, value = "Closed logical connection @%s")
	void logicalConnectionClosed(int hashCode);

	// Variants without hashCode used by provided logical connection
	@LogMessage(level = TRACE)
	@Message(id = 10003019, value = "Closing logical connection")
	void closingLogicalConnection();

	@LogMessage(level = TRACE)
	@Message(id = 10003020, value = "Closed logical connection")
	void logicalConnectionClosed();

	@LogMessage(level = DEBUG)
	@Message(
			id = 10003030,
			value =
					"'" + CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT + "' " +
					"""
					was enabled. This setting should only be enabled when JDBC Connections obtained by Hibernate \
					from the ConnectionProvider have auto-commit disabled. Enabling this setting when connections \
					have auto-commit enabled leads to execution of SQL operations outside of any JDBC transaction.\
					"""
	)
	void connectionProviderDisablesAutoCommitEnabled();
}
