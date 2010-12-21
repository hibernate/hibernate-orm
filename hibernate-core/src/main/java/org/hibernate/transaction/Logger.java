/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.transaction;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger extends BasicLogger {

    @LogMessage( level = TRACE )
    @Message( value = "Attempting to locate UserTransaction via JNDI [%s]" )
    void attemptingToLocateUserTransactionViaJndi( String utName );

    @LogMessage( level = TRACE )
    @Message( value = "Automatically closing session" )
    void automaticallyClosingSession();

    @LogMessage( level = TRACE )
    @Message( value = "Automatically flushing session" )
    void automaticallyFlushingSession();

    @LogMessage( level = DEBUG )
    @Message( value = "Began a new JTA transaction" )
    void beganNewJtaTransaction();

    @LogMessage( level = DEBUG )
    @Message( value = "Begin" )
    void begin();

    @LogMessage( level = DEBUG )
    @Message( value = "Commit" )
    void commit();

    @LogMessage( level = DEBUG )
    @Message( value = "Committed JDBC Connection" )
    void commitedJdbcConnection();

    @LogMessage( level = DEBUG )
    @Message( value = "Committed JTA UserTransaction" )
    void commitedJtaUserTransaction();

    @LogMessage( level = TRACE )
    @Message( value = "Configured JTATransactionFactory to use [%s] for UserTransaction JDNI namespace" )
    void configuredJtaTransactionFactoryForUserTransactionJndiNamespace( String userTransactionName );

    @LogMessage( level = DEBUG )
    @Message( value = "current autocommit status: %s" )
    void currentAutoCommitStatus( boolean toggleAutoCommit );

    @LogMessage( level = DEBUG )
    @Message( value = "Disabling autocommit" )
    void disablingAutoCommit();

    @LogMessage( level = INFO )
    @Message( value = "Instantiated TransactionManagerLookup" )
    void instantiatedTransactionManagerLookup();

    @LogMessage( level = INFO )
    @Message( value = "Instantiating TransactionManagerLookup: %s" )
    void instantiatingTransactionManagerLookup( String tmLookupClass );

    @Message( value = "JDBC begin failed" )
    String jdbcBeginFailed();

    @Message( value = "JDBC rollback failed" )
    String jdbcRollbackFailed();

    @LogMessage( level = DEBUG )
    @Message( value = "Set JTA UserTransaction to rollback only" )
    void jtaUserTransactionSetToRollbackOnly();

    @LogMessage( level = WARN )
    @Message( value = "You should set hibernate.transaction.manager_lookup_class if cache is enabled" )
    void managerLookupClassShouldBeSet();

    @LogMessage( level = TRACE )
    @Message( value = "Obtained UserTransaction" )
    void obtainedUserTransaction();

    @LogMessage( level = INFO )
    @Message( value = "Obtaining TransactionManager" )
    void obtainingTransactionManager();

    @LogMessage( level = DEBUG )
    @Message( value = "Re-enabling autocommit" )
    void reenablingAutoCommit();

    @LogMessage( level = DEBUG )
    @Message( value = "Rollback" )
    void rollback();

    @LogMessage( level = DEBUG )
    @Message( value = "Rolled back JDBC Connection" )
    void rolledBackJdbcConnection();

    @LogMessage( level = DEBUG )
    @Message( value = "Rolled back JTA UserTransaction" )
    void rolledBackJtaUserTransaction();

    @LogMessage( level = TRACE )
    @Message( value = "Transaction after completion callback, status: %d" )
    void transactionAfterCompletionCallback( int status );

    @LogMessage( level = TRACE )
    @Message( value = "Transaction before completion callback" )
    void transactionBeforeCompletionCallback();

    @Message( value = "TransactionFactory class not found" )
    String transactionFactoryClassNotFound();

    @Message( value = "TransactionFactory class not found: %s" )
    String transactionFactoryClassNotFound( String strategyClassName );

    @LogMessage( level = INFO )
    @Message( value = "No TransactionManagerLookup configured (in JTA environment, use of read-write or transactional second-level cache is not recommended)" )
    void transactionManagerLookupNotConfigured();

    @LogMessage( level = INFO )
    @Message( value = "Transaction strategy: %s" )
    void transactionStrategy( String strategyClassName );

    @Message( value = "JTA transaction begin failed" )
    String unableToBeginJtaTransaction();

    @Message( value = "Could not close session" )
    String unableToCloseSession();

    @Message( value = "Could not close session during rollback" )
    String unableToCloseSessionDuringRollback();

    @Message( value = "JTA commit failed" )
    String unableToCommitJta();

    @Message( value = "Could not determine transaction status" )
    String unableToDetermineTransactionStatus();

    @Message( value = "Could not determine transaction status after commit" )
    String unableToDetermineTransactionStatusAfterCommit();

    @Message( value = "Failed to instantiate TransactionFactory" )
    String unableToInstantiateTransactionFactory();

    @Message( value = "Failed to instantiate TransactionFactory: %s" )
    String unableToInstantiateTransactionFactory( Exception error );

    @Message( value = "Failed to instantiate TransactionManagerLookup" )
    String unableToInstantiateTransactionManagerLookup();

    @Message( value = "Failed to instantiate TransactionManagerLookup '%s'" )
    String unableToInstantiateTransactionManagerLookup( String tmLookupClass );

    @Message( value = "JDBC commit failed" )
    String unableToPerformJdbcCommit();

    @Message( value = "JTA rollback failed" )
    String unableToRollbackJta();

    @Message( value = "Could not set transaction to rollback only" )
    String unableToSetTransactionToRollbackOnly();

    @Message( value = "Could not toggle autocommit" )
    Object unableToToggleAutoCommit();

    @LogMessage( level = INFO )
    @Message( value = "Using default transaction strategy (direct JDBC transactions)" )
    void usingDefaultTransactionStrategy();
}
