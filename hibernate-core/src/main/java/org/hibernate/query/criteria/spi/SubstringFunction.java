/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;

import org.hibernate.query.criteria.JpaExpression;

/**
 * Models the ANSI SQL <tt>SUBSTRING</tt> function.
 *
 * @author Steve Ebersole
 */
public class SubstringFunction extends AbstractStandardFunction<String> {
	public static final String NAME = "substring";

	private final JpaExpression<String> value;
	private final JpaExpression<Integer> start;
	private final JpaExpression<Integer> length;

	public SubstringFunction(
			JpaExpression<String> value,
			JpaExpression<Integer> start,
			JpaExpression<Integer> length,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );
		this.value = value;
		this.start = start;
		this.length = length;
	}

	@SuppressWarnings({ "RedundantCast" })
	public SubstringFunction(
			JpaExpression<String> value,
			JpaExpression<Integer> start,
			CriteriaNodeBuilder criteriaBuilder) {
		this( value, start, (JpaExpression<Integer>)null, criteriaBuilder );
	}

	public SubstringFunction(
			JpaExpression<String> value,
			int start,
			CriteriaNodeBuilder criteriaBuilder) {
		this(
				value,
				criteriaBuilder.literal( start ),
				criteriaBuilder
		);
	}

	public SubstringFunction(
			JpaExpression<String> value,
			int start,
			int length,
			CriteriaNodeBuilder criteriaBuilder) {
		this(
				value,
				criteriaBuilder.literal( start ),
				criteriaBuilder.literal( length ),
				criteriaBuilder
		);
	}

	public JpaExpression<Integer> getLength() {
		return length;
	}

	public JpaExpression<Integer> getStart() {
		return start;
	}

	public JpaExpression<String> getValue() {
		return value;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitSubstringFunction( this );
	}
}
