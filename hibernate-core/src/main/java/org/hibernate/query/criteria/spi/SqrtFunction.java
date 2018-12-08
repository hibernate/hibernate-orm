/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;
import java.util.Collections;

import org.hibernate.query.criteria.JpaExpression;

/**
 * Models the ANSI SQL <tt>SQRT</tt> function.
 *
 * @author Steve Ebersole
 */
public class SqrtFunction extends AbstractStandardFunction<Double> {
	public static final String NAME = "sqrt";
	private final JpaExpression<? extends Number> argument;

	public SqrtFunction(JpaExpression<? extends Number> argument, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, Double.class, criteriaBuilder );
		this.argument = argument;
	}

	public JpaExpression<? extends Number> getArgument() {
		return argument;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitSqrtFunction( this );
	}
}
