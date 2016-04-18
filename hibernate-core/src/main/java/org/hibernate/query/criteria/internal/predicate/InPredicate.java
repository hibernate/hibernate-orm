/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.ValueHandlerFactory;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.hibernate.query.criteria.internal.expression.ParameterExpressionImpl;
import org.hibernate.type.Type;

/**
 * Models an <tt>[NOT] IN</tt> restriction
 *
 * @author Steve Ebersole
 */
public class InPredicate<T>
		extends AbstractSimplePredicate
		implements CriteriaBuilderImpl.In<T>, Serializable {
	private final Expression<? extends T> expression;
	private final List<Expression<? extends T>> values;

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with an empty list of values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression) {
		this( criteriaBuilder, expression, new ArrayList<Expression<? extends T>>() );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Expression<? extends T>... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			List<Expression<? extends T>> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = values;
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			T... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Collection<T> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = new ArrayList<Expression<? extends T>>( values.size() );
		final Class<? extends T> javaType = expression.getJavaType();
		ValueHandlerFactory.ValueHandler<? extends T> valueHandler = javaType != null && ValueHandlerFactory.isNumeric(javaType)
				? ValueHandlerFactory.determineAppropriateHandler((Class<? extends T>) javaType)
				: new ValueHandlerFactory.NoOpValueHandler<T>();
		for ( T value : values ) {
			this.values.add(
					new LiteralExpression<T>( criteriaBuilder, valueHandler.convert( value ) )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<T> getExpression() {
		return ( Expression<T> ) expression;
	}

	public Expression<? extends T> getExpressionInternal() {
		return expression;
	}

	public List<Expression<? extends T>> getValues() {
		return values;
	}

	@Override
	public InPredicate<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	@Override
	public InPredicate<T> value(Expression<? extends T> value) {
		values.add( value );
		return this;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getExpressionInternal(), registry );
		for ( Expression value : getValues() ) {
			Helper.possibleParameter(value, registry);
		}
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		final StringBuilder buffer = new StringBuilder();
		final Expression exp = getExpression();
		if ( ParameterExpressionImpl.class.isInstance( exp ) ) {
			// technically we only need to CAST (afaik) if expression and all values are parameters.
			// but checking for that condition could take long time on a lon value list
			final ParameterExpressionImpl parameterExpression = (ParameterExpressionImpl) exp;
			final SessionFactoryImplementor sfi = criteriaBuilder().getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
			final Type mappingType = sfi.getTypeResolver().heuristicType( parameterExpression.getParameterType().getName() );
			buffer.append( "cast(" )
					.append( parameterExpression.render( renderingContext ) )
					.append( " as " )
					.append( mappingType.getName() )
					.append( ")" );
		}
		else {
			buffer.append( ( (Renderable) getExpression() ).render( renderingContext ) );
		}

		if ( isNegated ) {
			buffer.append( " not" );
		}
		buffer.append( " in " );

		// subquery expressions are already wrapped in parenthesis, so we only need to
		// render the parenthesis here if the values represent an explicit value list
		boolean isInSubqueryPredicate = getValues().size() == 1
				&& Subquery.class.isInstance( getValues().get( 0 ) );
		if ( isInSubqueryPredicate ) {
			buffer.append( ( (Renderable) getValues().get(0) ).render( renderingContext ) );
		}
		else {
			buffer.append( '(' );
			String sep = "";
			for ( Expression value : getValues() ) {
				buffer.append( sep )
						.append( ( (Renderable) value ).render( renderingContext ) );
				sep = ", ";
			}
			buffer.append( ')' );
		}
		return buffer.toString();
	}
}
