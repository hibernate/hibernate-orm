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
package org.hibernate.proxool.internal;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-proxool module.  It reserves message ids ranging from
 * 30001 to 35000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
public interface ProxoolMessageLogger extends CoreMessageLogger {

	/**
	 * Logs the autocommit mode to be used for pooled connections
	 *
	 * @param autocommit The autocommit mode
	 */
	@LogMessage(level = INFO)
	@Message(value = "Autocommit mode: %s", id = 30001)
	void autoCommmitMode(boolean autocommit);

	/**
	 * Logs the name of a named pool to be used for configuration information
	 *
	 * @param proxoolAlias The name (alias) of the proxool pool
	 */
	@LogMessage(level = INFO)
	@Message(value = "Configuring Proxool Provider to use pool alias: %s", id = 30002)
	void configuringProxoolProviderToUsePoolAlias(String proxoolAlias);

	/**
	 * Logs the name of a named existing pool in memory to be used
	 *
	 * @param proxoolAlias The name (alias) of the proxool pool
	 */
	@LogMessage(level = INFO)
	@Message(value = "Configuring Proxool Provider using existing pool in memory: %s", id = 30003)
	void configuringProxoolProviderUsingExistingPool(String proxoolAlias);

	/**
	 * Logs a message that the proxool pool will be built using its JAXP (XML) configuration mechanism
	 *
	 * @param jaxpFile The XML configuration file to use
	 */
	@LogMessage(level = INFO)
	@Message(value = "Configuring Proxool Provider using JAXPConfigurator: %s", id = 30004)
	void configuringProxoolProviderUsingJaxpConfigurator(String jaxpFile);

	/**
	 * Logs a message that the proxool pool will be built using a properties file
	 *
	 * @param propFile The properties file to use
	 */
	@LogMessage(level = INFO)
	@Message(value = "Configuring Proxool Provider using Properties File: %s", id = 30005)
	void configuringProxoolProviderUsingPropertiesFile(String propFile);

	/**
	 * Builds a message about not being able to close the underlying proxool pool.
	 *
	 * @return The message
	 */
	@Message(value = "Exception occured when closing the Proxool pool", id = 30006)
	String exceptionClosingProxoolPool();

	/**
	 * Builds a message about invalid configuration
	 *
	 * @param proxoolPoolAlias The name (alias) of the proxool pool
	 *
	 * @return The message
	 */
	@Message(value = "Cannot configure Proxool Provider to use an existing in memory pool without the %s property set.", id = 30007)
	String unableToConfigureProxoolProviderToUseExistingInMemoryPool(String proxoolPoolAlias);

	/**
	 * Builds a message about invalid configuration
	 *
	 * @param proxoolPoolAlias The name (alias) of the proxool pool
	 *
	 * @return The message
	 */
	@Message(value = "Cannot configure Proxool Provider to use JAXP without the %s property set.", id = 30008)
	String unableToConfigureProxoolProviderToUseJaxp(String proxoolPoolAlias);

	/**
	 * Builds a message about invalid configuration
	 *
	 * @param proxoolPoolAlias The name (alias) of the proxool pool
	 *
	 * @return The message
	 */
	@Message(value = "Cannot configure Proxool Provider to use Properties File without the %s property set.", id = 30009)
	String unableToConfigureProxoolProviderToUsePropertiesFile(String proxoolPoolAlias);

	/**
	 * Builds a message about not being able to find or load the XML configuration file
	 *
	 * @param jaxpFile The XML file
	 *
	 * @return The message
	 */
	@Message(value = "Proxool Provider unable to load JAXP configurator file: %s", id = 30010)
	String unableToLoadJaxpConfiguratorFile(String jaxpFile);

	/**
	 * Builds a message about not being able to find or load the properties configuration file
	 *
	 * @param propFile The properties file
	 *
	 * @return The message
	 */
	@Message(value = "Proxool Provider unable to load Property configurator file: %s", id = 30011)
	String unableToLoadPropertyConfiguratorFile(String propFile);
}
