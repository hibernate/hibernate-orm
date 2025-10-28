/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import org.hibernate.Internal;
import org.hibernate.internal.SessionLogging;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;
import java.util.ServiceConfigurationError;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Miscellaneous logging related to Hibernate ORM Core.
 */
@SubSystemLogging(
		name = SessionLogging.NAME,
		description = "Miscellaneous Logging related to Hibernate ORM Core"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min=10002,max = 20000)
@Internal
public interface ServiceLogger extends BasicLogger {

	String NAME = SubSystemLogging.BASE + ".service";

	Logger LOGGER = Logger.getLogger( NAME );
	ServiceLogger SERVICE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), ServiceLogger.class, NAME );

	@LogMessage(level = TRACE)
	@Message(id = 10500, value = "Initializing service: %s")
	void initializingService(String serviceRole);

	@LogMessage(level = INFO)
	@Message(id = 10369, value = "Error stopping service: %s")
	void unableToStopService(String serviceRole, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 10505, value = "Ignoring ServiceConfigurationError caught while instantiating service: %s")
	void ignoringServiceConfigurationError(String serviceContract, @Cause ServiceConfigurationError error);

	@LogMessage(level = WARN)
	@Message(id = 10450, value = "Encountered request for service by non-primary service role [%s -> %s]")
	void alternateServiceRole(String requestedRole, String targetRole);

	@LogMessage(level = WARN)
	@Message(id = 10451, value = "Child registry [%s] was already registered; this will end badly later...")
	void childAlreadyRegistered(ServiceRegistryImplementor child);

	@LogMessage(level = TRACE)
	@Message(id = 10452, value = "Automatically destroying bootstrap registry after deregistration of every child ServiceRegistry")
	void destroyingBootstrapRegistry();

	@LogMessage(level = TRACE)
	@Message(id = 10453, value = "Skipping destroying bootstrap registry after deregistration of every child ServiceRegistry")
	void skippingBootstrapRegistryDestruction();
}
