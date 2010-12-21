/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.engine.jdbc.batch.internal;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
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

    @LogMessage( level = INFO )
    @Message( value = "On release of batch it still contained JDBC statements" )
    void batchContainedStatementsOnRelease();

    @LogMessage( level = TRACE )
    @Message( value = "building batch [size=%d]" )
    void buildingBatch( int size );

    @LogMessage( level = DEBUG )
    @Message( value = "Executing batch size: %d" )
    void executingBatchSize( int batchPosition );

    @LogMessage( level = DEBUG )
    @Message( value = "No batched statements to execute" )
    void noBatchesToExecute();

    @LogMessage( level = DEBUG )
    @Message( value = "Reusing batch statement" )
    void reusingBatchStatement();

    @LogMessage( level = ERROR )
    @Message( value = "SqlException escaped proxy : %s" )
    void sqlExceptionEscapedProxy( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Exception executing batch [%s]" )
    void unableToExecuteBatch( String message );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to release batch statement..." )
    void unableToReleaseBatchStatement();

    @LogMessage( level = WARN )
    @Message( value = "JDBC driver did not return the expected number of row counts" )
    void unexpectedRowCounts();
}
