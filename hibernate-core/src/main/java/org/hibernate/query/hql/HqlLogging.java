/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql;

import org.hibernate.HibernateException;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.query.QueryLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.ERROR;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90003501, max = 90004000 )
@SubSystemLogging(
		name = HqlLogging.LOGGER_NAME,
		description = "Logging related to HQL parsing"
)
public interface HqlLogging extends BasicLogger {
	String LOGGER_NAME = QueryLogging.LOGGER_NAME + ".hql";

	HqlLogging QUERY_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), HqlLogging.class, LOGGER_NAME );

	static String subLoggerName(String subName) {
		return LOGGER_NAME + '.' + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	static <T> T subLogger(String subName, Class<T> loggerJavaType) {
		return Logger.getMessageLogger( MethodHandles.lookup(), loggerJavaType, subLoggerName( subName ) );
	}

	@LogMessage(level = ERROR)
	@Message(value = "Error in named query: %s", id = 90003501)
	void namedQueryError(String queryName, @Cause HibernateException e);
}
