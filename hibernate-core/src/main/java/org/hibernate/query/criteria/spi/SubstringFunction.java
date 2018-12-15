/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models the ANSI SQL <tt>SUBSTRING</tt> function.
 *
 * @author Steve Ebersole
 */
public class SubstringFunction extends AbstractStandardFunction<String> {
	public static final String NAME = "substring";

	private final ExpressionImplementor<String> value;
	private final ExpressionImplementor<Integer> start;
	private final ExpressionImplementor<Integer> length;

	public SubstringFunction(
			ExpressionImplementor<String> value,
			ExpressionImplementor<Integer> start,
			ExpressionImplementor<Integer> length,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );
		this.value = value;
		this.start = start;
		this.length = length;
	}

	@SuppressWarnings({ "RedundantCast" })
	public SubstringFunction(
			ExpressionImplementor<String> value,
			ExpressionImplementor<Integer> start,
			CriteriaNodeBuilder criteriaBuilder) {
		this( value, start, (ExpressionImplementor<Integer>) null, criteriaBuilder );
	}

	public ExpressionImplementor<Integer> getLength() {
		return length;
	}

	public ExpressionImplementor<Integer> getStart() {
		return start;
	}

	public ExpressionImplementor<String> getValue() {
		return value;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitSubstringFunction( this );
	}
}
