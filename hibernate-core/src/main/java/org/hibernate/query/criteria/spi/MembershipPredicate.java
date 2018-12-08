/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;


/**
 * Models a <tt>[NOT] MEMBER OF</tt> restriction
 *
 * @author Steve Ebersole
 */
public class MembershipPredicate<E> extends AbstractSimplePredicate {
	private final ExpressionImplementor<E> elementExpression;
	private final PluralPath<E> pluralPath;

	public MembershipPredicate(
			ExpressionImplementor<E> elementExpression,
			PluralPath<E> pluralPath,
			CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.elementExpression = elementExpression;
		this.pluralPath = pluralPath;
	}

	public MembershipPredicate(
			ExpressionImplementor<E> elementExpression,
			PluralPath<E> pluralPath,
			boolean negated,
			CriteriaNodeBuilder criteriaBuilder) {
		super( negated, criteriaBuilder );
		this.elementExpression = elementExpression;
		this.pluralPath = pluralPath;
	}

	public ExpressionImplementor<E> getElementExpression() {
		return elementExpression;
	}

	public PluralPath<E> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitMembershipPredicate( this );
	}
}
