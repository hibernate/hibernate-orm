/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SubSystemLogging(
		name = BytecodeLogging.LOGGER_NAME,
		description = "Logging related to bytecode handling"
)
public interface BytecodeLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + "bytecode";
	Logger LOGGER = Logger.getLogger( LOGGER_NAME );
}
