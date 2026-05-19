/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import org.hibernate.Internal;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.NodeBuilder;

import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.In;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotIn;
import jakarta.data.constraint.NotLike;
import jakarta.data.constraint.NotNull;
import jakarta.data.constraint.Null;
import jakarta.data.expression.NavigableExpression;
import jakarta.data.metamodel.Attribute;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.CompositeRestriction;
import jakarta.data.spi.expression.function.CurrentDate;
import jakarta.data.spi.expression.function.CurrentDateTime;
import jakarta.data.spi.expression.function.CurrentTime;
import jakarta.data.spi.expression.function.FunctionExpression;
import jakarta.data.spi.expression.function.NumericCast;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.NumericOperatorExpression;
import jakarta.data.spi.expression.function.TextFunctionExpression;
import jakarta.data.spi.expression.literal.Literal;
import jakarta.data.spi.expression.path.Path;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Converts {@linkplain jakarta.data.restrict.Restriction Jakarta Data
 * restrictions} to {@linkplain Restriction Hibernate query restrictions} and
 * {@linkplain jakarta.data.constraint.Constraint Jakarta Data constraints} to
 * {@linkplain jakarta.persistence.criteria.Predicate JPA predicates}. The
 * operations of this class act as a bridge between the Jakarta Data APIs and
 * the native restriction API, which is itself a facade over the JPA Criteria
 * API.
 *
 * @author Gavin King
 * @since 8.0
 */
@Internal
public final class JakartaDataRestriction {
	private JakartaDataRestriction() {
	}

	/**
	 * Exposes a {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}
	 * as a {@linkplain Restriction Hibernate query restriction}, allowing it to be used
	 * in a specification.
	 */
	private record Adapter<T>(jakarta.data.restrict.Restriction<? super T> restriction)
			implements Restriction<T> {
		private Adapter {
			requireNonNull( restriction, "missing restriction" );
		}

		@Override
		public Restriction<T> negated() {
			return adaptRestriction( restriction.negate() );
		}

		@Override
		public Predicate toPredicate(Root<? extends T> root, CriteriaBuilder builder) {
			return JakartaDataRestriction.restriction( restriction, root, builder );
		}
	}

	/**
	 * Adapt the given {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}.
	 */
	public static <T> Restriction<T> adaptRestriction(jakarta.data.restrict.Restriction<? super T> restriction) {
		return new Adapter<>( restriction );
	}

	/**
	 * Obtain a {@linkplain Predicate JPA criteria predicate} representing the given
	 * {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}.
	 */
	private static Predicate restriction(
			jakarta.data.restrict.Restriction<?> restriction,
			Root<?> root,
			CriteriaBuilder builder) {
		if ( restriction instanceof BasicRestriction<?, ?> basicRestriction ) {
			return basicRestriction( basicRestriction, root, builder );
		}
		else if ( restriction instanceof CompositeRestriction<?> compositeRestriction ) {
			return compositeRestriction( compositeRestriction, root, builder );
		}
		else {
			throw new UnsupportedOperationException(
					"Unsupported Jakarta Data restriction type: " + restriction.getClass().getName() );
		}
	}

	/**
	 * Obtain a {@linkplain Predicate JPA criteria predicate} representing the given
	 * {@linkplain CompositeRestriction composite Jakarta Data restriction}.
	 */
	private static Predicate compositeRestriction(
			CompositeRestriction<?> compositeRestriction,
			Root<?> root,
			CriteriaBuilder builder) {
		var restrictions = compositeRestriction.restrictions();
		final List<Predicate> list = new ArrayList<>( restrictions.size() );
		for ( var restriction : restrictions ) {
			list.add( restriction( restriction, root, builder ) );
		}
		final var predicates = list.toArray( Predicate[]::new );
		final var predicate =
				switch ( compositeRestriction.type() ) {
					case ALL -> builder.and( predicates );
					case ANY -> builder.or( predicates );
				};
		return compositeRestriction.isNegated() ? builder.not( predicate ) : predicate;
	}

