/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results;

import org.hibernate.boot.BootLogging;
import org.hibernate.boot.query.BootQueryLogging;

import org.jboss.logging.Logger;

/**
 * Logging for processing of named and implicit result-set mapping descriptors
 *
 * @author Steve Ebersole
 */
public class BootResultMappingLogging {
	public static final Logger LOGGER = BootQueryLogging.subLogger( "results" );

	public static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	public static final boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
