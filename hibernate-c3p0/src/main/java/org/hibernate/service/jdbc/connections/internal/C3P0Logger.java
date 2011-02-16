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
import org.hibernate.HibernateLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for this hibernate-c3p0, with IDs ranging from 10001 to 15000 inclusively. New messages must
 * be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface C3P0Logger extends HibernateLogger {

    @LogMessage( level = WARN )
    @Message( value = "Both hibernate-style property '%s' and c3p0-style property '%s' have been set in hibernate.properties. "
                      + "Hibernate-style property '%s' will be used and c3p0-style property '%s' will be ignored!", id = 10001 )
    void bothHibernateAndC3p0StylesSet( String hibernateStyle,
                                        String c3p0Style,
                                        String hibernateStyle2,
                                        String c3p0Style2 );

    @LogMessage( level = INFO )
    @Message( value = "C3P0 using driver: %s at URL: %s", id = 10002 )
    void c3p0UsingDriver( String jdbcDriverClass,
                          String jdbcUrl );

    @Message( value = "JDBC Driver class not found: %s", id = 10003 )
    String jdbcDriverNotFound( String jdbcDriverClass );

    @LogMessage( level = WARN )
    @Message( value = "Could not destroy C3P0 connection pool", id = 10004 )
    void unableToDestroyC3p0ConnectionPool( @Cause SQLException e );

    @Message( value = "Could not instantiate C3P0 connection pool", id = 10005 )
    String unableToInstantiateC3p0ConnectionPool();
}
