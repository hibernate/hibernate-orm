/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.util;

import javax.persistence.LockTimeoutException;

import org.hibernate.PessimisticLockException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;

/**
 * @author Vlad Mihalcea
 */
public class ExceptionUtil {

	private static final ExceptionUtil INSTANCE = new ExceptionUtil();

	public static ExceptionUtil getInstance() {
		return INSTANCE;
	}

	private ExceptionUtil() {
	}

	/**
	 * Get the root cause of a particular {@code Throwable}
	 *
	 * @param t exception
	 *
	 * @return exception root cause
	 */
	public static Throwable rootCause(Throwable t) {
		Throwable cause = t.getCause();
		if ( cause != null && cause != t ) {
			return rootCause( cause );
		}
		return t;
	}

	/**
	 * Get a specific cause.
	 *
	 * @param t exception
	 * @param causeClass cause type
	 *
	 * @return exception root cause
	 */
	public static Throwable findCause(Throwable t, Class<? extends Throwable> causeClass) {
		Throwable cause = t.getCause();
		if ( cause != null && !causeClass.equals( cause.getClass() ) ) {
			return ( cause != t ) ? findCause( cause, causeClass ) : null;
		}
		return cause;
	}

	/**
	 * Was the given exception caused by a SQL lock timeout?
	 *
	 * @param e exception
	 *
	 * @return is caused by a SQL lock timeout
	 */
	public static boolean isSqlLockTimeout(Exception e) {
		// grr, exception can be any number of types based on database
		// 		see HHH-6887
		if ( LockAcquisitionException.class.isInstance( e )
				|| LockTimeoutException.class.isInstance( e )
				|| GenericJDBCException.class.isInstance( e )
				|| PessimisticLockException.class.isInstance( e )
				|| javax.persistence.PessimisticLockException.class.isInstance( e )
				|| JDBCConnectionException.class.isInstance( e ) ) {
			return true;
		}
		else {
			Throwable rootCause = ExceptionUtil.rootCause( e );
			if (
					rootCause != null && (
							rootCause.getMessage().contains( "timeout" ) ||
									rootCause.getMessage().contains( "timed out" ) ||
									rootCause.getMessage().contains( "lock(s) could not be acquired" ) ||
									rootCause.getMessage().contains( "Could not acquire a lock" )

					)
			) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Was the given exception caused by a SQL connection close
	 *
	 * @param e exception
	 *
	 * @return is caused by a SQL connection close
	 */
	public static boolean isConnectionClose(Exception e) {
		Throwable rootCause = ExceptionUtil.rootCause( e );
		if ( rootCause != null && (
				rootCause.getMessage().toLowerCase().contains( "connection is close" ) ||
				rootCause.getMessage().toLowerCase().contains( "closed connection" )
		) ) {
			return true;
		}
		return false;
	}
}
