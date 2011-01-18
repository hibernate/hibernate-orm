/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.service.jdbc.connections.internal;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import java.util.Properties;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface C3P0Logger extends BasicLogger {

    @LogMessage( level = INFO )
    @Message( value = "Autocommit mode: %s" )
    void autoCommitMode( boolean autocommit );

    @LogMessage( level = WARN )
    @Message( value = "Both hibernate-style property '%s' and c3p0-style property '%s' have been set in hibernate.properties. "
                      + "Hibernate-style property '%s' will be used and c3p0-style property '%s' will be ignored!" )
    void bothHibernateAndC3p0StylesSet( String hibernateStyle,
                                        String c3p0Style,
                                        String hibernateStyle2,
                                        String c3p0Style2 );

    @LogMessage( level = INFO )
    @Message( value = "C3P0 using driver: %s at URL: %s" )
    void c3p0UsingDriver( String jdbcDriverClass,
                          String jdbcUrl );

    @LogMessage( level = INFO )
    @Message( value = "Connection properties: %s" )
    void connectionProperties( Properties maskOut );

    @Message( value = "JDBC Driver class not found: %s" )
    String jdbcDriverNotFound( String jdbcDriverClass );

    @LogMessage( level = WARN )
    @Message( value = "No JDBC Driver class was specified by property %s" )
    void jdbcDriverNotSpecified( String driver );

    @LogMessage( level = INFO )
    @Message( value = "JDBC isolation level: %s" )
    void jdbcIsolationLevel( String isolationLevelToString );

    @Message( value = "Could not destroy C3P0 connection pool" )
    Object unableToDestroyC3p0ConnectionPool();

    @Message( value = "Could not instantiate C3P0 connection pool" )
    Object unableToInstantiateC3p0ConnectionPool();
}
