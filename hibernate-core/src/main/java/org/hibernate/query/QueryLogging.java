/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90003001, max = 90003500 )
@SubSystemLogging(
		name = QueryLogging.LOGGER_NAME,
		description = "Logging related to Query processing"
)
@Internal
public interface QueryLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".query";

	Logger QUERY_LOGGER = Logger.getLogger( LOGGER_NAME );
	QueryLogging QUERY_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), QueryLogging.class, LOGGER_NAME );

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
	@Message(value = "Error in named query: %s", id = 90003001)
	void namedQueryError(String queryName, @Cause HibernateException e);

	@LogMessage(level = INFO)
	@Message(value = "Unable to determine lock mode value: %s -> %s", id = 90003002)
	void unableToDetermineLockModeValue(String hintName, Object value);

	@LogMessage(level = INFO)
	@Message(value = "Ignoring unrecognized query hint [%s]", id = 90003003)
	void ignoringUnrecognizedQueryHint(String hintName);

	@LogMessage(level = WARN)
	@Message(value = "firstResult/maxResults specified with collection fetch; applying in memory", id = 90003004)
	void firstOrMaxResultsSpecifiedWithCollectionFetch();
}
