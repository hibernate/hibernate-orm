/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.factory;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * Logging related to IdentifierGeneratorFactory
 */
@SubSystemLogging(
		name = IdGenFactoryLogging.LOGGER_NAME,
		description = "Logging related to creation of IdentifierGenerator instances"
)
public interface IdGenFactoryLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".idgen.factory";
	Logger ID_GEN_FAC_LOGGER = Logger.getLogger( LOGGER_NAME );
}
