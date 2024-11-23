/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.criteria.LiteralHandlingMode;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.ValueHandlerFactory.ValueHandler;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

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

	@Override
	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	@Override
	public String render(RenderingContext renderingContext) {
		// In the case of literals, we currently do not have an easy way to get the value.
		// That would require some significant infrastructure changes.
		// For now, we force the normalRender() code path for enums which means we will
		// always use parameter binding for enum literals.
		if ( literal instanceof Enum ) {
			return normalRender( renderingContext, LiteralHandlingMode.BIND );
		}

		switch ( renderingContext.getClauseStack().getCurrent() ) {
			case SELECT: {
				return renderProjection( renderingContext );
			}
			case GROUP: {
				// technically a literal in the group-by clause
				// would be a reference to the position of a selection
				//
				// but this is what the code used to do...
				return renderProjection( renderingContext );
			}
			default: {
				return normalRender( renderingContext, renderingContext.getCriteriaLiteralHandlingMode() );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private String normalRender(RenderingContext renderingContext, LiteralHandlingMode literalHandlingMode) {
		switch ( literalHandlingMode ) {
			case AUTO: {
				if ( ValueHandlerFactory.isNumeric( literal ) ) {
					return ValueHandlerFactory.determineAppropriateHandler( (Class) literal.getClass() ).render( literal );
				}
				else {
					return bindLiteral( renderingContext );
				}
			}
			case BIND: {
				return bindLiteral( renderingContext );
			}
			case INLINE: {
				Object literalValue = literal;
				if ( String.class.equals( literal.getClass() ) ) {
					literalValue = renderingContext.getDialect().inlineLiteral( (String) literal );
				}

				ValueHandler valueHandler = ValueHandlerFactory.determineAppropriateHandler( (Class) literal.getClass() );
				if ( valueHandler == null ) {
					return bindLiteral( renderingContext );
				}

				return ValueHandlerFactory.determineAppropriateHandler( (Class) literal.getClass() ).render( literalValue );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected LiteralHandlingMode: " + literalHandlingMode );
			}
		}
	}

	private String renderProjection(RenderingContext renderingContext) {
		if ( ValueHandlerFactory.isCharacter( literal ) ) {
			// In case literal is a Character, pass literal.toString() as the argument.
			return renderingContext.getDialect().inlineLiteral( literal.toString() );
		}

		// some drivers/servers do not like parameters in the select clause
		final ValueHandlerFactory.ValueHandler handler =
				ValueHandlerFactory.determineAppropriateHandler( literal.getClass() );

		if ( handler == null ) {
			return normalRender( renderingContext, LiteralHandlingMode.BIND );
		}
		else {
			return handler.render( literal );
		}
	}

	private String bindLiteral(RenderingContext renderingContext) {
		final String parameterName = renderingContext.registerLiteralParameterBinding( getLiteral(), getJavaType() );
		return ':' + parameterName;
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
