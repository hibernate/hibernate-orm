/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.cache.impl.bridge;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
interface Logger {

    @LogMessage( level = INFO )
    @Message( value = "Cache provider: %s" )
    void cacheProvider( String name );

    @LogMessage( level = WARN )
    @Message( value = "read-only cache configured for mutable collection [%s]" )
    void readOnlyCacheConfiguredForMutableCollection( String name );
}
