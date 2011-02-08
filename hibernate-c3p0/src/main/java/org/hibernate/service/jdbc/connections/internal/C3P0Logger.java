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
import java.sql.SQLException;
import java.util.Properties;
import org.hibernate.HibernateLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;

/**
 * Defines internationalized messages for this hibernate-c3p0, with IDs ranging from 10001 to 15000 inclusively. New messages must
 * be added after the last message defined to ensure message codes are unique.
 */
public interface C3P0Logger extends HibernateLogger {

    @LogMessage( level = INFO )
    @Message( value = "Autocommit mode: %s", id = 10001 )
    void autoCommitMode( boolean autocommit );

    @LogMessage( level = WARN )
    @Message( value = "Both hibernate-style property '%s' and c3p0-style property '%s' have been set in hibernate.properties. "
                      + "Hibernate-style property '%s' will be used and c3p0-style property '%s' will be ignored!", id = 10002 )
    void bothHibernateAndC3p0StylesSet( String hibernateStyle,
                                        String c3p0Style,
                                        String hibernateStyle2,
                                        String c3p0Style2 );

    @LogMessage( level = INFO )
    @Message( value = "C3P0 using driver: %s at URL: %s", id = 10003 )
    void c3p0UsingDriver( String jdbcDriverClass,
                          String jdbcUrl );

    @LogMessage( level = INFO )
    @Message( value = "Connection properties: %s", id = 10004 )
    void connectionProperties( Properties maskOut );

    @Message( value = "JDBC Driver class not found: %s", id = 10005 )
    String jdbcDriverNotFound( String jdbcDriverClass );

    @LogMessage( level = WARN )
    @Message( value = "No JDBC Driver class was specified by property %s", id = 10006 )
    void jdbcDriverNotSpecified( String driver );

    @LogMessage( level = INFO )
    @Message( value = "JDBC isolation level: %s", id = 10007 )
    void jdbcIsolationLevel( String isolationLevelToString );

    @LogMessage( level = WARN )
    @Message( value = "Could not destroy C3P0 connection pool", id = 10008 )
    void unableToDestroyC3p0ConnectionPool( @Cause SQLException e );

    @Message( value = "Could not instantiate C3P0 connection pool", id = 10009 )
    String unableToInstantiateC3p0ConnectionPool();
}
