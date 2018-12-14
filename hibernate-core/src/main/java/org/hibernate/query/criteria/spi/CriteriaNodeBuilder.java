/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.JpaTuple;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCriteriaUpdate;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetJoin;

import static javax.persistence.criteria.Predicate.BooleanOperator.AND;
import static javax.persistence.criteria.Predicate.BooleanOperator.OR;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.EQUAL;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.GREATER_THAN;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.LESS_THAN;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static org.hibernate.query.criteria.spi.ComparisonPredicate.ComparisonOperator.NOT_EQUAL;
import static org.hibernate.query.criteria.spi.RestrictedSubQueryExpression.Modifier.ALL;
import static org.hibernate.query.criteria.spi.RestrictedSubQueryExpression.Modifier.ANY;
import static org.hibernate.query.criteria.spi.RestrictedSubQueryExpression.Modifier.SOME;
import static org.hibernate.query.criteria.spi.UnaryArithmetic.Operation.UNARY_MINUS;

/**
 * @author Steve Ebersole
 */
public class CriteriaNodeBuilder implements HibernateCriteriaBuilder {
	private final SessionFactoryImplementor sessionFactory;

	public CriteriaNodeBuilder(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public void close() {
		// for future use (maybe)
	}

	@Override
	public <X, T> JpaExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType) {
		throw new NotYetImplementedFor6Exception();
	}

	public PredicateImplementor wrap(ExpressionImplementor<Boolean> expression) {
		if ( expression instanceof PredicateImplementor ) {
			return (PredicateImplementor) expression;
		}
		else if ( expression instanceof PathImplementor ) {
			return new BooleanAssertionPredicate( expression, Boolean.TRUE, this );
		}
		else {
			return new BooleanExpressionPredicate( expression, this );
		}
	}

	@Override
	public PredicateImplementor wrap(Expression<Boolean> expression) {
		return wrap( (ExpressionImplementor<Boolean>) expression );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor wrap(Expression<Boolean>... expressions) {
		if ( expressions == null || expressions.length == 0 ) {
			return null;
		}

		final Junction junction = new Junction( AND, this );
		for ( Expression<Boolean> expression : expressions ) {
			junction.addExpression( wrap( expression ) );
		}
		return junction;
	}

	@SuppressWarnings("unchecked")
	public PredicateImplementor wrap(ExpressionImplementor<Boolean>... expressions) {
		if ( expressions == null || expressions.length == 0 ) {
			return null;
		}

		final Junction junction = new Junction( AND, this );
		for ( Expression<Boolean> expression : expressions ) {
			junction.addExpression( wrap( expression ) );
		}
		return junction;
	}



	// Query builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public JpaCriteriaQuery<Object> createQuery() {
		return new CriteriaQueryImpl<>( Object.class, this );
	}

