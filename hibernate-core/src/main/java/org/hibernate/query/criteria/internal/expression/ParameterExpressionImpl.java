/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 *
 * @author Steve Ebersole
 */
public class ParameterExpressionImpl<T>
		extends ExpressionImpl<T>
		implements ParameterExpression<T>, Serializable {
	private final String name;
	private final Integer position;

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			String name) {
		super( criteriaBuilder, javaType );
		this.name = name;
		this.position = null;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Integer position) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = position;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = null;
	}

	public String getName() {
		return name;
	}

	public Integer getPosition() {
		return position;
	}

	public Class<T> getParameterType() {
		return getJavaType();
	}

	public void registerParameters(ParameterRegistry registry) {
		registry.registerParameter( this );
	}
}
