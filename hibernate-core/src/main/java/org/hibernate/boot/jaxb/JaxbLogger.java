/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb;

import org.hibernate.Internal;
import org.hibernate.boot.BootLogging;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005501, max = 90005600 )
@SubSystemLogging(
		name = JaxbLogger.LOGGER_NAME,
		description = "Logging related to JAXB processing"
)
@Internal
public interface JaxbLogger extends BasicLogger {
	String LOGGER_NAME = BootLogging.NAME + ".jaxb";
	JaxbLogger JAXB_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JaxbLogger.class, LOGGER_NAME );
}
