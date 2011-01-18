/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.hibernate.service.jdbc.connections.internal;

import static org.jboss.logging.Logger.Level.INFO;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Interface defining messages that may be logged by the outer class
 */
@MessageLogger
public interface ProxoolLogger extends BasicLogger {

    @LogMessage( level = INFO )
    @Message( value = "Autocommit mode: %s" )
    void autoCommmitMode( boolean autocommit );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider to use pool alias: %s" )
    void configuringProxoolProviderToUsePoolAlias( String proxoolAlias );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using existing pool in memory: %s" )
    void configuringProxoolProviderUsingExistingPool( String proxoolAlias );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using JAXPConfigurator: %s" )
    void configuringProxoolProviderUsingJaxpConfigurator( String jaxpFile );

    @LogMessage( level = INFO )
    @Message( value = "Configuring Proxool Provider using Properties File: %s" )
    void configuringProxoolProviderUsingPropertiesFile( String proxoolAlias );

    @Message( value = "Exception occured when closing the Proxool pool" )
    String exceptionClosingProxoolPool();

    @LogMessage( level = INFO )
    @Message( value = "JDBC isolation level: %s" )
    void jdbcIsolationLevel( String isolationLevelToString );

    @Message( value = "Cannot configure Proxool Provider to use an existing in memory pool without the %s property set." )
    String unableToConfigureProxoolProviderToUseExistingInMemoryPool( String proxoolPoolAlias );

    @Message( value = "Cannot configure Proxool Provider to use JAXP without the %s property set." )
    String unableToConfigureProxoolProviderToUseJaxp( String proxoolPoolAlias );

    @Message( value = "Cannot configure Proxool Provider to use Properties File without the %s property set." )
    String unableToConfigureProxoolProviderToUsePropertiesFile( String proxoolPoolAlias );

    @Message( value = "Proxool Provider unable to load JAXP configurator file: %s" )
    String unableToLoadJaxpConfiguratorFile( String jaxpFile );

    @Message( value = "Proxool Provider unable to load Property configurator file: %s" )
    String unableToLoadPropertyConfiguratorFile( String propFile );
}
