/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory;

import org.jboss.logging.Logger;

/**
 * Logging related to IdentifierGeneratorFactory
 */
public class IdGenFactoryLogging {
	public static final Logger ID_GEN_FAC_LOGGER = Logger.getLogger( "org.hibernate.orm.idgen.factory" );

	public static final boolean IS_TRACE_ENABLE = ID_GEN_FAC_LOGGER.isTraceEnabled();
	public static final boolean IS_DEBUG_ENABLE = ID_GEN_FAC_LOGGER.isDebugEnabled();
}
