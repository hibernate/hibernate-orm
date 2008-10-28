package org.hibernate.annotations;

/**
 * Possible actions on deletes
 *
 * @author Emmanuel Bernard
 */
public enum OnDeleteAction {
	/**
	 * the default
	 */
	NO_ACTION,
	/**
	 * use cascade delete capabilities of the DD
	 */
	CASCADE
}
