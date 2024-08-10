/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.jaxb;

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
public interface JaxbLogger extends BasicLogger {
	String LOGGER_NAME = BootLogging.NAME + ".jaxb";
	JaxbLogger JAXB_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), JaxbLogger.class, LOGGER_NAME );
}
