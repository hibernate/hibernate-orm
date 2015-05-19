/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression.function;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>SQRT</tt> function.
 *
 * @author Steve Ebersole
 */
public class SqrtFunction
		extends ParameterizedFunctionExpression<Double>
		implements Serializable {
	public static final String NAME = "sqrt";

	public SqrtFunction(CriteriaBuilderImpl criteriaBuilder, Expression<? extends Number> expression) {
		super( criteriaBuilder, Double.class, NAME, expression );
	}

	@Override
	protected boolean isStandardJpaFunction() {
		return true;
	}
}
