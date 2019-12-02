/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @asciidoc
 *
 * Logger for DomainResult related messages.  This includes messages related to:
 *
 * 		* creation of the DomainResult / Fetch nodes
 * 		* creation of Initializer / DomainResultAssembler delegates
 * 		* processing of JDBC values via Initializer / DomainResultAssembler
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005001, max = 90005100 )
public interface ResultsLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.sql.results";

	/**
	 * Static access to the logging instance
	 */
	ResultsLogger INSTANCE = Logger.getMessageLogger(
			ResultsLogger.class,
			LOGGER_NAME
	);

	// todo (6.0) : make sure sql result processing classes use this logger

	boolean TRACE_ENABLED = INSTANCE.isTraceEnabled();
	boolean DEBUG_ENABLED = INSTANCE.isDebugEnabled();
	boolean INFO_ENABLED = INSTANCE.isInfoEnabled();
}
