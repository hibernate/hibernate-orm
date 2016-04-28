/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.ExplicitParameterInfo;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.type.Type;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 *
 * @author Steve Ebersole
 */
public class ParameterExpressionImpl<T>
		extends ExpressionImpl<T>
		implements ParameterExpression<T>, QueryParameter<T>, Serializable {
	private final String name;
	private final Integer position;
	private final Type expectedType;

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			String name,
			Type expectedType) {
		super( criteriaBuilder, javaType );
		this.name = name;
		this.position = null;
		this.expectedType = expectedType;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Integer position,
			Type expectedType) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = position;
		this.expectedType = expectedType;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Type expectedType) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = null;
		this.expectedType = expectedType;
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

	public String render(RenderingContext renderingContext) {
		final ExplicitParameterInfo parameterInfo = renderingContext.registerExplicitParameter( this );
		return parameterInfo.render();
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}

	@Override
	public Type getType() {
		return expectedType;
	}

	@Override
	public boolean isJpaPositionalParameter() {
		return true;
	}
}
