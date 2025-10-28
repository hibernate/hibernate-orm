/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import org.hibernate.Internal;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90006001, max = 90006100)
@Internal
public interface IncubationLogger {
	String CATEGORY = SubSystemLogging.BASE + ".incubating";

	IncubationLogger INCUBATION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), IncubationLogger.class, CATEGORY );

	@LogMessage(level = INFO)
	@Message(
			id = 90006001,
			value = "Setting '%s' is still incubating (see Javadoc of corresponding member of 'org.hibernate.cfg.AvailableSettings')"
	)
	void incubatingSetting(String settingName);
}
