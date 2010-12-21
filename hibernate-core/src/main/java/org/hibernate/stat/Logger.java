/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.stat;

import static org.jboss.logging.Logger.Level.INFO;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger extends BasicLogger {

    @LogMessage( level = INFO )
    @Message( value = "Collections fetched (minimize this): %ld" )
    void collectionsFetched( long collectionFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections fetched (minimize this): %s" )
    void collectionsFetched( AtomicLong collectionFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections loaded: %ld" )
    void collectionsLoaded( long collectionLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections loaded: %s" )
    void collectionsLoaded( AtomicLong collectionLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections recreated: %ld" )
    void collectionsRecreated( long collectionRecreateCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections recreated: %s" )
    void collectionsRecreated( AtomicLong collectionRecreateCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections removed: %ld" )
    void collectionsRemoved( long collectionRemoveCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections removed: %s" )
    void collectionsRemoved( AtomicLong collectionRemoveCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections updated: %ld" )
    void collectionsUpdated( long collectionUpdateCount );

    @LogMessage( level = INFO )
    @Message( value = "Collections updated: %s" )
    void collectionsUpdated( AtomicLong collectionUpdateCount );

    @LogMessage( level = INFO )
    @Message( value = "Connections obtained: %ld" )
    void connectionsObtained( long connectCount );

    @LogMessage( level = INFO )
    @Message( value = "Connections obtained: %s" )
    void connectionsObtained( AtomicLong connectCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities deleted: %ld" )
    void entitiesDeleted( long entityDeleteCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities deleted: %s" )
    void entitiesDeleted( AtomicLong entityDeleteCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities fetched (minimize this): %ld" )
    void entitiesFetched( long entityFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities fetched (minimize this): %s" )
    void entitiesFetched( AtomicLong entityFetchCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities inserted: %ld" )
    void entitiesInserted( long entityInsertCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities inserted: %s" )
    void entitiesInserted( AtomicLong entityInsertCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities loaded: %ld" )
    void entitiesLoaded( long entityLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities loaded: %s" )
    void entitiesLoaded( AtomicLong entityLoadCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities updated: %ld" )
    void entitiesUpdated( long entityUpdateCount );

    @LogMessage( level = INFO )
    @Message( value = "Entities updated: %s" )
    void entitiesUpdated( AtomicLong entityUpdateCount );

    @LogMessage( level = INFO )
    @Message( value = "Flushes: %ld" )
    void flushes( long flushCount );

    @LogMessage( level = INFO )
    @Message( value = "Flushes: %s" )
    void flushes( AtomicLong flushCount );

    @LogMessage( level = INFO )
    @Message( value = "HQL: %s, time: %sms, rows: %s" )
    void hql( String hql,
              Long valueOf,
              Long valueOf2 );

    @LogMessage( level = INFO )
    @Message( value = "Logging statistics...." )
    void loggingStatistics();

    @LogMessage( level = INFO )
    @Message( value = "Max query time: %ldms" )
    void maxQueryTime( long queryExecutionMaxTime );

    @LogMessage( level = INFO )
    @Message( value = "Max query time: %sms" )
    void maxQueryTime( AtomicLong queryExecutionMaxTime );

    @LogMessage( level = INFO )
    @Message( value = "Optimistic lock failures: %ld" )
    void optimisticLockFailures( long optimisticFailureCount );

    @LogMessage( level = INFO )
    @Message( value = "Optimistic lock failures: %s" )
    void optimisticLockFailures( AtomicLong optimisticFailureCount );

    @LogMessage( level = INFO )
    @Message( value = "Queries executed to database: %ld" )
    void queriesExecuted( long queryExecutionCount );

    @LogMessage( level = INFO )
    @Message( value = "Queries executed to database: %s" )
    void queriesExecuted( AtomicLong queryExecutionCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache hits: %ld" )
    void queryCacheHits( long queryCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache hits: %s" )
    void queryCacheHits( AtomicLong queryCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache misses: %ld" )
    void queryCacheMisses( long queryCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache misses: %s" )
    void queryCacheMisses( AtomicLong queryCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache puts: %ld" )
    void queryCachePuts( long queryCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Query cache puts: %s" )
    void queryCachePuts( AtomicLong queryCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache hits: %ld" )
    void secondLevelCacheHits( long secondLevelCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache hits: %s" )
    void secondLevelCacheHits( AtomicLong secondLevelCacheHitCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache misses: %ld" )
    void secondLevelCacheMisses( long secondLevelCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache misses: %s" )
    void secondLevelCacheMisses( AtomicLong secondLevelCacheMissCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache puts: %ld" )
    void secondLevelCachePuts( long secondLevelCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Second level cache puts: %s" )
    void secondLevelCachePuts( AtomicLong secondLevelCachePutCount );

    @LogMessage( level = INFO )
    @Message( value = "Sessions closed: %ld" )
    void sessionsClosed( long sessionCloseCount );

    @LogMessage( level = INFO )
    @Message( value = "Sessions closed: %s" )
    void sessionsClosed( AtomicLong sessionCloseCount );

    @LogMessage( level = INFO )
    @Message( value = "Sessions opened: %ld" )
    void sessionsOpened( long sessionOpenCount );

    @LogMessage( level = INFO )
    @Message( value = "Sessions opened: %s" )
    void sessionsOpened( AtomicLong sessionOpenCount );

    @LogMessage( level = INFO )
    @Message( value = "Start time: %s" )
    void startTime( long startTime );

    @LogMessage( level = INFO )
    @Message( value = "Statements closed: %ld" )
    void statementsClosed( long closeStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Statements closed: %s" )
    void statementsClosed( AtomicLong closeStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Statements prepared: %ld" )
    void statementsPrepared( long prepareStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Statements prepared: %s" )
    void statementsPrepared( AtomicLong prepareStatementCount );

    @LogMessage( level = INFO )
    @Message( value = "Successful transactions: %ld" )
    void successfulTransactions( long committedTransactionCount );

    @LogMessage( level = INFO )
    @Message( value = "Successful transactions: %s" )
    void successfulTransactions( AtomicLong committedTransactionCount );

    @LogMessage( level = INFO )
    @Message( value = "Transactions: %ld" )
    void transactions( long transactionCount );

    @LogMessage( level = INFO )
    @Message( value = "Transactions: %s" )
    void transactions( AtomicLong transactionCount );
}
