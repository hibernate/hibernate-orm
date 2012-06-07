/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.jdbc.connections.internal;

import java.sql.SQLException;

import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import org.hibernate.internal.CoreMessageLogger;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-c3p0 module.  It reserves message ids ranging from
 * 10001 to 15000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface C3P0MessageLogger extends CoreMessageLogger {

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
