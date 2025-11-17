/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;

/**
 * The logger interface for the Hibernate Spatial module.
 *
 * @author Karel Maesen, Geovise BVBA
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 80000001, max = 80001000)
@SubSystemLogging(
		name = HSMessageLogger.LOGGER_NAME,
		description = "Base logging for Hibernate Spatial",
		mixed = true
)
public interface HSMessageLogger extends BasicLogger {

	String LOGGER_NAME = "org.hibernate.spatial";

	HSMessageLogger SPATIAL_MSG_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), HSMessageLogger.class, LOGGER_NAME );

	@LogMessage(level = INFO)
	@Message(value = "Hibernate Spatial integration enabled: %s", id = 80000001)
	void spatialEnabled(boolean enabled);

	@LogMessage(level = DEBUG)
	@Message(value = "Hibernate Spatial using Connection Finder for creating Oracle types: %s", id = 80000002)
	void connectionFinder(String className);

	@LogMessage(level = DEBUG)
	@Message(value = "hibernate-spatial adding type contributions from: %s", id = 80000003)
	void typeContributions(String source);

	@LogMessage(level = DEBUG)
	@Message(value = "hibernate-spatial adding function contributions from: %s", id = 80000004)
	void functionContributions(String source);
}
