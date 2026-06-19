/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

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
import java.util.Locale;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
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
	QueryLogging QUERY_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), QueryLogging.class, LOGGER_NAME, Locale.ROOT );
	QueryLogging QUERY_PLAN_CACHE_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			QueryLogging.class,
			subLoggerName( "plan.cache" ),
			Locale.ROOT
	);

	static String subLoggerName(String subName) {
		return LOGGER_NAME + '.' + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

	@LogMessage(level = ERROR)
	@Message(value = "Error in named query: %s", id = 90003001)
	void namedQueryError(String queryName, @Cause HibernateException e);

	@LogMessage(level = INFO)
	@Message(value = "Ignoring unrecognized query hint [%s]", id = 90003003)
	void ignoringUnrecognizedQueryHint(String hintName);

	@LogMessage(level = WARN)
	@Message(value = "firstResult/maxResults specified with collection fetch; applying in memory", id = 90003004)
	void firstOrMaxResultsSpecifiedWithCollectionFetch();

	@LogMessage(level = TRACE)
	@Message(value = "Starting query interpretation cache (size %s)", id = 90003005)
	void startingQueryInterpretationCache(int maxQueryPlanCount);

	@LogMessage(level = TRACE)
	@Message(value = "Resolving cached query plan for [%s]", id = 90003006)
	void resolvingCachedQueryPlan(Object key);

	@LogMessage(level = TRACE)
	@Message(value = "Resolving HQL interpretation for [%s]", id = 90003007)
	void resolvingHqlInterpretation(String queryString);

	@LogMessage(level = TRACE)
	@Message(value = "Resolving native query parameters for [%s]", id = 90003008)
	void resolvingNativeQueryParameters(String queryString);

	@LogMessage(level = TRACE)
	@Message(value = "Destroying query interpretation cache", id = 90003009)
	void destroyingQueryInterpretationCache();
}
