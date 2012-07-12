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
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Models an ANSI SQL <tt>NULLIF</tt> expression.  <tt>NULLIF</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class NullifExpression<T> extends ExpressionImpl<T> implements Serializable {
	private final Expression<? extends T> primaryExpression;
	private final Expression<?> secondaryExpression;

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Expression<?> secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = secondaryExpression;
	}

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Object secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = new LiteralExpression( criteriaBuilder, secondaryExpression );
	}

	private static Class determineType(Class javaType, Expression primaryExpression) {
		return javaType != null ? javaType : primaryExpression.getJavaType();
	}

	public Expression<? extends T> getPrimaryExpression() {
		return primaryExpression;
	}

	public Expression<?> getSecondaryExpression() {
		return secondaryExpression;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getPrimaryExpression(), registry );
		Helper.possibleParameter( getSecondaryExpression(), registry );
	}

	public String render(RenderingContext renderingContext) {
		return "nullif("
				+ ( (Renderable) getPrimaryExpression() ).render( renderingContext )
				+ ','
				+ ( (Renderable) getSecondaryExpression() ).render( renderingContext )
				+ ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
