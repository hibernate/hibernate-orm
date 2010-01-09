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
import javax.persistence.TypedQuery;

import org.hibernate.ejb.criteria.ValueHandlerFactory;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;

/**
 * Represents a literal expression.
 *
 * @author Steve Ebersole
 */
public class LiteralExpression<T> extends ExpressionImpl<T> implements Serializable {
	private Object literal;

	@SuppressWarnings({ "unchecked" })
	public LiteralExpression(CriteriaBuilderImpl criteriaBuilder, T literal) {
		this( criteriaBuilder, (Class<T>) determineClass( literal ), literal );
	}

	private static Class determineClass(Object literal) {
		return literal == null ? null : literal.getClass();
	}

	public LiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type, T literal) {
		super( criteriaBuilder, type );
		this.literal = literal;
	}

	@SuppressWarnings({ "unchecked" })
	public T getLiteral() {
		return (T) literal;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	@SuppressWarnings({ "unchecked" })
	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		if ( ValueHandlerFactory.isNumeric( literal ) ) {
			return ValueHandlerFactory.determineAppropriateHandler( (Class) literal.getClass() ).render( literal );
		}

		// else...
		final String parameterName = renderingContext.generateParameterName();
		renderingContext.registerImplicitParameterBinding(
				new CriteriaQueryCompiler.ImplicitParameterBinding() {
					public String getParameterName() {
						return parameterName;
					}

					public Class getJavaType() {
						return LiteralExpression.this.getJavaType();
					}

					public void bind(TypedQuery typedQuery) {
						typedQuery.setParameter( parameterName, getLiteral() );
					}
				}
		);
		return ':' + parameterName;
	}

	@SuppressWarnings({ "unchecked" })
	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		// some drivers/servers do not like parameters in the select clause
		final ValueHandlerFactory.ValueHandler handler =
				ValueHandlerFactory.determineAppropriateHandler( literal.getClass() );
		if ( ValueHandlerFactory.isCharacter( literal ) ) {
			return '\'' + handler.render( literal ) + '\'';
		}
		else {
			return handler.render( literal );
		}
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void resetJavaType(Class targetType) {
		super.resetJavaType( targetType );
		ValueHandlerFactory.ValueHandler valueHandler = getValueHandler();
		if ( valueHandler == null ) {
			valueHandler = ValueHandlerFactory.determineAppropriateHandler( targetType );
			forceConversion( valueHandler );
		}

		if ( valueHandler != null ) {
			literal = valueHandler.convert( literal );
		}
	}
}
