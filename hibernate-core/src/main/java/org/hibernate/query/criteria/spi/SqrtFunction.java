/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models the ANSI SQL <tt>SQRT</tt> function.
 *
 * @author Steve Ebersole
 */
public class SqrtFunction extends AbstractStandardFunction<Double> {
	public static final String NAME = "sqrt";

	private final ExpressionImplementor<? extends Number> argument;

	public SqrtFunction(ExpressionImplementor<? extends Number> argument, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Double.class, criteriaBuilder );
		this.argument = argument;
	}

	public ExpressionImplementor<? extends Number> getArgument() {
		return argument;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitSqrtFunction( this );
	}
}
