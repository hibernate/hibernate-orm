/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.ExplicitParameterInfo;
import org.hibernate.jpa.criteria.compile.RenderingContext;

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

	public String render(RenderingContext renderingContext) {
		final ExplicitParameterInfo parameterInfo = renderingContext.registerExplicitParameter( this );
		return parameterInfo.render();
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
