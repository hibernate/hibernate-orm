/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;

/**
 * Models the ANSI SQL <tt>LENGTH</tt> function.
 *
 * @author Steve Ebersole
 */
public class LengthFunction
		extends AbstractStandardFunction<Integer>
		implements Serializable {
	public static final String NAME = "length";
	private final ExpressionImplementor<String> argument;

	public LengthFunction(ExpressionImplementor<String> argument, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Integer.class, criteriaBuilder );
		this.argument = argument;
	}

	public ExpressionImplementor<String> getArgument() {
		return argument;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitLengthFunction( this );
	}
}
