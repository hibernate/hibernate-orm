/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.ejb;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import java.net.URISyntaxException;
import java.net.URL;
import org.hibernate.HibernateLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;

/**
 * Defines internationalized messages for this hibernate-entitymanager, with IDs ranging from 15001 to 20000 inclusively. New
 * messages must be added after the last message defined to ensure message codes are unique.
 */
public interface EntityManagerLogger extends HibernateLogger {

    @LogMessage( level = INFO )
    @Message( value = "Bound Ejb3Configuration to JNDI name: %s", id = 15001 )
    void boundEjb3ConfigurationToJndiName( String name );

    @LogMessage( level = INFO )
    @Message( value = "Ejb3Configuration name: %s", id = 15002 )
    void ejb3ConfigurationName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was renamed from name: %s", id = 15003 )
    void ejb3ConfigurationRenamedFromName( String name );

    @LogMessage( level = INFO )
    @Message( value = "An Ejb3Configuration was unbound from name: %s", id = 15004 )
    void ejb3ConfigurationUnboundFromName( String name );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file does not exist (ignored): %s", id = 15005 )
    void explodedJarDoesNotExist( URL jarUrl );

    @LogMessage( level = WARN )
    @Message( value = "Exploded jar file not a directory (ignored): %s", id = 15006 )
    void explodedJarNotDirectory( URL jarUrl );

    @LogMessage( level = ERROR )
    @Message( value = "Illegal argument on static metamodel field injection : %s#%s; expected type :  %s; encountered type : %s", id = 15007 )
    void illegalArgumentOnStaticMetamodelFieldInjection( String name,
                                                         String name2,
                                                         String name3,
                                                         String name4 );

    @LogMessage( level = ERROR )
    @Message( value = "Malformed URL: %s", id = 15008 )
    void malformedUrl( URL jarUrl,
                       @Cause URISyntaxException e );

    @LogMessage( level = WARN )
    @Message( value = "Malformed URL: %s", id = 15009 )
    void malformedUrlWarning( URL jarUrl,
                              @Cause URISyntaxException e );

    @LogMessage( level = WARN )
    @Message( value = "Unable to find file (ignored): %s", id = 15010 )
    void unableToFindFile( URL jarUrl,
                           @Cause Exception e );

    @LogMessage( level = ERROR )
    @Message( value = "Unable to locate static metamodel field : %s#%s", id = 15011 )
    void unableToLocateStaticMetamodelField( String name,
                                             String name2 );

    @LogMessage( level = INFO )
    @Message( value = "Using provided datasource", id = 15012 )
    void usingProvidedDataSource();
}