	/**
	 * Obtain a {@linkplain Predicate JPA criteria predicate} representing the given
	 * {@linkplain BasicRestriction basic Jakarta Data restriction}.
	 */
	private static Predicate basicRestriction(
			BasicRestriction<?, ?> restriction,
			Root<?> root,
			CriteriaBuilder builder) {
		final var expression = expression( restriction.expression(), root, builder );
		return constraintPredicate( expression, restriction.constraint(), root, builder );
	}

	/**
	 * Apply the given {@linkplain jakarta.data.constraint.Constraint Jakarta Data constraint}
	 * to the given {@linkplain Expression JPA expression}, returning a {@linkplain Predicate
	 * JPA criteria predicate}.
	 */
	public static Predicate applyConstraint(
			Expression<?> expression,
			Constraint<?> constraint,
			Root<?> root,
			CriteriaBuilder builder) {
		requireNonNull( expression, "missing expression" );
		requireNonNull( constraint, "missing constraint" );
		requireNonNull( root, "missing root" );
		requireNonNull( builder, "missing builder" );
		return constraintPredicate( expression, constraint, root, builder );
	}

	/**
	 * Obtain a {@linkplain Predicate JPA criteria predicate} representing the given
	 * {@linkplain Constraint Jakarta Data constraint} applied to the given
	 * {@linkplain Expression JPA expression}.
	 */
	private static Predicate constraintPredicate(
			Expression<?> expression,
			Constraint<?> constraint,
			Root<?> root,
			CriteriaBuilder builder) {
		if ( constraint instanceof EqualTo<?> equalTo ) {
			return builder.equal( expression, expression( equalTo.expression(), root, builder ) );
		}
		else if ( constraint instanceof NotEqualTo<?> notEqualTo ) {
			return builder.notEqual( expression, expression( notEqualTo.expression(), root, builder ) );
		}
		else if ( constraint instanceof In<?> in ) {
			return in( expression, in.expressions(), root, builder );
		}
		else if ( constraint instanceof NotIn<?> notIn ) {
			return builder.not( in( expression, notIn.expressions(), root, builder ) );
		}
		else if ( constraint instanceof Between<?> between ) {
			return between(
					expression,
					expression( between.lowerBound(), root, builder ),
					expression( between.upperBound(), root, builder ),
					false,
					builder
			);
		}
		else if ( constraint instanceof NotBetween<?> notBetween ) {
			return between(
					expression,
					expression( notBetween.lowerBound(), root, builder ),
					expression( notBetween.upperBound(), root, builder ),
					true,
					builder
			);
		}
		else if ( constraint instanceof GreaterThan<?> greaterThan ) {
			return comparison(
					expression,
					ComparisonOperator.GREATER_THAN,
					expression( greaterThan.bound(), root, builder ),
					builder
			);
		}
		else if ( constraint instanceof LessThan<?> lessThan ) {
			return comparison(
					expression,
					ComparisonOperator.LESS_THAN,
					expression( lessThan.bound(), root, builder ),
					builder
			);
		}
		else if ( constraint instanceof AtLeast<?> atLeast ) {
			return comparison(
					expression,
					ComparisonOperator.GREATER_THAN_OR_EQUAL,
					expression( atLeast.bound(), root, builder ),
					builder
			);
		}
		else if ( constraint instanceof AtMost<?> atMost ) {
			return comparison(
					expression,
					ComparisonOperator.LESS_THAN_OR_EQUAL,
					expression( atMost.bound(), root, builder ),
					builder
			);
		}
		else if ( constraint instanceof Like like ) {
			return builder.like(
					stringExpression( expression ),
					stringExpression( like.pattern(), root, builder ),
					like.escape()
			);
		}
		else if ( constraint instanceof NotLike notLike ) {
			return builder.notLike(
					stringExpression( expression ),
					stringExpression( notLike.pattern(), root, builder ),
					notLike.escape()
			);
		}
		else if ( constraint instanceof Null<?> ) {
			return builder.isNull( expression );
		}
		else if ( constraint instanceof NotNull<?> ) {
			return builder.isNotNull( expression );
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized constraint type '" + constraint.getClass().getName() + "'" );
		}
	}

