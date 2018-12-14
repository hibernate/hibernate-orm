/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models the ANSI SQL <tt>LOWER</tt> function.
 *
 * @author Steve Ebersole
 */
public class LowerFunction extends AbstractStandardFunction<String> {
	public static final String NAME = "lower";

	private final ExpressionImplementor<String> argument;

	public LowerFunction(ExpressionImplementor<String> argument, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );
		this.argument = argument;
	}

	public ExpressionImplementor<String> getArgument() {
		return argument;
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitLowerFunction( this );
	}
}
