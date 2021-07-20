/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Boris Unckel
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 10002001, max = 10003000)
public interface ValidatorLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.internal.util.validator";

	ValidatorLogger INSTANCE = Logger.getMessageLogger(ValidatorLogger.class, LOGGER_NAME);

	@Message(id = 10002001, value = "Parameter '%s' must not be null")
	IllegalArgumentException nullParamIAE(String paramName);

	@Message(id = 10002002, value = "Parameter '%s' must not be null")
	NullPointerException nullParamNPE(String paramName);
}