	private static Predicate in(
			Expression<?> expression,
			List<? extends jakarta.data.expression.Expression<?, ?>> expressions,
			Root<?> root,
			CriteriaBuilder builder) {
		final ArrayList<Expression<?>> values = new ArrayList<>( expressions.size() );
		for ( var value : expressions ) {
			if ( value instanceof Literal<?> literal ) {
				verifyValueType( expression, literal.value() );
				values.add( literal( literal.value(), expression, builder ) );
			}
			else {
				final var valueExpression = expression( value, root, builder );
				verifyAssignableExpressionType( expression, valueExpression );
				values.add( valueExpression );
			}
		}
		return expression.in( values.toArray( Expression<?>[]::new ) );
	}

	/**
	 * Adapt the given {@linkplain jakarta.data.expression.Expression Jakarta Data expression}
	 * to a {@linkplain Expression JPA criteria expression}.
	 */
	private static Expression<?> expression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		if ( expression instanceof Attribute<?> attribute ) {
			return root.get( attribute.name() );
		}
		else if ( expression instanceof Path<?, ?> path ) {
			return path( path.expression(), root ).get( path.attribute().name() );
		}
		else if ( expression instanceof Literal<?> literal ) {
			return builder.literal( literal.value() );
		}
		else if ( expression instanceof CurrentDate<?> ) {
			return builder.localDate();
		}
		else if ( expression instanceof CurrentTime<?> ) {
			return builder.localTime();
		}
		else if ( expression instanceof CurrentDateTime<?> ) {
			return builder.localDateTime();
		}
		else if ( expression instanceof NumericCast<?, ?> numericCast ) {
			return numericCast( numericCast, root, builder );
		}
		else if ( expression instanceof TextFunctionExpression<?> function ) {
			return textFunction( function, root, builder );
		}
		else if ( expression instanceof NumericFunctionExpression<?, ?> function ) {
			return numericFunction( function, root, builder );
		}
		else if ( expression instanceof FunctionExpression<?, ?> function ) {
			return function( function, root, builder );
		}
		else if ( expression instanceof NumericOperatorExpression<?, ?> operation ) {
			return numericOperation( operation, root, builder );
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized expression type '" + expression.getClass().getName() + "'" );
		}
	}

	/**
	 * Adapt the given {@linkplain jakarta.data.expression.NavigableExpression Jakarta Data path}
	 * to a {@linkplain jakarta.persistence.criteria.Path JPA criteria path}.
	 */
	private static jakarta.persistence.criteria.Path<?> path(NavigableExpression<?, ?> expression, Root<?> root) {
		if ( expression instanceof Attribute<?> attribute ) {
			return root.get( attribute.name() );
		}
		else if ( expression instanceof Path<?, ?> path ) {
			return path( path.expression(), root ).get( path.attribute().name() );
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized path expression type '" + expression.getClass().getName() + "'" );
		}
	}

	/**
	 * Adapt the given {@linkplain TextFunctionExpression Jakarta Data text function}
	 * to a {@linkplain Expression JPA criteria expression}.
	 */
	private static Expression<?> textFunction(
			TextFunctionExpression<?> function,
			Root<?> root,
			CriteriaBuilder builder) {
		final var arguments = function.arguments();
		return switch ( function.name() ) {
			case TextFunctionExpression.LOWER ->
					builder.lower( stringExpression( arguments.get( 0 ), root, builder ) );
			case TextFunctionExpression.UPPER ->
					builder.upper( stringExpression( arguments.get( 0 ), root, builder ) );
			case TextFunctionExpression.CONCAT ->
					builder.concat(
							stringExpression( arguments.get( 0 ), root, builder ),
							stringExpression( arguments.get( 1 ), root, builder )
					);
			case TextFunctionExpression.LEFT ->
					builder.substring(
							stringExpression( arguments.get( 0 ), root, builder ),
							1,
							integerLiteral( arguments.get( 1 ) )
					);
			case TextFunctionExpression.RIGHT -> {
				final var text = stringExpression( arguments.get( 0 ), root, builder );
				final int length = integerLiteral( arguments.get( 1 ) );
				yield builder.substring( text,
						builder.diff( builder.length( text ), length - 1 ),
						builder.literal( length ) );
			}
			default -> function( function, root, builder );
		};
	}

	/**
	 * Adapt the given {@linkplain NumericFunctionExpression Jakarta Data numeric function}
	 * to a {@linkplain Expression JPA criteria expression}.
	 */
	private static Expression<?> numericFunction(
			NumericFunctionExpression<?, ?> function,
			Root<?> root,
			CriteriaBuilder builder) {
		final var arguments = function.arguments();
		return switch ( function.name() ) {
			case NumericFunctionExpression.ABS ->
					builder.abs( numberExpression( arguments.get( 0 ), root, builder ) );
			case NumericFunctionExpression.NEG ->
					builder.neg( numberExpression( arguments.get( 0 ), root, builder ) );
			case NumericFunctionExpression.LENGTH ->
					builder.length( stringExpression( arguments.get( 0 ), root, builder ) );
			default -> function( function, root, builder );
		};
	}

	/**
	 * Adapt the given {@linkplain NumericCast Jakarta Data numeric cast} to a
	 * {@linkplain Expression JPA criteria expression}.
	 */
	private static Expression<?> numericCast(
			NumericCast<?, ?> cast,
			Root<?> root,
			CriteriaBuilder builder) {
		final var expression = numberExpression( cast.expression(), root, builder );
		final var type = wrapperType( cast.type() );
		if ( type == Long.class ) {
			return builder.toLong( expression );
		}
		else if ( type == Double.class ) {
			return builder.toDouble( expression );
		}
		else if ( type == BigInteger.class ) {
			return builder.toBigInteger( expression );
		}
		else if ( type == BigDecimal.class ) {
			return builder.toBigDecimal( expression );
		}
		else {
			return expression.cast( cast.type() );
		}
	}

	/**
	 * Adapt the given {@linkplain NumericOperatorExpression Jakarta Data operator expression}
	 * to a {@linkplain Expression JPA criteria expression}.
	 */
	private static Expression<?> numericOperation(
			NumericOperatorExpression<?, ?> operation,
			Root<?> root,
			CriteriaBuilder builder) {
		final var left = numberExpression( operation.left(), root, builder );
		final var right = numberExpression( operation.right(), root, builder );
		return switch ( operation.operator() ) {
			case PLUS -> builder.sum( left, right );
			case MINUS -> builder.diff( left, right );
			case TIMES -> builder.prod( left, right );
			case DIVIDE -> nodeBuilder( builder ).quotPortable( left, right );
		};
	}

	private static Predicate comparison(
			Expression<?> expression,
			ComparisonOperator operator,
			Expression<?> bound,
			CriteriaBuilder builder) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( bound, Comparable.class );
		return nodeBuilder( builder ).comparison( expression, operator, bound );
	}

	private static Predicate between(
			Expression<?> expression,
			Expression<?> lowerBound,
			Expression<?> upperBound,
			boolean negated,
			CriteriaBuilder builder) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( lowerBound, Comparable.class );
		verifyExpressionType( upperBound, Comparable.class );
		return nodeBuilder( builder ).between( expression, lowerBound, upperBound, negated );
	}

	private static Expression<?> function(
			FunctionExpression<?, ?> function,
			Root<?> root,
			CriteriaBuilder builder) {
		final var arguments = function.arguments();
		final List<Expression<?>> list = new ArrayList<>( arguments.size() );
		for ( var argument : arguments ) {
			list.add( expression( argument, root, builder ) );
		}
		return builder.function( function.name(), function.type(),
				list.toArray( Expression<?>[]::new ) );
	}

	private static Expression<String> stringExpression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		return stringExpression( expression( expression, root, builder ) );
	}

	private static Expression<String> stringExpression(Expression<?> expression) {
		verifyExpressionType( expression, String.class );
		return expression.as( String.class );
	}

	private static Expression<? extends Number> numberExpression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		final var expressionType = wrapperType( expression.type() );
		if ( !Number.class.isAssignableFrom( expressionType ) ) {
			throw new IllegalArgumentException(
					"Expected 'Number' expression but got '" + expressionType.getName() + "'" );
		}
		return numberExpression( expression( expression, root, builder ),
				expressionType.asSubclass( Number.class ) );
	}

	private static <N extends Number> Expression<N> numberExpression(
			Expression<?> expression,
			Class<N> expectedType) {
		verifyExpressionType( expression, expectedType );
		return expression.as( expectedType );
	}

	private static Expression<?> literal(
			Object value,
			Expression<?> expression,
			CriteriaBuilder builder) {
		if ( value != null ) {
			return builder.literal( value );
		}
		else {
			final var javaType = wrapperType( expression.getJavaType() );
			return javaType == null
					? builder.nullLiteral( Object.class )
					: builder.nullLiteral( javaType );
		}
	}

	private static NodeBuilder nodeBuilder(CriteriaBuilder builder) {
		if ( builder instanceof NodeBuilder nodeBuilder ) {
			return nodeBuilder;
		}
		else {
			throw new IllegalArgumentException( "Not Hibernate CriteriaBuilder" );
		}
	}

	private static void verifyExpressionType(
			Expression<?> expression,
			Class<?> expectedType) {
		final var expressionType = expression.getJavaType();
		if ( expressionType != null ) {
			final var javaType = wrapperType( expressionType );
			if ( !expectedType.isAssignableFrom( javaType ) ) {
				throw new IllegalArgumentException(
						"Expected '" + expectedType.getName() + "' expression but got '" + javaType.getName() + "'" );
			}
		}
	}

	private static void verifyAssignableExpressionType(
			Expression<?> expression,
			Expression<?> valueExpression) {
		final var expressionType = wrapperType( expression.getJavaType() );
		final var valueType = wrapperType( valueExpression.getJavaType() );
		if ( expressionType != null && valueType != null && !expressionType.isAssignableFrom( valueType ) ) {
			throw new IllegalArgumentException(
					"Expected '" + expressionType.getName() + "' expression but got '" + valueType.getName() + "'" );
		}
	}

	private static void verifyValueType(Expression<?> expression, Object value) {
		final var expressionType = wrapperType( expression.getJavaType() );
		if ( value != null && expressionType != null && !expressionType.isInstance( value ) ) {
			throw new IllegalArgumentException(
					"Expected '" + expressionType.getName() + "' value but got '" + value.getClass().getName() + "'" );
		}
	}

	private static Class<?> wrapperType(Class<?> javaType) {
		if ( javaType == null || !javaType.isPrimitive() ) {
			return javaType;
		}
		else if ( javaType == boolean.class ) {
			return Boolean.class;
		}
		else if ( javaType == byte.class ) {
			return Byte.class;
		}
		else if ( javaType == char.class ) {
			return Character.class;
		}
		else if ( javaType == double.class ) {
			return Double.class;
		}
		else if ( javaType == float.class ) {
			return Float.class;
		}
		else if ( javaType == int.class ) {
			return Integer.class;
		}
		else if ( javaType == long.class ) {
			return Long.class;
		}
		else if ( javaType == short.class ) {
			return Short.class;
		}
		else {
			return Void.class;
		}
	}

	private static int integerLiteral(jakarta.data.expression.Expression<?, ?> expression) {
		if ( expression instanceof Literal<?> literal && literal.value() instanceof Integer integer ) {
			return integer;
		}
		else {
			throw new IllegalArgumentException(
					"Expected an integer literal but got '" + expression.getClass().getName() + "'" );
		}
	}
}
