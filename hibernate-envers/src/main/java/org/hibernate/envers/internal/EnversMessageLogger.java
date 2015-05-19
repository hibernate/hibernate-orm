/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-envers module.  It reserves message ids ranging from
 * 25001 to 30000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
public interface EnversMessageLogger extends BasicLogger {
	/**
	 * Message indicating that user attempted to use the deprecated ValidTimeAuditStrategy
	 */
	@LogMessage(level = WARN)
	@Message(value = "ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead", id = 25001)
	void validTimeAuditStrategyDeprecated();
}
