// $Id: ConnectionReleaseMode.java 8409 2005-10-14 20:28:18Z steveebersole $
package org.hibernate;

import java.io.Serializable;

/**
 * Defines the various policies by which Hibernate might release its underlying
 * JDBC connection.
 *
 * @author Steve Ebersole
 */
public class ConnectionReleaseMode  implements Serializable {

	/**
	 * Indicates that JDBC connection should be aggressively released after each 
	 * SQL statement is executed. In this mode, the application <em>must</em>
	 * explicitly close all iterators and scrollable results. This mode may
	 * only be used with a JTA datasource.
	 */
	public static final ConnectionReleaseMode AFTER_STATEMENT = new ConnectionReleaseMode( "after_statement" );

	/**
	 * Indicates that JDBC connections should be released after each transaction 
	 * ends (works with both JTA-registered synch and HibernateTransaction API).
	 * This mode may not be used with an application server JTA datasource.
	 * <p/>
	 * This is the default mode starting in 3.1; was previously {@link #ON_CLOSE}.
	 */
	public static final ConnectionReleaseMode AFTER_TRANSACTION = new ConnectionReleaseMode( "after_transaction" );

	/**
	 * Indicates that connections should only be released when the Session is explicitly closed 
	 * or disconnected; this is the legacy (Hibernate2 and pre-3.1) behavior.
	 */
	public static final ConnectionReleaseMode ON_CLOSE = new ConnectionReleaseMode( "on_close" );


	private String name;

	private ConnectionReleaseMode(String name) {
		this.name = name;
	}

	/**
	 * Override of Object.toString().  Returns the release mode name.
	 *
	 * @return The release mode name.
	 */
	public String toString() {
		return name;
	}

	/**
	 * Determine the correct ConnectionReleaseMode instance based on the given
	 * name.
	 *
	 * @param modeName The release mode name.
	 * @return The appropriate ConnectionReleaseMode instance
	 * @throws HibernateException Indicates the modeName param did not match any known modes.
	 */
	public static ConnectionReleaseMode parse(String modeName) throws HibernateException {
		if ( AFTER_STATEMENT.name.equals( modeName ) ) {
			return AFTER_STATEMENT;
		}
		else if ( AFTER_TRANSACTION.name.equals( modeName ) ) {
			return AFTER_TRANSACTION;
		}
		else if ( ON_CLOSE.name.equals( modeName ) ) {
			return ON_CLOSE;
		}
		throw new HibernateException( "could not determine appropriate connection release mode [" + modeName + "]" );
	}

	private Object readResolve() {
		return parse( name );
	}
}
