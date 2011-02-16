/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.service.jdbc.connections.internal;
import static org.jboss.logging.Logger.Level.INFO;
import org.hibernate.HibernateLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Defines internationalized messages for this hibernate-proxool, with IDs ranging from 30001 to 35000 inclusively. New messages
 * must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface ProxoolLogger extends HibernateLogger {

    @LogMessage( level = INFO )
    @Message( value = "Autocommit mode: %s", id = 30001 )
    void autoCommmitMode( boolean autocommit );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider to use pool alias: %s", id = 30002 )
    void configuringProxoolProviderToUsePoolAlias( String proxoolAlias );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using existing pool in memory: %s", id = 30003 )
    void configuringProxoolProviderUsingExistingPool( String proxoolAlias );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using JAXPConfigurator: %s", id = 30004 )
    void configuringProxoolProviderUsingJaxpConfigurator( String jaxpFile );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using Properties File: %s", id = 30005 )
    void configuringProxoolProviderUsingPropertiesFile( String proxoolAlias );

    @Message( value = "Exception occured when closing the Proxool pool", id = 30006 )
    String exceptionClosingProxoolPool();

    @Message( value = "Cannot configure Proxool Provider to use an existing in memory pool without the %s property set.", id = 30007 )
    String unableToConfigureProxoolProviderToUseExistingInMemoryPool( String proxoolPoolAlias );

    @Message( value = "Cannot configure Proxool Provider to use JAXP without the %s property set.", id = 30008 )
    String unableToConfigureProxoolProviderToUseJaxp( String proxoolPoolAlias );

    @Message( value = "Cannot configure Proxool Provider to use Properties File without the %s property set.", id = 30009 )
    String unableToConfigureProxoolProviderToUsePropertiesFile( String proxoolPoolAlias );

    @Message( value = "Proxool Provider unable to load JAXP configurator file: %s", id = 30010 )
    String unableToLoadJaxpConfiguratorFile( String jaxpFile );

    @Message( value = "Proxool Provider unable to load Property configurator file: %s", id = 30011 )
    String unableToLoadPropertyConfiguratorFile( String propFile );
}
