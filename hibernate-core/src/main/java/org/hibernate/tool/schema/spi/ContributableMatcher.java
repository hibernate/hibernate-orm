/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.boot.model.relational.ContributableDatabaseObject;

/**
 * Matcher for whether tables and sequences should be included based on its
 * {@link ContributableDatabaseObject#getContributor()}
 */
@FunctionalInterface
public interface ContributableMatcher {
	/**
	 * Matches everything
	 */
	ContributableMatcher ALL = contributed -> true;
	/**
	 * Matches nothing
	 */
	ContributableMatcher NONE = contributed -> false;

	/**
	 * Does the given `contributed` match this matcher?
	 */
	boolean matches(ContributableDatabaseObject contributed);
}
