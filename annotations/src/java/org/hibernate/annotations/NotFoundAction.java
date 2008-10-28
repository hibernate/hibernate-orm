package org.hibernate.annotations;

/**
 * Actoin to use when an element is not found in DB while beeing expected
 *
 * @author Emmanuel Bernard
 */
public enum NotFoundAction {
	/**
	 * raise an exception when an element is not found (default and recommended)
	 */
	EXCEPTION,
	/**
	 * ignore the element when not found in DB
	 */
	IGNORE
}
