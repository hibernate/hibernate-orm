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
package org.hibernate.ejb.criteria.expression;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.CriteriaBuilder.Coalesce;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;

/**
 * Models an ANSI SQL <tt>COALESCE</tt> expression.  <tt>COALESCE</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class CoalesceExpression<T> extends ExpressionImpl<T> implements Coalesce<T>, Serializable {
	private final List<Expression<? extends T>> expressions;
	private Class<T> javaType;

	public CoalesceExpression(CriteriaBuilderImpl criteriaBuilder) {
		this( criteriaBuilder, null );
	}

	public CoalesceExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
		this.javaType = javaType;
		this.expressions = new ArrayList<Expression<? extends T>>();
	}

	@Override
	public Class<T> getJavaType() {
		return javaType;
	}

	public Coalesce<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	@SuppressWarnings({ "unchecked" })
	public Coalesce<T> value(Expression<? extends T> value) {
		expressions.add( value );
		if ( javaType == null ) {
			javaType = (Class<T>) value.getJavaType();
		}
		return this;
	}

	public List<Expression<? extends T>> getExpressions() {
		return expressions;
	}

	public void registerParameters(ParameterRegistry registry) {
		for ( Expression expression : getExpressions() ) {
			Helper.possibleParameter(expression, registry);
		}
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		StringBuilder buffer = new StringBuilder( "coalesce(" );
		String sep = "";
		for ( Expression expression : getExpressions() ) {
			buffer.append( sep )
					.append( ( (Renderable) expression ).render( renderingContext ) );
			sep = ", ";
		}
		return buffer.append( ")" ).toString();
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
