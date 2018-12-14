/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models an <tt>EXISTS(<subquery>)</tt> predicate
 *
 * @author Steve Ebersole
 */
public class ExistsPredicate extends AbstractSimplePredicate {
	private final SubQuery<?> subQuery;

	public ExistsPredicate(SubQuery<?> subQuery, CriteriaNodeBuilder builder) {
		super( builder );
		this.subQuery = subQuery;
	}

	public SubQuery<?> getSubQuery() {
		return subQuery;
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitExistsPredicate( this );
	}
}
