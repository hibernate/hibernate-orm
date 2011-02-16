/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.cache;
import static org.jboss.logging.Logger.Level.WARN;
import org.hibernate.HibernateLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for this hibernate-ehcache, with IDs ranging from 20001 to 25000 inclusively. New messages
 * must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface EhCacheLogger extends HibernateLogger {

    @LogMessage( level = WARN )
    @Message( value = "Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() between repeated calls to "
                      + "buildSessionFactory. Using previously created EhCacheProvider. If this behaviour is required, consider "
                      + "using net.sf.ehcache.hibernate.SingletonEhCacheProvider.", id = 20001 )
    void attemptToRestartAlreadyStartedEhCacheProvider();

    @LogMessage( level = WARN )
    @Message( value = "Could not find configuration [%s]; using defaults.", id = 20002 )
    void unableToFindConfiguration( String name );

    @LogMessage( level = WARN )
    @Message( value = "Could not find a specific ehcache configuration for cache named [%s]; using defaults.", id = 20003 )
    void unableToFindEhCacheConfiguration( String name );

    @LogMessage( level = WARN )
    @Message( value = "A configurationResourceName was set to %s but the resource could not be loaded from the classpath. Ehcache will configure itself using defaults.", id = 20004 )
    void unableToLoadConfiguration( String configurationResourceName );
}
