/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxool.internal;

import org.hibernate.internal.log.ConnectionPoolingLogger;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-proxool module.  It reserves message ids ranging from
 * 30001 to 35000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 30001, max = 35000 )
public interface ProxoolMessageLogger extends ConnectionPoolingLogger {

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
	@Message(value = "Exception occurred when closing the Proxool pool", id = 30006)
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
