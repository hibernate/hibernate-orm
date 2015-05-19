/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Possible optimistic locking strategies.
 *
 * @author Emmanuel Bernard
 */
public enum OptimisticLockType {
	/**
	 * Perform no optimistic locking.
	 */
	NONE,
	/**
	 * Perform optimistic locking using a dedicated version column.
	 *
	 * @see javax.persistence.Version
	 */
	VERSION,
	/**
	 * Perform optimistic locking based on *dirty* fields as part of an expanded WHERE clause restriction for the
	 * UPDATE/DELETE SQL statement.
	 */
	DIRTY,
	/**
	 * Perform optimistic locking based on *all* fields as part of an expanded WHERE clause restriction for the
	 * UPDATE/DELETE SQL statement.
	 */
	ALL
}
