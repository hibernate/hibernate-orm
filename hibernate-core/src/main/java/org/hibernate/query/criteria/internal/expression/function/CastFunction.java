/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.expression.AbstractExpression;

/**
 * Models a <tt>CAST</tt> function.
 *
 * @param <T> The cast result type.
 * @param <Y> The type of the expression to be cast.
 *
 * @author Steve Ebersole
 */
public class CastFunction<T,Y>
		extends BasicFunctionExpression<T>
		implements FunctionExpression<T>, Serializable {
	public static final String CAST_NAME = "cast";

	private final AbstractExpression<Y> castSource;

	public CastFunction(
			HibernateCriteriaBuilder criteriaBuilder,
			Class<T> javaType,
			AbstractExpression<Y> castSource) {
		super( criteriaBuilder, javaType, CAST_NAME );
		this.castSource = castSource;
	}

	public AbstractExpression<Y> getCastSource() {
		return castSource;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getCastSource(), registry );
	}
}
