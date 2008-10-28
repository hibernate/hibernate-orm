//$Id$
package org.hibernate.annotations;

/**
 * Optimistic locking strategy
 * VERSION is the default and recommanded one
 *
 * @author Emmanuel Bernard
 */
public enum OptimisticLockType {
	/**
	 * no optimistic locking
	 */
	NONE,
	/**
	 * use a column version
	 */
	VERSION,
	/**
	 * dirty columns are compared
	 */
	DIRTY,
	/**
	 * all columns are compared
	 */
	ALL
}
