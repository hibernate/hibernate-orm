/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results;

import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

/**
 * @asciidoc
 *
 * Logger for DomainResult related messages.  This includes messages related to:
 *
 * 		* creation of the DomainResult / Fetch nodes
 * 		* creation of Initializer / DomainResultAssembler delegates
 * 		* processing of values from JDBC and cache via Initializer / DomainResultAssembler
 *
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90005001, max = 90005100 )
@SubSystemLogging(
		name = ResultsLogger.LOGGER_NAME,
		description = "Logging related to `DomainResult` graphs which build individual parts of the domain model from JDBC or from cache"
)
public interface ResultsLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".results";

	Logger RESULTS_LOGGER = Logger.getLogger( LOGGER_NAME );
	ResultsLogger RESULTS_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), ResultsLogger.class, LOGGER_NAME );

	// todo (6.0) : make sure sql result processing classes use this logger

	static String subLoggerName(String subName) {
		return LOGGER_NAME + "." + subName;
	}

	static Logger subLogger(String subName) {
		return Logger.getLogger( subLoggerName( subName ) );
	}

}
