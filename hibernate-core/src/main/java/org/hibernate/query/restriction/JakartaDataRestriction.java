/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import org.hibernate.Internal;

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
import jakarta.data.restrict.Restrict;
import jakarta.data.spi.expression.function.FunctionExpression;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.NumericOperatorExpression;
import jakarta.data.spi.expression.function.TextFunctionExpression;
import jakarta.data.spi.expression.literal.Literal;
import jakarta.data.spi.expression.path.Path;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Adapts Jakarta Data restrictions to Hibernate query restrictions.
 */
@Internal
public final class JakartaDataRestriction {
	private JakartaDataRestriction() {
	}

	public static <T> Restriction<T> from(jakarta.data.restrict.Restriction<? super T> restriction) {
		return new Adapter<>( requireNonNull( restriction, "restriction" ) );
	}

	public static <T> Restriction<T> all(
			List<? extends jakarta.data.restrict.Restriction<? super T>> restrictions) {
		return from( Restrict.all( restrictions ) );
	}

	@SafeVarargs
	public static <T> Restriction<T> all(jakarta.data.restrict.Restriction<? super T>... restrictions) {
		return from( Restrict.all( restrictions ) );
	}

	private record Adapter<T>(jakarta.data.restrict.Restriction<? super T> restriction)
			implements Restriction<T> {
		@Override
		public Restriction<T> negated() {
			return from( restriction.negate() );
		}

		@Override
		public Predicate toPredicate(Root<? extends T> root, CriteriaBuilder builder) {
			return JakartaDataRestriction.restriction( restriction, root, builder );
		}
	}

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

	private static Predicate compositeRestriction(
			CompositeRestriction<?> restriction,
			Root<?> root,
			CriteriaBuilder builder) {
		final var predicate =
				switch ( restriction.type() ) {
					case ALL -> builder.and( restrictions( restriction.restrictions(), root, builder ) );
					case ANY -> builder.or( restrictions( restriction.restrictions(), root, builder ) );
				};
		return restriction.isNegated() ? builder.not( predicate ) : predicate;
	}

	private static Predicate[] restrictions(
			List<? extends jakarta.data.restrict.Restriction<?>> restrictions,
			Root<?> root,
			CriteriaBuilder builder) {
		return restrictions.stream()
				.map( restriction -> restriction( restriction, root, builder ) )
				.toArray( Predicate[]::new );
	}

	private static Predicate basicRestriction(
			BasicRestriction<?, ?> restriction,
			Root<?> root,
			CriteriaBuilder builder) {
		final var expression = expression( restriction.expression(), root, builder );
		return constraint( expression, restriction.constraint(), root, builder );
	}

	private static Predicate constraint(
			jakarta.persistence.criteria.Expression<?> expression,
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
			return builder.between(
					comparableExpression( expression ),
					comparableExpression( expression( between.lowerBound(), root, builder ) ),
					comparableExpression( expression( between.upperBound(), root, builder ) )
			);
		}
		else if ( constraint instanceof NotBetween<?> notBetween ) {
			return builder.not( builder.between(
					comparableExpression( expression ),
					comparableExpression( expression( notBetween.lowerBound(), root, builder ) ),
					comparableExpression( expression( notBetween.upperBound(), root, builder ) )
			) );
		}
		else if ( constraint instanceof GreaterThan<?> greaterThan ) {
			return builder.greaterThan(
					comparableExpression( expression ),
					comparableExpression( expression( greaterThan.bound(), root, builder ) )
			);
		}
		else if ( constraint instanceof LessThan<?> lessThan ) {
			return builder.lessThan(
					comparableExpression( expression ),
					comparableExpression( expression( lessThan.bound(), root, builder ) )
			);
		}
		else if ( constraint instanceof AtLeast<?> atLeast ) {
			return builder.greaterThanOrEqualTo(
					comparableExpression( expression ),
					comparableExpression( expression( atLeast.bound(), root, builder ) )
			);
		}
		else if ( constraint instanceof AtMost<?> atMost ) {
			return builder.lessThanOrEqualTo(
					comparableExpression( expression ),
					comparableExpression( expression( atMost.bound(), root, builder ) )
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
			throw new UnsupportedOperationException(
					"Unsupported Jakarta Data constraint type: " + constraint.getClass().getName() );
		}
	}

	private static Predicate in(
			jakarta.persistence.criteria.Expression<?> expression,
			List<? extends jakarta.data.expression.Expression<?, ?>> expressions,
			Root<?> root,
			CriteriaBuilder builder) {
		final var in = builder.in( objectExpression( expression ) );
		for ( var value : expressions ) {
			if ( value instanceof Literal<?> literal ) {
				in.value( literal.value() );
			}
			else {
				in.value( objectExpression( expression( value, root, builder ) ) );
			}
		}
		return in;
	}

