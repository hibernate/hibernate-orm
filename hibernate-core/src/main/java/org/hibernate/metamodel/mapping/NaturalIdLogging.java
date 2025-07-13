/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.TRACE;

/**
 * Logging related to natural-id operations
 *
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = NaturalIdLogging.LOGGER_NAME,
		description = "Logging related to handling of natural-id mappings"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90001, max = 90100)
@Internal
public interface NaturalIdLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".mapping.natural_id";
	Logger NATURAL_ID_LOGGER = Logger.getLogger( LOGGER_NAME );
	NaturalIdLogging NATURAL_ID_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			NaturalIdLogging.class,
			LOGGER_NAME
	);

	@LogMessage(level = TRACE)
	@Message(value = "Caching natural id resolution from load [%s] : %s -> %s", id = 90001)
	void cachingNaturalIdResolutionFromLoad(String entityName, Object naturalId, Object id);

	@LogMessage(level = TRACE)
	@Message(value = "Locally caching natural id resolution [%s] : %s -> %s", id = 90002)
	void locallyCachingNaturalIdResolution(String entityName, Object naturalId, Object id);

	@LogMessage(level = TRACE)
	@Message(value = "Removing locally cached natural id resolution [%s] : %s -> %s", id = 90003)
	void removingLocallyCachedNaturalIdResolution(String entityName, Object naturalId, Object id);

	@LogMessage(level = TRACE)
	@Message(value = "Resolved natural key [%s] -> primary key [%s] resolution in session cache for [%s]:", id = 90004)
	void resolvedNaturalIdInSessionCache(Object naturalId, Object pk, String entityName);

	@LogMessage(level = TRACE)
	@Message(value = "Found natural key [%s] -> primary key [%s] xref in second-level cache for [%s]", id = 90005)
	void foundNaturalIdInSecondLevelCache(Object naturalId, Object pk, String entityName);
}
