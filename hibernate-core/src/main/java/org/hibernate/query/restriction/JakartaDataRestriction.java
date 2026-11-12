package org.hibernate.query.restriction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.spi.NodeBuilder;

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
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.util.Objects.requireNonNull;

/**
 * Converts {@linkplain jakarta.data.restrict.Restriction Jakarta Data
 * restrictions} to {@linkplain Restriction Hibernate query restrictions} or
 * directly applies them to {@linkplain CriteriaQuery JPA criteria queries}, and
 * {@linkplain jakarta.data.constraint.Constraint Jakarta Data constraints} to
 * {@linkplain jakarta.persistence.criteria.Predicate JPA predicates}. The
 * operations of this class act as a bridge between the Jakarta Data APIs and
 * the native restriction API, which is itself a facade over the JPA Criteria
 * API.
 *
 * @apiNote This class is not intended for direct use by application programs.
 *          It is called by Jakarta Data repository implementations generated
 *          by Hibernate Data Repositories.
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
	private record Adapter<T>(@Nonnull jakarta.data.restrict.Restriction<? super T> restriction)
			implements Restriction<T> {
		private Adapter {
			requireNonNull( restriction, "missing restriction" );
		}

		@Override
		@Nonnull
		public Restriction<T> negated() {
			return adaptRestriction( restriction.negate() );
		}

		@Override
		@Nonnull
		public Predicate toPredicate(@Nonnull Root<? extends T> root, @Nonnull CriteriaBuilder builder) {
			return JakartaDataRestriction.restriction( restriction, root, builder );
		}
	}

	/**
	 * Adapt the given {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}.
	 */
	@Nonnull
	public static <T> Restriction<T> adaptRestriction(@Nonnull jakarta.data.restrict.Restriction<? super T> restriction) {
		return new Adapter<>( restriction );
	}

	/**
	 * Apply the given {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}
	 * to the given root entity of the given {@linkplain CriteriaQuery criteria query}.
	 */
	public static <T> void applyRestriction(
			@Nonnull jakarta.data.restrict.Restriction<? super T> restriction,
			@Nonnull CriteriaQuery<?> query,
			@Nonnull Root<? extends T> root,
			@Nonnull CriteriaBuilder builder) {
		requireNonNull( restriction, "missing restriction" );
		requireNonNull( query, "missing query" );
		requireNonNull( root, "missing root" );
		requireNonNull( builder, "missing builder" );
		final var predicate = restriction( restriction, root, builder );
		if ( query.getRestriction() == null ) {
			query.where( predicate );
		}
		else {
			query.where( query.getRestriction(), predicate );
		}
	}

	/**
	 * Apply the given {@linkplain jakarta.data.Order Jakarta Data order}
	 * to the given root entity of the given {@linkplain CriteriaQuery criteria query}.
	 */
	public static void applyOrder(
			@Nonnull jakarta.data.Order<?> order,
			@Nonnull CriteriaQuery<?> query,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		requireNonNull( order, "missing order" );
		for ( var sort : order.sorts() ) {
			applySort( sort, query, root, builder );
		}
	}

	/**
	 * Apply the given {@linkplain jakarta.data.Sort Jakarta Data sort}
	 * to the given root entity of the given {@linkplain CriteriaQuery criteria query}.
	 */
	public static void applySort(
			@Nonnull jakarta.data.Sort<?> sort,
			@Nonnull CriteriaQuery<?> query,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		requireNonNull( sort, "missing sort" );
		requireNonNull( query, "missing query" );
		requireNonNull( root, "missing root" );
		requireNonNull( builder, "missing builder" );
		final var sortExpression = sort.expression();
		final var criteriaExpression =
				sortExpression == null
						? path( root, requireNonNull( sort.property(), "missing sort property" ) )
						: expression( sortExpression, root, builder );
		final var expression =
				sort.ignoreCase()
						? builder.lower( criteriaExpression.as( String.class ) )
						: criteriaExpression;
		appendOrder( query, sort.isDescending() ? builder.desc( expression ) : builder.asc( expression ) );
	}

	@Nonnull
	private static jakarta.persistence.criteria.Path<?> path(@Nonnull Root<?> root, @Nonnull String path) {
		requireNonNull( path, "missing path" );
		final var tokens = new StringTokenizer( path, "." );
		if ( !tokens.hasMoreTokens() ) {
			throw new IllegalArgumentException( "Path may not be empty" );
		}
		jakarta.persistence.criteria.Path<?> criteriaPath = root;
		while ( tokens.hasMoreTokens() ) {
			criteriaPath = criteriaPath.get( tokens.nextToken() );
		}
		return criteriaPath;
	}

	private static void appendOrder(@Nonnull CriteriaQuery<?> query, @Nonnull jakarta.persistence.criteria.Order order) {
		final List<jakarta.persistence.criteria.Order> orders = new ArrayList<>( query.getOrderList() );
		orders.add( order );
		query.orderBy( orders );
	}

	/**
	 * Obtain a {@linkplain Predicate JPA criteria predicate} representing the given
	 * {@linkplain jakarta.data.restrict.Restriction Jakarta Data restriction}.
	 */
	@Nonnull
	private static Predicate restriction(
			@Nonnull jakarta.data.restrict.Restriction<?> restriction,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Predicate compositeRestriction(
			@Nonnull CompositeRestriction<?> compositeRestriction,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Predicate basicRestriction(
			@Nonnull BasicRestriction<?, ?> restriction,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		final var expression = expression( restriction.expression(), root, builder );
		return constraintPredicate( expression, restriction.constraint(), root, builder );
	}

	/**
	 * Apply the given {@linkplain jakarta.data.constraint.Constraint Jakarta Data constraint}
	 * to the given {@linkplain Expression JPA expression}, returning a {@linkplain Predicate
	 * JPA criteria predicate}.
	 */
	@Nonnull
	public static <T> Predicate applyConstraint(
			@Nonnull Expression<? extends T> expression,
			@Nonnull Constraint<T> constraint,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Predicate constraintPredicate(
			@Nonnull Expression<?> expression,
			@Nonnull Constraint<?> constraint,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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

	@Nonnull
	private static Predicate in(
			@Nonnull Expression<?> expression,
			@Nonnull List<? extends jakarta.data.expression.Expression<?, ?>> expressions,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Expression<?> expression(
			@Nonnull jakarta.data.expression.Expression<?, ?> expression,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		if ( expression instanceof Attribute<?> attribute ) {
			return path( root, attribute.name() );
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
	@Nonnull
	private static jakarta.persistence.criteria.Path<?> path(
			@Nonnull NavigableExpression<?, ?> expression, @Nonnull Root<?> root) {
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
	@Nonnull
	private static Expression<?> textFunction(
			@Nonnull TextFunctionExpression<?> function,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Expression<?> numericFunction(
			@Nonnull NumericFunctionExpression<?, ?> function,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Expression<?> numericCast(
			@Nonnull NumericCast<?, ?> cast,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
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
	@Nonnull
	private static Expression<?> numericOperation(
			@Nonnull NumericOperatorExpression<?, ?> operation,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		final var left = numberExpression( operation.left(), root, builder );
		final var right = numberExpression( operation.right(), root, builder );
		return switch ( operation.operator() ) {
			case PLUS -> builder.sum( left, right );
			case MINUS -> builder.diff( left, right );
			case TIMES -> builder.prod( left, right );
			case DIVIDE -> nodeBuilder( builder ).quotPortable( left, right );
		};
	}

	@Nonnull
	private static Predicate comparison(
			@Nonnull Expression<?> expression,
			@Nonnull ComparisonOperator operator,
			@Nonnull Expression<?> bound,
			@Nonnull CriteriaBuilder builder) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( bound, Comparable.class );
		return nodeBuilder( builder ).comparison( expression, operator, bound );
	}

	@Nonnull
	private static Predicate between(
			@Nonnull Expression<?> expression,
			@Nonnull Expression<?> lowerBound,
			@Nonnull Expression<?> upperBound,
			boolean negated,
			@Nonnull CriteriaBuilder builder) {
		verifyExpressionType( expression, Comparable.class );
		verifyExpressionType( lowerBound, Comparable.class );
		verifyExpressionType( upperBound, Comparable.class );
		return nodeBuilder( builder ).between( expression, lowerBound, upperBound, negated );
	}

	@Nonnull
	private static Expression<?> function(
			@Nonnull FunctionExpression<?, ?> function,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		final var arguments = function.arguments();
		final List<Expression<?>> list = new ArrayList<>( arguments.size() );
		for ( var argument : arguments ) {
			list.add( expression( argument, root, builder ) );
		}
		return builder.function( function.name(), function.type(),
				list.toArray( Expression<?>[]::new ) );
	}

	@Nonnull
	private static Expression<String> stringExpression(
			@Nonnull jakarta.data.expression.Expression<?, ?> expression,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		return stringExpression( expression( expression, root, builder ) );
	}

	@Nonnull
	private static Expression<String> stringExpression(@Nonnull Expression<?> expression) {
		verifyExpressionType( expression, String.class );
		return expression.as( String.class );
	}

	@Nonnull
	private static Expression<? extends Number> numberExpression(
			@Nonnull jakarta.data.expression.Expression<?, ?> expression,
			@Nonnull Root<?> root,
			@Nonnull CriteriaBuilder builder) {
		final var expressionType = wrapperType( expression.type() );
		if ( !Number.class.isAssignableFrom( expressionType ) ) {
			throw new IllegalArgumentException(
					"Expected 'Number' expression but got '" + expressionType.getName() + "'" );
		}
		return numberExpression( expression( expression, root, builder ),
				expressionType.asSubclass( Number.class ) );
	}

	@Nonnull
	private static <N extends Number> Expression<N> numberExpression(
			@Nonnull Expression<?> expression,
			@Nonnull Class<N> expectedType) {
		verifyExpressionType( expression, expectedType );
		return expression.as( expectedType );
	}

	@Nonnull
	private static Expression<?> literal(
			@Nullable Object value,
			@Nonnull Expression<?> expression,
			@Nonnull CriteriaBuilder builder) {
		if ( value != null ) {
			return builder.literal( value );
		}
		else {
			final var javaType = javaTypeIfKnown( expression );
			return javaType == null
					? builder.nullLiteral( Object.class )
					: builder.nullLiteral( javaType );
		}
	}

	@Nonnull
	private static NodeBuilder nodeBuilder(@Nonnull CriteriaBuilder builder) {
		if ( builder instanceof NodeBuilder nodeBuilder ) {
			return nodeBuilder;
		}
		else {
			throw new IllegalArgumentException( "Not Hibernate CriteriaBuilder" );
		}
	}

	private static void verifyExpressionType(
			@Nonnull Expression<?> expression,
			@Nonnull Class<?> expectedType) {
		final var expressionType = javaTypeIfKnown( expression );
		if ( expressionType != null ) {
			final var javaType = wrapperType( expressionType );
			if ( !expectedType.isAssignableFrom( javaType ) ) {
				throw new IllegalArgumentException(
						"Expected '" + expectedType.getName() + "' expression but got '" + javaType.getName() + "'" );
			}
		}
	}

	private static void verifyAssignableExpressionType(
			@Nonnull Expression<?> expression,
			@Nonnull Expression<?> valueExpression) {
		final var expressionType = javaTypeIfKnown( expression );
		final var valueType = javaTypeIfKnown( valueExpression );
		if ( expressionType != null && valueType != null && !expressionType.isAssignableFrom( valueType ) ) {
			throw new IllegalArgumentException(
					"Expected '" + expressionType.getName() + "' expression but got '" + valueType.getName() + "'" );
		}
	}

	private static void verifyValueType(@Nonnull Expression<?> expression, @Nullable Object value) {
		final var expressionType = javaTypeIfKnown( expression );
		if ( value != null && expressionType != null && !expressionType.isInstance( value ) ) {
			throw new IllegalArgumentException(
					"Expected '" + expressionType.getName() + "' value but got '" + value.getClass().getName() + "'" );
		}
	}

	@Nullable
	private static Class<?> javaTypeIfKnown(@Nonnull Expression<?> expression) {
		final var javaType = expression instanceof JpaTupleElement<?> tupleElement
				? tupleElement.getJavaTypeIfKnown()
				: expression.getJavaType();
		return javaType == null ? null : wrapperType( javaType );
	}

	@Nonnull
	private static Class<?> wrapperType(@Nonnull Class<?> javaType) {
		if ( !javaType.isPrimitive() ) {
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

	private static int integerLiteral(@Nonnull jakarta.data.expression.Expression<?, ?> expression) {
		if ( expression instanceof Literal<?> literal && literal.value() instanceof Integer integer ) {
			return integer;
		}
		else {
			throw new IllegalArgumentException(
					"Expected an integer literal but got '" + expression.getClass().getName() + "'" );
		}
	}
}
