/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;

import static org.jboss.logging.Logger.Level.ERROR;

/**
 * @author Steve Ebersole
 */
public interface QueryLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.query";

	QueryLogger QUERY_LOGGER = Logger.getMessageLogger(
			QueryLogger.class,
			LOGGER_NAME
	);

	boolean TRACE_ENABLED = QUERY_LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = QUERY_LOGGER.isDebugEnabled();


	@LogMessage(level = ERROR)
	@Message(value = "Error in named query: %s", id = 90003001)
	void namedQueryError(String queryName, @Cause HibernateException e);
}
