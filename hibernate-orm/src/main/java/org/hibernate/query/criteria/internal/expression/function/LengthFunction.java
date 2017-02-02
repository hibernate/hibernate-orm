/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>LENGTH</tt> function.
 *
 * @author Steve Ebersole
 */
public class LengthFunction
		extends ParameterizedFunctionExpression<Integer>
		implements Serializable {
	public static final String NAME = "length";

	@Override
	protected boolean isStandardJpaFunction() {
		return true;
	}

	public LengthFunction(CriteriaBuilderImpl criteriaBuilder, Expression<String> value) {
		super( criteriaBuilder, Integer.class, NAME, value );
	}
}