	@Override
	public <T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<>( resultClass, this );
	}

	@Override
	public JpaCriteriaQuery<Tuple> createTupleQuery() {
		return new CriteriaQueryImpl<>( Tuple.class, this );
	}

	@Override
	public <T> JpaCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <T> JpaCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		throw new NotYetImplementedFor6Exception();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	@Override
	public <X, T extends X> PathImplementor<T> treat(Path<X> path, Class<T> type) {
		return ( (PathImplementor<T>) path ).treatAs( type );
	}

	@Override
	public <X, T extends X> RootImplementor<T> treat(Root<X> root, Class<T> type) {
		return ( (RootImplementor<X>) root ).treatAs( type );
	}

	@Override
	public <X, T, V extends T> JoinImplementor<X, V> treat(Join<X, T> join, Class<V> type) {
		return ( (JoinImplementor<X,T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> CollectionJoinImplementor<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return ( (CollectionJoinImplementor<X,T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> JpaSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return ( (SetJoinImplementor<X,T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> JpaListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return ( (ListJoinImplementor<X,T>) join ).treatAs( type );
	}

	@Override
	public <X, K, V, S extends V> JpaMapJoin<X, K, S> treat(MapJoin<X, K, V> join, Class<S> type) {
		return ( (MapJoinImplementor<X,K,V>) join ).treatAs( type );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	@Override
	@SuppressWarnings("unchecked")
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>[] selections) {
		return construct( resultClass, (List) Arrays.asList( selections ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> CompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> selections) {
		return new ConstructorSelection<>( resultClass, (List) selections, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompoundSelection<Tuple> tuple(Selection<?>[] selections) {
		return tuple( (List) Arrays.asList( selections ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompoundSelection<Tuple> tuple(List<? extends JpaSelection<?>> selections) {
		return new MultiSelectSelection( selections, JpaTuple.class, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompoundSelection<Object[]> array(Selection<?>[] selections) {
		return array( (List) Arrays.asList( selections ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompoundSelection<Object[]> array(List<? extends JpaSelection<?>> selections) {
		return new MultiSelectSelection<>( (List) selections, Object[].class, this );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions


	@Override
	public <T> ExpressionImplementor<T> literal(T value) {
		return new LiteralExpression<>( value, this );
	}

	@Override
	public <T> List<ExpressionImplementor<T>> literals(T[] values) {
		final List<ExpressionImplementor<T>> literals = CollectionHelper.arrayList( values.length );

		for ( T value : values ) {
			literals.add( literal( value ) );
		}

		return literals;
	}

	@Override
	public <T> List<ExpressionImplementor<T>> literals(List<T> values) {
		final List<ExpressionImplementor<T>> literals = CollectionHelper.arrayList( values.size() );

		for ( T value : values ) {
			literals.add( literal( value ) );
		}

		return literals;
	}

	@Override
	public <T> JpaExpression<T> nullLiteral(Class<T> resultClass) {
		return new NullLiteralExpression<>( resultClass, this );
	}

	@Override
	public <N extends Number> AbsFunction<N> abs(Expression<N> argument) {
		return new AbsFunction<>( (ExpressionImplementor<N>) argument, this );
	}

	@Override
	public <N extends Number> AggregationFunction<Double> avg(Expression<N> argument) {
		return new AggregationFunction.AVG( (ExpressionImplementor<? extends Number>) argument, this );
	}

	@Override
	public AggregationFunction<Long> count(Expression<?> argument) {
		return new AggregationFunction.COUNT( (ExpressionImplementor<?>) argument, false, this );
	}

	@Override
	public AggregationFunction<Long> countDistinct(Expression<?> argument) {
		return new AggregationFunction.COUNT( (ExpressionImplementor<?>) argument, true, this );
	}

	@Override
	public <N extends Number> AggregationFunction<N> sum(Expression<N> argument) {
		return new AggregationFunction.SUM<>( (ExpressionImplementor<N>) argument, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AggregationFunction<Long> sumAsLong(Expression<Integer> argument) {
		return new AggregationFunction.SUM<>( ( (ExpressionImplementor) argument ).asLong(), this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AggregationFunction<Double> sumAsDouble(Expression<Float> argument) {
		return new AggregationFunction.SUM<>( ( (ExpressionImplementor) argument ).asFloat(), this );
	}

	@Override
	public <N extends Number> AggregationFunction<N> max(Expression<N> argument) {
		return new AggregationFunction.MAX<>( (ExpressionImplementor<N>) argument, this );
	}

	@Override
	public <N extends Number> AggregationFunction<N> min(Expression<N> argument) {
		return new AggregationFunction.MIN<>( (ExpressionImplementor<N>) argument, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N extends Number> BinaryArithmetic<N> sum(
			Expression<? extends N> lhs,
			Expression<? extends N> rhs) {
		if ( lhs == null || rhs == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = BinaryArithmetic.determineResultType( lhs.getJavaType(), rhs.getJavaType() );

		return new BinaryArithmetic<N>(
				(ExpressionImplementor) lhs,
				BinaryArithmetic.Operation.ADD,
				(ExpressionImplementor) rhs,
				resultType,
				this
		);
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> sum(
			N lhs,
			Expression<? extends N> rhs) {
		return sum( literal( lhs ), rhs );
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> sum(Expression<? extends N> lhs, N rhs) {
		return sum( lhs, literal( rhs ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N extends Number> BinaryArithmetic<N> diff(
			Expression<? extends N> lhs,
			Expression<? extends N> rhs) {
		if ( lhs == null || rhs == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = BinaryArithmetic.determineResultType( lhs.getJavaType(), rhs.getJavaType() );

		return new BinaryArithmetic<N>(
				(ExpressionImplementor) lhs,
				BinaryArithmetic.Operation.SUBTRACT,
				(ExpressionImplementor) rhs,
				resultType,
				this
		);
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> diff(
			Expression<? extends N> lhs,
			N rhs) {
		return diff( lhs, literal( rhs ) );
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> diff(
			N lhs,
			Expression<? extends N> rhs) {
		return diff( literal( lhs ), rhs );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N extends Number> BinaryArithmetic<N> prod(
			Expression<? extends N> lhs,
			Expression<? extends N> rhs) {
		if ( lhs == null || rhs == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = BinaryArithmetic.determineResultType( lhs.getJavaType(), rhs.getJavaType() );

		return new BinaryArithmetic<N>(
				(ExpressionImplementor) lhs,
				BinaryArithmetic.Operation.MULTIPLY,
				(ExpressionImplementor) rhs,
				resultType,
				this
		);
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> prod(
			Expression<? extends N> lhs,
			N rhs) {
		return prod( lhs, literal( rhs ) );
	}

	@Override
	public <N extends Number> BinaryArithmetic<N> prod(N lhs, Expression<? extends N> rhs) {
		return prod( literal( lhs ), rhs );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BinaryArithmetic<Number> quot(
			Expression<? extends Number> lhs,
			Expression<? extends Number> rhs) {
		if ( lhs == null || rhs == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmetic.determineResultType( lhs.getJavaType(), rhs.getJavaType() );

		return new BinaryArithmetic<Number>(
				(ExpressionImplementor) lhs,
				BinaryArithmetic.Operation.DIVIDE,
				(ExpressionImplementor) rhs,
				resultType,
				this
		);
	}

	@Override
	public BinaryArithmetic<Number> quot(
			Expression<? extends Number> lhs,
			Number rhs) {
		return quot( lhs, literal( rhs ) );
	}

	@Override
	public BinaryArithmetic<Number> quot(
			Number lhs,
			Expression<? extends Number> rhs) {
		return quot( literal( lhs), rhs );
	}

	@Override
	@SuppressWarnings("unchecked")
	public BinaryArithmetic<Integer> mod(Expression<Integer> lhs, Expression<Integer> rhs) {
		if ( lhs == null || rhs == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = BinaryArithmetic.determineResultType( lhs.getJavaType(), rhs.getJavaType() );

		return new BinaryArithmetic<Integer>(
				(ExpressionImplementor) lhs,
				BinaryArithmetic.Operation.DIVIDE,
				(ExpressionImplementor) rhs,
				resultType,
				this
		);
	}

	@Override
	public JpaExpression<Integer> mod(Expression<Integer> lhs, Integer rhs) {
		return mod( lhs, literal( rhs ) );
	}

	@Override
	public JpaExpression<Integer> mod(Integer lhs, Expression<Integer> rhs) {
		return mod( literal( lhs ), rhs );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqrtFunction sqrt(Expression<? extends Number> argument) {
		return new SqrtFunction( (ExpressionImplementor) argument, this );
	}

	@Override
	public <N extends Number> UnaryArithmetic<N> neg(Expression<N> operand) {
		return new UnaryArithmetic<>( UNARY_MINUS, ( ExpressionImplementor<N>) operand, this );
	}

	@Override
	public <T> CoalesceExpression<T> coalesce() {
		return new CoalesceExpression<>( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> CoalesceExpression<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		return coalesce( (ExpressionImplementor) x, (ExpressionImplementor) y );
	}

	@SuppressWarnings("unchecked")
	public <Y> CoalesceExpression<Y> coalesce(ExpressionImplementor<? extends Y> x, ExpressionImplementor<? extends Y> y) {
		return new CoalesceExpression( Arrays.asList( x, y ), this );
	}

	@Override
	public <Y> CoalesceExpression<Y> coalesce(Expression<? extends Y> x, Y y) {
		return coalesce( x, literal( y ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ConcatExpression concat(Expression<String> first, Expression<String> second) {
		return concat( (ExpressionImplementor) first, (ExpressionImplementor) second );
	}

	public ConcatExpression concat(ExpressionImplementor<String> first, ExpressionImplementor<String> second) {
		return new ConcatExpression( first, second, this );
	}

	@Override
	public ConcatExpression concat(Expression<String> first, String second) {
		return concat( first, literal( second ) );
	}

	@Override
	public ConcatExpression concat(String first, Expression<String> second) {
		return concat( literal( first ), second );
	}

	@Override
	public ConcatExpression concat(String first, String second) {
		return concat( literal( first ), literal( second ) );
	}

	@Override
	public <Y> RestrictedSubQueryExpression<Y> all(Subquery<Y> subquery) {
		return new RestrictedSubQueryExpression<>( (SubQuery<Y>) subquery, ALL, this );
	}

	@Override
	public <Y> RestrictedSubQueryExpression<Y> some(Subquery<Y> subquery) {
		return new RestrictedSubQueryExpression<>( (SubQuery<Y>) subquery, SOME, this );
	}

	@Override
	public <Y> RestrictedSubQueryExpression<Y> any(Subquery<Y> subquery) {
		return new RestrictedSubQueryExpression<>( (SubQuery<Y>) subquery, ANY, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C, R> org.hibernate.query.criteria.spi.SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return selectCase( (ExpressionImplementor) expression );
	}

	@SuppressWarnings("unchecked")
	public <C, R> org.hibernate.query.criteria.spi.SimpleCase<C, R> selectCase(ExpressionImplementor<? extends C> expression) {
		return new org.hibernate.query.criteria.spi.SimpleCase( expression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> SearchedCase<R> selectCase() {
		return new SearchedCase( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> NullifExpression<Y> nullif(Expression<Y> primary, Expression<?> secondary) {
		return new NullifExpression( (ExpressionImplementor) primary, (ExpressionImplementor) secondary, this );
	}

	@Override
	public <Y> NullifExpression<Y> nullif(Expression<Y> primary, Y secondary) {
		return nullif( primary, literal( secondary ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<Long> toLong(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asLong();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<Integer> toInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<Float> toFloat(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asFloat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<Double> toDouble(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asDouble();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asBigDecimal();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<BigInteger> toBigInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor) expression ).asBigInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaExpression<String> toString(Expression<Character> expression) {
		return ( (ExpressionImplementor) expression ).asString();
	}

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass) {
		return new ParameterExpression<>( paramClass, this );
	}

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return new ParameterExpression<>( name, paramClass, this );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	@Override
	@SuppressWarnings("unchecked")
	public <T> GenericFunction<T> function(String name, Class<T> type, Expression<?>[] args) {
		return function( name, type, (List) Arrays.asList( args ) );
	}

	@SuppressWarnings("unchecked")
	public <T> GenericFunction<T> function(String name, Class<T> type, List<ExpressionImplementor<?>> args) {
		return new GenericFunction( name, args, type, this );
	}

	@Override
	public CurrentDateFunction currentDate() {
		return new CurrentDateFunction( this );
	}

	@Override
	public CurrentTimestampFunction currentTimestamp() {
		return new CurrentTimestampFunction( this );
	}

	@Override
	public CurrentTimeFunction currentTime() {
		return new CurrentTimeFunction( this );
	}

	@Override
	public LowerFunction lower(Expression<String> stringExpression) {
		return lower( (ExpressionImplementor<String>) stringExpression );
	}

	public LowerFunction lower(ExpressionImplementor<String> stringExpression) {
		return new LowerFunction( stringExpression, this );
	}

	@Override
	public UpperFunction upper(Expression<String> stringExpression) {
		return upper( (ExpressionImplementor<String>) stringExpression );
	}

	public UpperFunction upper(ExpressionImplementor<String> stringExpression) {
		return new UpperFunction( stringExpression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public LengthFunction length(Expression<String> stringExpression) {
		return length( (ExpressionImplementor) stringExpression );
	}

	public LengthFunction length(ExpressionImplementor<String> stringExpression) {
		return new LengthFunction( stringExpression, this );
	}

	@Override
	public LocateFunction locate(Expression<String> expression, String pattern) {
		return locate( expression, literal( pattern ) );
	}

	@Override
	public LocateFunction locate(Expression<String> expression, Expression<String> pattern) {
		return locate( expression, pattern, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public LocateFunction locate(
			Expression<String> expression,
			Expression<String> pattern,
			Expression<Integer> from) {
		return locate(
				(ExpressionImplementor) expression,
				(ExpressionImplementor) pattern,
				(ExpressionImplementor) from
		);
	}

	public LocateFunction locate(
			ExpressionImplementor<String> expression,
			ExpressionImplementor<String> pattern,
			ExpressionImplementor<Integer> from) {
		return new LocateFunction( expression, pattern, from, this );
	}

	@Override
	public LocateFunction locate(Expression<String> expression, String pattern, int from) {
		return locate( expression, literal( pattern ), literal( from ) );
	}

	@Override
	public SubstringFunction substring(Expression<String> value, int start) {
		return substring( value, literal( start ) );
	}

	@Override
	public SubstringFunction substring(Expression<String> value, Expression<Integer> start) {
		return substring( value, start, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SubstringFunction substring(
			Expression<String> value,
			Expression<Integer> start,
			Expression<Integer> length) {
		return substring(
				(ExpressionImplementor) value,
				(ExpressionImplementor) start,
				(ExpressionImplementor) length
		);
	}

	public SubstringFunction substring(
			ExpressionImplementor<String> value,
			ExpressionImplementor<Integer> start,
			ExpressionImplementor<Integer> length) {
		return new SubstringFunction( value, start, length, this );
	}

	@Override
	public SubstringFunction substring(Expression<String> value, int start, int length) {
		return substring( value, literal( start ), literal( length ) );
	}

	@Override
	public TrimFunction trim(Expression<String> value) {
		return trim( Trimspec.BOTH, value );
	}

	@Override
	public TrimFunction trim(Trimspec trimspec, Expression<String> value) {
		return trim( trimspec, literal( ' ' ), value );
	}

	@Override
	public TrimFunction trim(Expression<Character> trimCharacter, Expression<String> value) {
		return trim( Trimspec.BOTH, trimCharacter, value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public TrimFunction trim(
			Trimspec trimspec,
			Expression<Character> trimCharacter,
			Expression<String> value) {
		return trim(
				trimspec,
				(ExpressionImplementor) trimCharacter,
				(ExpressionImplementor) value
		);
	}

	public TrimFunction trim(
			Trimspec trimspec,
			ExpressionImplementor<Character> trimCharacter,
			ExpressionImplementor<String> value) {
		return new TrimFunction( trimspec, trimCharacter, value, this );
	}

	@Override
	public TrimFunction trim(char trimCharacter, Expression<String> value) {
		return trim( literal( trimCharacter ), value );
	}

	@Override
	public TrimFunction trim(Trimspec trimspec, char trimCharacter, Expression<String> value) {
		return trim( trimspec, literal( trimCharacter ), value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	@Override
	public PredicateImplementor not(Expression<Boolean> restriction) {
		return wrap( restriction ).not();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor and(Expression<Boolean> x, Expression<Boolean> y) {
		return new Junction(
				AND,
				(List) Arrays.asList( x, y ),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor and(Predicate... restrictions) {
		return new Junction(
				AND,
				(List) Arrays.asList( restrictions ),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor or(Expression<Boolean> x, Expression<Boolean> y) {
		return new Junction(
				OR,
				(List) Arrays.asList( x, y ),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor or(Predicate... restrictions) {
		return new Junction(
				OR,
				(List) Arrays.asList( restrictions ),
				this
		);
	}

	@Override
	public PredicateImplementor conjunction() {
		return new Junction( AND, this );
	}

	@Override
	public PredicateImplementor disjunction() {
		return new Junction( OR, this );
	}

	@Override
	public PredicateImplementor isTrue(Expression<Boolean> x) {
		return new BooleanAssertionPredicate( (ExpressionImplementor<Boolean>) x, true, this );
	}

	@Override
	public PredicateImplementor isFalse(Expression<Boolean> x) {
		return new BooleanAssertionPredicate( (ExpressionImplementor<Boolean>) x, false, this );
	}

	@Override
	public PredicateImplementor isNull(Expression<?> x) {
		return new NullnessPredicate( (ExpressionImplementor<?>) x, this );
	}

	@Override
	public PredicateImplementor isNotNull(Expression<?> x) {
		return isNull( x ).not();
	}

	@Override
	public PredicateImplementor equal(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, EQUAL, ( ExpressionImplementor)y, this );
	}

	@Override
	public PredicateImplementor equal(Expression<?> x, Object y) {
		return equal( x, literal( y ) );
	}

	@Override
	public PredicateImplementor notEqual(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, NOT_EQUAL, ( ExpressionImplementor)y, this );
	}

	@Override
	public PredicateImplementor notEqual(Expression<?> x, Object y) {
		return notEqual( x, literal( y ) );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, GREATER_THAN, (ExpressionImplementor) y, this );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor greaterThan(Expression<? extends Y> x, Y y) {
		return greaterThan( x, literal( y ) );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, GREATER_THAN_OR_EQUAL, (ExpressionImplementor) y, this );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return greaterThanOrEqualTo( x, literal( y ) );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, LESS_THAN, (ExpressionImplementor) y, this );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor lessThan(Expression<? extends Y> x, Y y) {
		return lessThan( x, literal( y ) );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, LESS_THAN_OR_EQUAL, (ExpressionImplementor) y, this );
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return lessThanOrEqualTo( x, literal( y ) );
	}

	@Override
	public PredicateImplementor gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, GREATER_THAN, (ExpressionImplementor) y, this );
	}

	@Override
	public PredicateImplementor gt(Expression<? extends Number> x, Number y) {
		return gt( x, literal( y ) );
	}

	@Override
	public PredicateImplementor ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, GREATER_THAN_OR_EQUAL, (ExpressionImplementor) y, this );
	}

	@Override
	public PredicateImplementor ge(Expression<? extends Number> x, Number y) {
		return ge( x, literal( y ) );
	}

	@Override
	public PredicateImplementor lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, LESS_THAN, (ExpressionImplementor) y, this );
	}

	@Override
	public PredicateImplementor lt(Expression<? extends Number> x, Number y) {
		return lt( x, literal( y ) );
	}

	@Override
	public PredicateImplementor le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( (ExpressionImplementor) x, LESS_THAN_OR_EQUAL, (ExpressionImplementor) y, this );
	}

	@Override
	public PredicateImplementor le(Expression<? extends Number> x, Number y) {
		return le( x, literal( y ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y extends Comparable<? super Y>> PredicateImplementor between(Expression<? extends Y> value, Expression<? extends Y> lower, Expression<? extends Y> upper) {
		return new BetweenPredicate<>(
				(ExpressionImplementor) value,
				(ExpressionImplementor) lower,
				(ExpressionImplementor) upper,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> PredicateImplementor between(Expression<? extends Y> value, Y lower, Y upper) {
		return between( value, literal( upper ), literal( lower ) );
	}

	@Override
	public PredicateImplementor like(Expression<String> expression, String pattern) {
		return like( expression, literal( pattern ) );
	}

	@Override
	public PredicateImplementor like(Expression<String> expression, Expression<String> pattern) {
		return like( expression, pattern, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor like(Expression<String> expression, Expression<String> pattern, Expression<Character> escapeChar) {
		return new LikePredicate(
				(ExpressionImplementor) expression,
				(ExpressionImplementor) pattern,
				(ExpressionImplementor) escapeChar,
				this
		);
	}

	@Override
	public PredicateImplementor like(Expression<String> expression, Expression<String> pattern, char escapeChar) {
		return like( expression, pattern, literal( escapeChar ) );
	}

	@Override
	public PredicateImplementor like(Expression<String> expression, String pattern, Expression<Character> escapeChar) {
		return like( expression, literal( pattern ), escapeChar );
	}

	@Override
	public PredicateImplementor like(Expression<String> expression, String pattern, char escapeChar) {
		return like( expression, literal( pattern ), literal( escapeChar ) );
	}

	@Override
	public PredicateImplementor notLike(Expression<String> expression, String pattern) {
		return notLike( expression, literal( pattern ) );
	}

	@Override
	public PredicateImplementor notLike(Expression<String> expression, Expression<String> pattern) {
		return notLike( expression, pattern, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PredicateImplementor notLike(Expression<String> expression, Expression<String> pattern, Expression<Character> escapeChar) {
		return new LikePredicate(
				(ExpressionImplementor) expression,
				(ExpressionImplementor) pattern,
				(ExpressionImplementor) escapeChar,
				true,
				this
		);
	}

	@Override
	public PredicateImplementor notLike(Expression<String> expression, Expression<String> pattern, char escapeChar) {
		return notLike( expression, pattern, literal( escapeChar ) );
	}

	@Override
	public PredicateImplementor notLike(Expression<String> expression, String pattern, Expression<Character> escapeChar) {
		return notLike( expression, literal( pattern ), escapeChar );
	}

	@Override
	public PredicateImplementor notLike(Expression<String> expression, String pattern, char escapeChar) {
		return notLike( expression, literal( pattern ), literal( escapeChar ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression) {
		return new InPredicate<>( (ExpressionImplementor) expression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		return new InPredicate(
				(ExpressionImplementor) expression,
				Arrays.asList( values ),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression, T... values) {
		return new InPredicate(
				(ExpressionImplementor) expression,
				literals( values ),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression, List<T> values) {
		return new InPredicate(
				(ExpressionImplementor) expression,
				literals( values ),
				this
		);
	}

	@Override
	public ExistsPredicate exists(Subquery<?> subquery) {
		return new ExistsPredicate( (SubQuery<?>) subquery, this );
	}

	@Override
	public <C extends Collection<?>> EmptinessPredicate isEmpty(Expression<C> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new EmptinessPredicate( (PluralPath<C>) pluralPathExpression, this );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown collection expression type [%s] for is_empty check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <C extends Collection<?>> EmptinessPredicate isNotEmpty(Expression<C> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new EmptinessPredicate( (PluralPath<C>) pluralPathExpression, true, this );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown collection expression type [%s] for is_not_empty check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <M extends Map<?, ?>> EmptinessPredicate isMapEmpty(JpaExpression<M> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new EmptinessPredicate( (PluralPath<M>) pluralPathExpression, this );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown map expression type [%s] for is_empty check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <M extends Map<?, ?>> JpaPredicate isMapNotEmpty(JpaExpression<M> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new EmptinessPredicate( (PluralPath<M>) pluralPathExpression, true, this );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown map expression type [%s] for is_not_empty check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(Expression<E> elem, Expression<C> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new MembershipPredicate<>(
					(ExpressionImplementor<E>) elem,
					(PluralPath<E>) pluralPathExpression,
					this
			);
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown map expression type [%s] for is_member check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(E elem, Expression<C> pluralPathExpression) {
		return isMember( literal( elem ), pluralPathExpression );
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(Expression<E> elem, Expression<C> pluralPathExpression) {
		if ( pluralPathExpression instanceof PluralPath ) {
			return new MembershipPredicate<>(
					(ExpressionImplementor<E>) elem,
					(PluralPath<E>) pluralPathExpression,
					true,
					this
			);
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unknown map expression type [%s] for is_member check",
						pluralPathExpression.getClass().getName()
				)
		);
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(E elem, Expression<C> pluralPathExpression) {
		return isNotMember( literal( elem ), pluralPathExpression );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering

	@Override
	public SortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		return new SortSpecification(
				(ExpressionImplementor) sortExpression,
				sortOrder,
				nullPrecedence,
				this
		);
	}

	@Override
	public SortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder) {
		return sort( sortExpression, sortOrder, NullPrecedence.NONE );
	}

	@Override
	public SortSpecification sort(JpaExpression<?> sortExpression) {
		return sort( sortExpression, SortOrder.ASCENDING );
	}

	@Override
	public SortSpecification asc(Expression<?> sortExpression) {
		return sort( (ExpressionImplementor) sortExpression, SortOrder.ASCENDING );
	}

	@Override
	public SortSpecification desc(Expression<?> sortExpression) {
		return sort( (ExpressionImplementor) sortExpression, SortOrder.DESCENDING );
	}











	@Override
	public <X extends Comparable<? super X>> ExpressionImplementor<X> greatest(Expression<X> argument) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X extends Comparable<? super X>> JpaExpression<X> least(Expression<X> argument) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <K, M extends Map<K, ?>> JpaExpression<Set<K>> keys(M map) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <K, L extends List<?>> JpaExpression<Set<K>> indexes(L list) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <V, C extends Collection<V>> JpaExpression<Collection<V>> values(C collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(C collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(JpaExpression<M> mapExpression) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(M map) {
		throw new NotYetImplementedFor6Exception();
	}
}
