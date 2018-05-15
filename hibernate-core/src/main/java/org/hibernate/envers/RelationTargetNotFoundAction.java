/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers;

import org.hibernate.Incubating;

/**
 * Possible actions when an associated entity cannot be found.
 *
 * @author Chris Cranford
 *
 * @see org.hibernate.annotations.NotFoundAction
 */
@Incubating(since = "6.0")
public enum RelationTargetNotFoundAction {
	/**
	 * Specifies that exception handling should be based on the global system property:
	 * {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG}.
	 */
	DEFAULT,

	/**
	 * Specifies that exceptions should be thrown regardless of the global system property:
	 * {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG}.
	 */
	ERROR,

	/**
	 * Specifies that exceptions should be ignored regardless of the global system property:
	 * {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG}.
	 */
	IGNORE
}
