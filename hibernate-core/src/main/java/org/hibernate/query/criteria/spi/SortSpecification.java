/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;

/**
 * @author Steve Ebersole
 */
public class SortSpecification extends AbstractNode implements JpaOrder {
	private final ExpressionImplementor sortExpression;

	private SortOrder sortOrder;
	private NullPrecedence nullPrecedence;

	public SortSpecification(
			ExpressionImplementor sortExpression,
			CriteriaNodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.sortExpression = sortExpression;
	}

	public SortSpecification(
			ExpressionImplementor sortExpression,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
			CriteriaNodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.sortExpression = sortExpression;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	@Override
	public JpaExpression<?> getExpression() {
		return sortExpression;
	}

	@Override
	public SortOrder getSortOrder() {
		return sortOrder;
	}

	@Override
	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}

	@Override
	public SortSpecification nullPrecedence(NullPrecedence precedence) {
		this.nullPrecedence = precedence;
		return this;
	}

	@Override
	public SortSpecification reverse() {
		sortOrder = sortOrder.reverse();
		return this;
	}

	@Override
	public boolean isAscending() {
		return sortOrder == SortOrder.ASCENDING;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitSortSpecification( this );
	}
}
