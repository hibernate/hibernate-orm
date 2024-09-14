/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.sql.model.TableMapping;

/**
 * Used to check if a table should be included in the current execution
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface TableInclusionChecker {
	/**
	 * Perform the check
	 *
	 * @return {@code true} indicates the table should be included;
	 * {@code false} indicates it should not
	 */
	boolean include(TableMapping tableMapping);
}
