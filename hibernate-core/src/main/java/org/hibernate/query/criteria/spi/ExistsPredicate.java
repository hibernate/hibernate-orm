/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Subquery;

import org.hibernate.query.criteria.JpaSubQuery;

/**
 * Models an <tt>EXISTS(<subquery>)</tt> predicate
 *
 * @author Steve Ebersole
 */
public class ExistsPredicate extends AbstractSimplePredicate {
	private final JpaSubQuery<?> subQuery;

	public ExistsPredicate(JpaSubQuery<?> subQuery, CriteriaNodeBuilder builder) {
		super( builder );
		this.subQuery = subQuery;
	}

	public Subquery<?> getSubQuery() {
		return subQuery;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitExistsPredicate( this );
	}
}
