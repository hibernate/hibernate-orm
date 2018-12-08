/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaExpression;

/**
 * Models the ANSI SQL <tt>LOCATE</tt> function.
 *
 * @author Steve Ebersole
 */
public class LocateFunction extends AbstractStandardFunction<Integer> {
	public static final String NAME = "locate";

	private final JpaExpression<String> pattern;
	private final JpaExpression<String> string;
	private final JpaExpression<Integer> start;

	public LocateFunction(
			JpaExpression<String> pattern,
			JpaExpression<String> string,
			JpaExpression<Integer> start,
			CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Integer.class, criteriaBuilder );

		this.pattern = pattern;
		this.string = string;
		this.start = start;
	}

	public JpaExpression<String> getPattern() {
		return pattern;
	}

	public JpaExpression<Integer> getStart() {
		return start;
	}

	public JpaExpression<String> getString() {
		return string;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitLocateFunction( this );
	}
}
