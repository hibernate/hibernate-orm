/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxool.internal;

import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-proxool module.  It reserves message ids ranging from
 * 30001 to 35000 inclusively.
 * <p>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 30001, max = 35000 )
@SubSystemLogging(
		name = ProxoolMessageLogger.LOGGER_NAME,
		description = "Logs details related to Proxool connection pooling"
)
public interface ProxoolMessageLogger extends ConnectionInfoLogger {
	String LOGGER_NAME = ConnectionInfoLogger.LOGGER_NAME + ".proxool";
	ProxoolMessageLogger PROXOOL_MESSAGE_LOGGER = Logger.getMessageLogger( ProxoolMessageLogger.class, LOGGER_NAME );

	/**
	 * Logs the name of a named pool to be used for configuration information
	 *
	 * @param proxoolAlias The name (alias) of the proxool pool
	 */
	@LogMessage(level = DEBUG)
	@Message(value = "Configuring Proxool Provider to use pool alias: %s", id = 30002)
	void configuringProxoolProviderToUsePoolAlias(String proxoolAlias);

	/**
	 * Logs the name of a named existing pool in memory to be used
	 *
	 * @param proxoolAlias The name (alias) of the proxool pool
	 */
	@LogMessage(level = DEBUG)
	@Message(value = "Configuring Proxool Provider using existing pool in memory: %s", id = 30003)
	void configuringProxoolProviderUsingExistingPool(String proxoolAlias);

	/**
	 * Logs a message that the proxool pool will be built using its JAXP (XML) configuration mechanism
	 *
	 * @param jaxpFile The XML configuration file to use
	 */
	@LogMessage(level = DEBUG)
	@Message(value = "Configuring Proxool Provider using JAXPConfigurator: %s", id = 30004)
	void configuringProxoolProviderUsingJaxpConfigurator(String jaxpFile);

	/**
	 * Logs a message that the proxool pool will be built using a properties file
	 *
	 * @param propFile The properties file to use
	 */
	@LogMessage(level = DEBUG)
	@Message(value = "Configuring Proxool Provider using Properties File: %s", id = 30005)
	void configuringProxoolProviderUsingPropertiesFile(String propFile);

}
