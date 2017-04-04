/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

/**
 * An expression asserting that a collection property is empty
 *
 * @author Gavin King
 */
public class NotEmptyExpression extends AbstractEmptinessExpression implements Criterion {
	/**
	 * Constructs an EmptyExpression
	 *
	 * @param propertyName The collection property name
	 *
	 * @see Restrictions#isNotEmpty
	 */
	protected NotEmptyExpression(String propertyName) {
		super( propertyName );
	}

	@Override
	protected boolean excludeEmpty() {
		return true;
	}

}
