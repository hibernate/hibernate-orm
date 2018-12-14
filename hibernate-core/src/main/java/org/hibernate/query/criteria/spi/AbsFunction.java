/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models the ANSI SQL <tt>ABS</tt> function.
 *
 * @author Steve Ebersole
 */
public class AbsFunction<N extends Number> extends AbstractStandardFunction<N> {
	public static final String NAME = "abs";

	private final ExpressionImplementor<N> numericExpression;

	@SuppressWarnings("unchecked")
	public AbsFunction(ExpressionImplementor<N> numericExpression, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, (Class) numericExpression.getJavaType(), criteriaBuilder );
		this.numericExpression = numericExpression;
	}

	public ExpressionImplementor<N> getArgument() {
		return numericExpression;
	}

	@Override
	public <T> T accept(CriteriaVisitor visitor) {
		return visitor.visitAbsFunction( this );
	}
}
