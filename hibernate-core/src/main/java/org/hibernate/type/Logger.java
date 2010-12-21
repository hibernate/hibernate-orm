/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.type;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import org.hibernate.engine.SessionFactoryImplementor;
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
    @Message( value = "Binding null to parameter: %d" )
    void bindingToParameter( int index );

    @LogMessage( level = TRACE )
    @Message( value = "Binding '%d' to parameter: %d" )
    void bindingToParameter( int ordinal,
                             int index );

    @LogMessage( level = TRACE )
    @Message( value = "Binding '%s' to parameter: %d" )
    void bindingToParameter( String name,
                             int index );

    @LogMessage( level = TRACE )
    @Message( value = "Returning null as column %s" )
    void returningAsColumn( String string );

    @LogMessage( level = TRACE )
    @Message( value = "Returning '%d' as column %s" )
    void returningAsColumn( int ordinal,
                            String string );

    @LogMessage( level = TRACE )
    @Message( value = "Returning '%s' as column %s" )
    void returningAsColumn( String name,
                            String string );

    @LogMessage( level = TRACE )
    @Message( value = "Scoping types to session factory %s" )
    void scopingTypesToSessionFactory( SessionFactoryImplementor factory );

    @LogMessage( level = WARN )
    @Message( value = "Scoping types to session factory %s after already scoped %s" )
    void scopingTypesToSessionFactoryAfterAlreadyScoped( SessionFactoryImplementor factory,
                                                         SessionFactoryImplementor factory2 );

    @LogMessage( level = INFO )
    @Message( value = "Could not bind value '%s' to parameter: %d; %s" )
    void unableToBindValueToParameter( String nullSafeToString,
                                       int index,
                                       String message );

    @LogMessage( level = INFO )
    @Message( value = "Could not read column value from result set: %s; %s" )
    void unableToReadColumnValueFromResultSet( String name,
                                               String message );
}
