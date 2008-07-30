/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JDBCExceptionReporter {

	public static final Logger log = LoggerFactory.getLogger(JDBCExceptionReporter.class);
	public static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	public static final String DEFAULT_WARNING_MSG = "SQL Warning";

	private JDBCExceptionReporter() {}
	
	public static void logAndClearWarnings(Connection connection) {
		if ( log.isWarnEnabled() ) {
			try {
				logWarnings( connection.getWarnings() );
			}
			catch (SQLException sqle) {
				//workaround for WebLogic
				log.debug("could not log warnings", sqle);
			}
		}
		try {
			//Sybase fail if we don't do that, sigh...
			connection.clearWarnings();
		}
		catch (SQLException sqle) {
			log.debug("could not clear warnings", sqle);
		}
	}

	public static void logWarnings(SQLWarning warning) {
		logWarnings(warning, null);
	}

	public static void logWarnings(SQLWarning warning, String message) {
		if ( log.isWarnEnabled() ) {
			if ( log.isDebugEnabled() && warning != null ) {
				message = StringHelper.isNotEmpty(message) ? message : DEFAULT_WARNING_MSG;
				log.debug( message, warning );
			}
			while (warning != null) {
				StringBuffer buf = new StringBuffer(30)
				        .append( "SQL Warning: ")
						.append( warning.getErrorCode() )
						.append( ", SQLState: ")
						.append( warning.getSQLState() );
				log.warn( buf.toString() );
				log.warn( warning.getMessage() );
				warning = warning.getNextWarning();
			}
		}
	}

	public static void logExceptions(SQLException ex) {
		logExceptions(ex, null);
	}

	public static void logExceptions(SQLException ex, String message) {
		if ( log.isErrorEnabled() ) {
			if ( log.isDebugEnabled() ) {
				message = StringHelper.isNotEmpty(message) ? message : DEFAULT_EXCEPTION_MSG;
				log.debug( message, ex );
			}
			while (ex != null) {
				StringBuffer buf = new StringBuffer(30)
						.append( "SQL Error: " )
				        .append( ex.getErrorCode() )
				        .append( ", SQLState: " )
				        .append( ex.getSQLState() );
				log.warn( buf.toString() );
				log.error( ex.getMessage() );
				ex = ex.getNextException();
			}
		}
	}

//	public static JDBCException newJDBCException(String string, SQLException root, String sql) {
//		string = string + " [" + sql + ']';
//		log.error(string, root);
//		logExceptions(root);
//		return new JDBCException(string, root, sql);
//	}
//
//	public static JDBCException newJDBCException(String string, SQLException root) {
//		log.error(string, root);
//		logExceptions(root);
//		return new JDBCException(string, root);
//	}

}






