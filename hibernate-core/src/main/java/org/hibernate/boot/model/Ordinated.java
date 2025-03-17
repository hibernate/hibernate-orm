/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

/**
 * Interface that defines the loading order of a contribution
 *
 * @author Steven Barendregt
 */
public interface Ordinated {

	/**
	 * Determines order in which contributions will be applied
	 * (lowest ordinal first).
	 * <p>
	 * The range 0-500 is reserved for Hibernate, range 500-1000 for libraries and
	 * 1000-Integer.MAX_VALUE for user-defined Contributors.
	 * <p>
	 * Contributions from higher precedence contributors (higher numbers) effectively override
	 * contributions from lower precedence.
	 *
	 * @return the ordinal for a Contributor
	 */
	default int ordinal() {
		return 1000;
	}

}
