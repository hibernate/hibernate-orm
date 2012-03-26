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

import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import org.hibernate.internal.CoreMessageLogger;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-proxool module.  It reserves message ids ranging from
 * 30001 to 35000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger( projectCode = "HHH" )
public interface ProxoolMessageLogger extends CoreMessageLogger {

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
