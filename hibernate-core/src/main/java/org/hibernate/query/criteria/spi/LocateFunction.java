/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models the ANSI SQL <tt>LOCATE</tt> function.
 *
 * @author Steve Ebersole
 */
public class LocateFunction extends AbstractStandardFunction<Integer> {
	public static final String NAME = "locate";

	private final ExpressionImplementor<String> pattern;
	private final ExpressionImplementor<String> string;
	private final ExpressionImplementor<Integer> start;

	public LocateFunction(
			ExpressionImplementor<String> pattern,
			ExpressionImplementor<String> string,
			ExpressionImplementor<Integer> start,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Integer.class, criteriaBuilder );

		this.pattern = pattern;
		this.string = string;
		this.start = start;
	}

	public ExpressionImplementor<String> getPattern() {
		return pattern;
	}

	public ExpressionImplementor<Integer> getStart() {
		return start;
	}

	public ExpressionImplementor<String> getString() {
		return string;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitLocateFunction( this );
	}
}
