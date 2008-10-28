package org.hibernate.annotations;

/**
 * Enumeration extending javax.persistence flush modes.
 *
 * @author Carlos González-Cadenas
 */

public enum FlushModeType {
	/**
	 * see {@link org.hibernate.FlushMode.ALWAYS}
	 */
	ALWAYS,
	/**
	 * see {@link org.hibernate.FlushMode.AUTO}
	 */
	AUTO,
	/**
	 * see {@link org.hibernate.FlushMode.COMMIT}
	 */
	COMMIT,
	/**
	 * see {@link org.hibernate.FlushMode.NEVER}
	 * @deprecated use MANUAL, will be removed in a subsequent release
	 */
	NEVER,
	/**
	 * see {@link org.hibernate.FlushMode.MANUAL}
	 */
	MANUAL,

	/**
	 * Current flush mode of the persistence context at the time the query is executed
	 */
	PERSISTENCE_CONTEXT
}