	private static jakarta.persistence.criteria.Expression<?> expression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		if ( expression instanceof Attribute<?> attribute ) {
			return root.get( attribute.name() );
		}
		else if ( expression instanceof Path<?, ?> path ) {
			return path( path, root ).get( path.attribute().name() );
		}
		else if ( expression instanceof Literal<?> literal ) {
			return builder.literal( literal.value() );
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
			throw new UnsupportedOperationException(
					"Unsupported Jakarta Data expression type: " + expression.getClass().getName() );
		}
	}

	private static jakarta.persistence.criteria.Path<?> path(Path<?, ?> path, Root<?> root) {
		return path( path.expression(), root );
	}

	private static jakarta.persistence.criteria.Path<?> path(NavigableExpression<?, ?> expression, Root<?> root) {
		if ( expression instanceof Attribute<?> attribute ) {
			return root.get( attribute.name() );
		}
		else if ( expression instanceof Path<?, ?> path ) {
			return path( path.expression(), root ).get( path.attribute().name() );
		}
		else {
			throw new UnsupportedOperationException(
					"Unsupported Jakarta Data path expression type: " + expression.getClass().getName() );
		}
	}

	private static jakarta.persistence.criteria.Expression<?> textFunction(
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

	private static jakarta.persistence.criteria.Expression<?> numericFunction(
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

	private static jakarta.persistence.criteria.Expression<?> numericOperation(
			NumericOperatorExpression<?, ?> operation,
			Root<?> root,
			CriteriaBuilder builder) {
		final var left = numberExpression( operation.left(), root, builder );
		final var right = numberExpression( operation.right(), root, builder );
		return switch ( operation.operator() ) {
			case PLUS -> builder.sum( left, right );
			case MINUS -> builder.diff( left, right );
			case TIMES -> builder.prod( left, right );
			case DIVIDE -> builder.quot( left, right );
		};
	}

	private static jakarta.persistence.criteria.Expression<?> function(
			FunctionExpression<?, ?> function,
			Root<?> root,
			CriteriaBuilder builder) {
		return builder.function(
				function.name(),
				function.type(),
				function.arguments().stream()
						.map( argument -> expression( argument, root, builder ) )
						.toArray( jakarta.persistence.criteria.Expression<?>[]::new )
		);
	}

	private static jakarta.persistence.criteria.Expression<String> stringExpression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		return stringExpression( expression( expression, root, builder ) );
	}

	private static jakarta.persistence.criteria.Expression<String> stringExpression(
			jakarta.persistence.criteria.Expression<?> expression) {
		return typedExpression( expression, String.class );
	}

	private static jakarta.persistence.criteria.Expression<Number> numberExpression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		return numberExpression( expression( expression, root, builder ) );
	}

	private static jakarta.persistence.criteria.Expression<Number> numberExpression(
			jakarta.persistence.criteria.Expression<?> expression) {
		return typedExpression( expression, Number.class );
	}

	private static jakarta.persistence.criteria.Expression<Comparable<Object>> comparableExpression(
			jakarta.persistence.criteria.Expression<?> expression) {
		verifyExpressionType( expression, Comparable.class );
		return castExpression( expression );
	}

	private static jakarta.persistence.criteria.Expression<Object> objectExpression(
			jakarta.persistence.criteria.Expression<?> expression) {
		return castExpression( expression );
	}

	private static <T> jakarta.persistence.criteria.Expression<T> typedExpression(
			jakarta.persistence.criteria.Expression<?> expression,
			Class<T> expectedType) {
		verifyExpressionType( expression, expectedType );
		return castExpression( expression );
	}

	@SuppressWarnings("unchecked")
	private static <T> jakarta.persistence.criteria.Expression<T> castExpression(
			jakarta.persistence.criteria.Expression<?> expression) {
		return (jakarta.persistence.criteria.Expression<T>) expression;
	}

	private static void verifyExpressionType(
			jakarta.persistence.criteria.Expression<?> expression,
			Class<?> expectedType) {
		final Class<?> javaType = wrapperType( expression.getJavaType() );
		if ( javaType != null && !expectedType.isAssignableFrom( javaType ) ) {
			throw new UnsupportedOperationException(
					"Expected " + expectedType.getName() + " expression but got: " + javaType.getName() );
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
			throw new UnsupportedOperationException(
					"Expected an integer literal but got: " + expression.getClass().getName() );
		}
	}
}
