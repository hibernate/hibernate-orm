/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.cache;

import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface EhCacheLogger extends BasicLogger {

    @LogMessage( level = WARN )
    @Message( value = "Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() between repeated calls to "
                      + "buildSessionFactory. Using previously created EhCacheProvider. If this behaviour is required, consider "
                      + "using net.sf.ehcache.hibernate.SingletonEhCacheProvider." )
    void attemptToRestartAlreadyStartedEhCacheProvider();

    @LogMessage( level = WARN )
    @Message( value = "Could not find configuration [%s]; using defaults." )
    void unableToFindConfiguration( String name );
}
