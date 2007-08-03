//$Id: JDBCExceptionReporter.java 10670 2006-10-31 21:30:15Z epbernard $
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






