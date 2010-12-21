/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.hql.ast.exec;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.sql.SQLWarning;
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
    @Message( value = "Generated ID-INSERT-SELECT SQL (multi-table delete) : %s" )
    void generatedIdInsertSelectDelete( String idInsertSelect );

    @LogMessage( level = TRACE )
    @Message( value = "Generated ID-INSERT-SELECT SQL (multi-table update) : %s" )
    void generatedIdInsertSelectUpdate( String idInsertSelect );

    @LogMessage( level = WARN )
    @Message( value = "Unable to cleanup temporary id table after use [%s]" )
    void unableToCleanupTemporaryIdTable( Throwable t );

    @LogMessage( level = DEBUG )
    @Message( value = "Unable to create temporary id table [%s]" )
    void unableToCreateTemporaryIdTable( String message );

    @LogMessage( level = WARN )
    @Message( value = "Unable to drop temporary id table after use [%s]" )
    void unableToDropTemporaryIdTable( String message );

    @LogMessage( level = WARN )
    @Message( value = "Warnings creating temp table : %s" )
    void warningsCreatingTempTable( SQLWarning warning );
}
