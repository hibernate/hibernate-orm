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
 * Models the ANSI SQL <tt>LOWER</tt> function.
 *
 * @author Steve Ebersole
 */
public class LowerFunction extends AbstractStandardFunction<String> {
	public static final String NAME = "lower";
	private final JpaExpression<String> argument;

	public LowerFunction(JpaExpression<String> argument, CriteriaNodeBuilder criteriaBuilder) {
		super( NAME, String.class, criteriaBuilder );
		this.argument = argument;
	}

	public JpaExpression<String> getArgument() {
		return argument;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitLowerFunction( this );
	}
}
