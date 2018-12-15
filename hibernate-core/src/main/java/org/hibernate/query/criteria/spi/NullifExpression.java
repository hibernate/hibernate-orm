/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models an ANSI SQL <tt>NULLIF</tt> expression.  <tt>NULLIF</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class NullifExpression<T> extends AbstractExpression<T> {
	private final ExpressionImplementor<? extends T> primaryExpression;
	private final ExpressionImplementor<? extends T> secondaryExpression;

	@SuppressWarnings("unchecked")
	public NullifExpression(
			ExpressionImplementor<? extends T> primaryExpression,
			ExpressionImplementor<? extends T> secondaryExpression,
			CriteriaNodeBuilder builder) {
		super( determineType( primaryExpression, secondaryExpression ), builder );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = secondaryExpression;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> determineType(ExpressionImplementor<? extends T>... expressions) {
		for ( ExpressionImplementor<? extends T> expression : expressions ) {
			if ( expression.getJavaTypeDescriptor() != null ) {
				return (Class<T>) expression.getJavaTypeDescriptor().getJavaType();
			}
		}

		return null;
	}

	public ExpressionImplementor<? extends T> getPrimaryExpression() {
		return primaryExpression;
	}

	public ExpressionImplementor<? extends T> getSecondaryExpression() {
		return secondaryExpression;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitNullifExpression( this );
	}
}
