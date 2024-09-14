/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90006001, max = 90007000)
public interface IncubationLogger {
	String CATEGORY = SubSystemLogging.BASE + ".incubating";

	IncubationLogger INCUBATION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), IncubationLogger.class, CATEGORY );

	@LogMessage(level = WARN)
	@Message(
			id = 90006001,
			value = "Encountered incubating setting [%s].  See javadoc on corresponding " +
					"`org.hibernate.cfg.AvailableSettings` constant for details."
	)
	void incubatingSetting(String settingName);
}
