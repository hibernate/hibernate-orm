/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.ValueHandlerFactory;
import org.hibernate.jpa.criteria.compile.RenderingContext;

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
	public String render(RenderingContext renderingContext) {
		if ( ValueHandlerFactory.isNumeric( literal ) ) {
			return ValueHandlerFactory.determineAppropriateHandler( (Class) literal.getClass() ).render( literal );
		}

		// else...
		final String parameterName = renderingContext.registerLiteralParameterBinding( getLiteral(), getJavaType() );
		return ':' + parameterName;
	}

	@SuppressWarnings({ "unchecked" })
	public String renderProjection(RenderingContext renderingContext) {
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
