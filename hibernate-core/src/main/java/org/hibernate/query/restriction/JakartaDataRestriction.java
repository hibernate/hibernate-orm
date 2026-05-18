/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.restriction;

import org.hibernate.Internal;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;

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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Adapts Jakarta Data restrictions to Hibernate query restrictions.
 * @author Gavin King
 * @since 8.0
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
		return constraintPredicate( expression, restriction.constraint(), root, builder );
	}

	public static Predicate predicate(
			jakarta.persistence.criteria.Expression<?> expression,
			Constraint<?> constraint,
			Root<?> root,
			CriteriaBuilder builder) {
		return constraintPredicate(
				requireNonNull( expression, "expression" ),
				requireNonNull( constraint, "constraint" ),
				requireNonNull( root, "root" ),
				requireNonNull( builder, "builder" )
		);
	}

	private static Predicate constraintPredicate(
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
			return between(
					expression,
					expression( between.lowerBound(), root, builder ),
					expression( between.upperBound(), root, builder ),
					false
			);
		}
		else if ( constraint instanceof NotBetween<?> notBetween ) {
			return between(
					expression,
					expression( notBetween.lowerBound(), root, builder ),
					expression( notBetween.upperBound(), root, builder ),
					true
			);
		}
		else if ( constraint instanceof GreaterThan<?> greaterThan ) {
			return comparison(
					expression,
					ComparisonOperator.GREATER_THAN,
					expression( greaterThan.bound(), root, builder )
			);
		}
		else if ( constraint instanceof LessThan<?> lessThan ) {
			return comparison(
					expression,
					ComparisonOperator.LESS_THAN,
					expression( lessThan.bound(), root, builder )
			);
		}
		else if ( constraint instanceof AtLeast<?> atLeast ) {
			return comparison(
					expression,
					ComparisonOperator.GREATER_THAN_OR_EQUAL,
					expression( atLeast.bound(), root, builder )
			);
		}
		else if ( constraint instanceof AtMost<?> atMost ) {
			return comparison(
					expression,
					ComparisonOperator.LESS_THAN_OR_EQUAL,
					expression( atMost.bound(), root, builder )
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
		final var values = new ArrayList<jakarta.persistence.criteria.Expression<?>>( expressions.size() );
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
		return expression.in( values.toArray( jakarta.persistence.criteria.Expression<?>[]::new ) );
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
		else if ( expression instanceof CurrentDate<?> ) {
			return hibernateCriteriaBuilder( builder ).localDate();
		}
		else if ( expression instanceof CurrentTime<?> ) {
			return hibernateCriteriaBuilder( builder ).localTime();
		}
		else if ( expression instanceof CurrentDateTime<?> ) {
			return hibernateCriteriaBuilder( builder ).localDateTime();
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
					abs( numberExpression( arguments.get( 0 ), root, builder ), builder );
			case NumericFunctionExpression.NEG ->
					neg( numberExpression( arguments.get( 0 ), root, builder ), builder );
			case NumericFunctionExpression.LENGTH ->
					builder.length( stringExpression( arguments.get( 0 ), root, builder ) );
			default -> function( function, root, builder );
		};
	}

	private static jakarta.persistence.criteria.Expression<?> numericCast(
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
		else if ( expression instanceof JpaExpression<?> jpaExpression ) {
			return hibernateCriteriaBuilder( builder ).cast( jpaExpression, cast.type() );
		}
		else {
			throw new UnsupportedOperationException(
					"Unsupported Jakarta Data numeric cast target type: " + cast.type().getName() );
		}
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
			case DIVIDE -> new SqmBinaryArithmetic<>(
					BinaryArithmeticOperator.DIVIDE_PORTABLE,
					sqmExpression( left ),
					sqmExpression( right ),
					nodeBuilder( left )
			);
		};
	}

	private static Predicate comparison(
			jakarta.persistence.criteria.Expression<?> expression,
			ComparisonOperator operator,
			jakarta.persistence.criteria.Expression<?> bound) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( bound, Comparable.class );
		return new SqmComparisonPredicate(
				sqmExpression( expression ),
				operator,
				sqmExpression( bound ),
				nodeBuilder( expression )
		);
	}

	private static Predicate between(
			jakarta.persistence.criteria.Expression<?> expression,
			jakarta.persistence.criteria.Expression<?> lowerBound,
			jakarta.persistence.criteria.Expression<?> upperBound,
			boolean negated) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( lowerBound, Comparable.class );
		verifyExpressionType( upperBound, Comparable.class );
		return new SqmBetweenPredicate(
				sqmExpression( expression ),
				sqmExpression( lowerBound ),
				sqmExpression( upperBound ),
				negated,
				nodeBuilder( expression )
		);
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

	private static jakarta.persistence.criteria.Expression<? extends Number> numberExpression(
			jakarta.data.expression.Expression<?, ?> expression,
			Root<?> root,
			CriteriaBuilder builder) {
		final var expressionType = wrapperType( expression.type() );
		if ( !Number.class.isAssignableFrom( expressionType ) ) {
			throw new UnsupportedOperationException(
					"Expected " + Number.class.getName() + " expression but got: " + expressionType.getName() );
		}
		return typedNumberExpression( expression( expression, root, builder ), expressionType.asSubclass( Number.class ) );
	}

	private static <N extends Number> jakarta.persistence.criteria.Expression<N> typedNumberExpression(
			jakarta.persistence.criteria.Expression<?> expression,
			Class<N> expectedType) {
		verifyExpressionType( expression, expectedType );
		return expression.as( expectedType );
	}

	private static <N extends Number> jakarta.persistence.criteria.Expression<N> abs(
			jakarta.persistence.criteria.Expression<N> expression,
			CriteriaBuilder builder) {
		return builder.abs( expression );
	}

	private static <N extends Number> jakarta.persistence.criteria.Expression<N> neg(
			jakarta.persistence.criteria.Expression<N> expression,
			CriteriaBuilder builder) {
		return builder.neg( expression );
	}

	private static <T> jakarta.persistence.criteria.Expression<T> typedExpression(
			jakarta.persistence.criteria.Expression<?> expression,
			Class<T> expectedType) {
		verifyExpressionType( expression, expectedType );
		return expression.as( expectedType );
	}

	private static jakarta.persistence.criteria.Expression<?> literal(
			Object value,
			jakarta.persistence.criteria.Expression<?> expression,
			CriteriaBuilder builder) {
		if ( value != null ) {
			return builder.literal( value );
		}
		else {
			final var javaType = wrapperType( expression.getJavaType() );
			return javaType == null
					? builder.nullLiteral( Object.class )
					: nullLiteral( javaType, builder );
		}
	}

	private static <T> jakarta.persistence.criteria.Expression<T> nullLiteral(Class<T> javaType, CriteriaBuilder builder) {
		return builder.nullLiteral( javaType );
	}

	private static SqmExpression<?> sqmExpression(jakarta.persistence.criteria.Expression<?> expression) {
		if ( expression instanceof SqmExpression<?> sqmExpression ) {
			return sqmExpression;
		}
		else {
			throw new UnsupportedOperationException(
					"Jakarta Data expression requires a Hibernate SQM expression but got: "
							+ expression.getClass().getName() );
		}
	}

	private static NodeBuilder nodeBuilder(jakarta.persistence.criteria.Expression<?> expression) {
		return sqmExpression( expression ).nodeBuilder();
	}

	private static void verifyExpressionType(
			jakarta.persistence.criteria.Expression<?> expression,
			Class<?> expectedType) {
		final Class<?> expressionType = expression.getJavaType();
		if ( expressionType != null ) {
			final var javaType = wrapperType( expressionType );
			if ( !expectedType.isAssignableFrom( javaType ) ) {
				throw new UnsupportedOperationException(
						"Expected " + expectedType.getName() + " expression but got: " + javaType.getName() );
			}
		}
	}

	private static void verifyAssignableExpressionType(
			jakarta.persistence.criteria.Expression<?> expression,
			jakarta.persistence.criteria.Expression<?> valueExpression) {
		final var expressionType = wrapperType( expression.getJavaType() );
		final var valueType = wrapperType( valueExpression.getJavaType() );
		if ( expressionType != null && valueType != null && !expressionType.isAssignableFrom( valueType ) ) {
			throw new UnsupportedOperationException(
					"Expected " + expressionType.getName() + " expression but got: " + valueType.getName() );
		}
	}

	private static void verifyValueType(
			jakarta.persistence.criteria.Expression<?> expression,
			Object value) {
		final var expressionType = wrapperType( expression.getJavaType() );
		if ( value != null && expressionType != null && !expressionType.isInstance( value ) ) {
			throw new UnsupportedOperationException(
					"Expected " + expressionType.getName() + " value but got: " + value.getClass().getName() );
		}
	}

	private static HibernateCriteriaBuilder hibernateCriteriaBuilder(CriteriaBuilder builder) {
		if ( builder instanceof HibernateCriteriaBuilder hibernateCriteriaBuilder ) {
			return hibernateCriteriaBuilder;
		}
		else {
			throw new UnsupportedOperationException(
					"Jakarta Data expression requires a Hibernate CriteriaBuilder but got: "
							+ builder.getClass().getName() );
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
