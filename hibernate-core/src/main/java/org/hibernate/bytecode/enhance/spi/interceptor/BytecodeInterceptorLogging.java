/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import org.hibernate.Internal;
import org.hibernate.bytecode.BytecodeLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to bytecode enhancement interceptors
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90005901, max = 90006000)
@SubSystemLogging(
		name = BytecodeInterceptorLogging.LOGGER_NAME,
		description = "Logging related to bytecode-based interception"
)
@Internal
public interface BytecodeInterceptorLogging extends BasicLogger {
	String SUB_NAME = "interceptor";
	String LOGGER_NAME = BytecodeLogging.LOGGER_NAME + "." + SUB_NAME;

	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
	BytecodeInterceptorLogging MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BytecodeInterceptorLogging.class, LOGGER_NAME );

	@LogMessage(level = WARN)
	@Message(
			id = 90005901,
			value = "`%s#%s` was mapped with explicit lazy-group (`%s`).  Hibernate will ignore the lazy-group - this is generally " +
					"not a good idea for to-one associations as it would lead to 2 separate SQL selects to initialize the association.  " +
					"This is expected to be improved in future versions of Hibernate"
	)
	void lazyGroupIgnoredForToOne(String ownerName, String attributeName, String requestedLazyGroup);
}
