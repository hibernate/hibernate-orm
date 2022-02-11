/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity;

import org.hibernate.sql.results.LoadingLogger;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public interface EntityLoadingLogging {
	String LOCAL_NAME = "entity";
	String LOGGER_NAME = LoadingLogger.subLoggerName( LOCAL_NAME );
	Logger ENTITY_LOADING_LOGGER = LoadingLogger.subLogger( LOCAL_NAME );

	boolean TRACE_ENABLED = ENTITY_LOADING_LOGGER.isTraceEnabled();
	boolean DEBUG_ENABLED = ENTITY_LOADING_LOGGER.isDebugEnabled();
}
