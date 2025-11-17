/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Subsystem logging related to ConnectionProvider
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = ConnectionProviderLogging.NAME,
		description = "Logging related to ConnectionProvider"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 102001, max = 102100)
interface ConnectionProviderLogging {
	String NAME = SubSystemLogging.BASE + ".connection";
	ConnectionProviderLogging CONNECTION_PROVIDER_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), ConnectionProviderLogging.class, NAME );

	@LogMessage(level = WARN)
	@Message(id = 102001,
			value = "Configuration settings with for connection provider '%s' are set, but the connection provider is not on the classpath; these properties will be ignored")
	void providerClassNotFound(String c3p0ProviderClassName);

	@LogMessage(level = INFO)
	@Message(id = 102002,
			value = "Instantiating explicit connection provider: %s")
	void instantiatingExplicitConnectionProvider(String providerClassName);

	@LogMessage(level = WARN)
	@Message(id = 102003,
			value = "No appropriate connection provider encountered; client must supply connections")
	void noAppropriateConnectionProvider();

}
