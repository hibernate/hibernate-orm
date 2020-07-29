/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BootLogging {
	public static final String NAME = "org.hibernate.orm.boot";

	public static final Logger LOGGER = Logger.getLogger( NAME );

	public static final boolean DEBUG_ENABLED = LOGGER.isDebugEnabled();
	public static final boolean TRACE_ENABLED = LOGGER.isTraceEnabled();
}
