package org.hibernate.cfg;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;

/**
 * Enumerates the possible reactions to a boot-time check.
 *
 * @since 8.0
 */
@Incubating(since = "8.0")
public enum CheckHandling {
	/**
	 * Log a warning.
	 */
	WARN,
	/**
	 * Throw an exception, preventing boot.
	 */
	ERROR,
	/**
	 * Ignore the check entirely.
	 */
	IGNORE;

	public static CheckHandling interpret(Object value, CheckHandling defaultValue) {
		if ( value instanceof CheckHandling checkHandling ) {
			return checkHandling;
		}
		else if ( value instanceof String string ) {
			for ( var handling : values() ) {
				if ( handling.name().equalsIgnoreCase( string ) ) {
					return handling;
				}
			}
			throw new HibernateException( "Unrecognized CheckHandling value: " + value
					+ " (should be 'warn', 'error', or 'ignore')" );
		}
		else if ( value != null ) {
			throw new HibernateException( "Unrecognized CheckHandling value: " + value
					+ " (should be 'warn', 'error', or 'ignore')" );
		}
		return defaultValue;
	}
}
