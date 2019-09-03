/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

/**
 * Support for Hibernate's {@link org.hibernate.annotations.Subselect} feature
 *
 * @author Steve Ebersole
 */
public class InlineView implements Table {
	private final String query;

	public InlineView(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public String getTableExpression() {
		return getQuery();
	}
}
