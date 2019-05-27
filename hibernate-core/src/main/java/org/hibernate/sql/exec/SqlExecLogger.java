/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90004001, max = 90005000 )
public interface SqlExecLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.sql.exec";

	/**
	 * Static access to the logging instance
	 */
	SqlExecLogger INSTANCE = Logger.getMessageLogger(
			SqlExecLogger.class,
			LOGGER_NAME
	);

	// todo (6.0) : make sure sql execution classes use this logger
}
