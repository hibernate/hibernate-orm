/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.Incubating;

/**
 * A tuple of values.
 *
 * @since 6.5
 */
@Incubating
public interface JpaValues {

	/**
	 * Returns the expressions of this tuple.
	 */
	List<? extends JpaExpression<?>> getExpressions();
}
