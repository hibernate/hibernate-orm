/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.hibernate.QueryException;
import org.hibernate.SortOrder;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.spi.TrimSpecificationExpressionWrapper;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentInstantFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.TrimSpecification;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;

/**
 * Acts as a JPA {@link javax.persistence.criteria.CriteriaBuilder} by
 * using SQM nodes as the JPA Criteria nodes
 * 
 * @author Steve Ebersole
 */
public class SqmCriteriaNodeBuilder implements NodeBuilder {
	private final QueryEngine queryEngine;
	private final MetamodelImplementor domainModel;
	private final ServiceRegistry serviceRegistry;

	public SqmCriteriaNodeBuilder(
			QueryEngine queryEngine,
			MetamodelImplementor domainModel,
			ServiceRegistry serviceRegistry) {
		this.queryEngine = queryEngine;
		this.domainModel = domainModel;
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public MetamodelImplementor getDomainModel() {
		return domainModel;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	public void close() {
		// for potential future use
	}

	@Override
	public SqmSelectStatement<Object> createQuery() {
		return new SqmSelectStatement<>( Object.class, this );
	}

	@Override
	public <T> SqmSelectStatement<T> createQuery(Class<T> resultClass) {
		return new SqmSelectStatement<>( resultClass, this );
	}

	@Override
	public SqmSelectStatement<Tuple> createTupleQuery() {
		return new SqmSelectStatement<>( Tuple.class, this );
	}

	@Override
	public <T> SqmUpdateStatement<T> createCriteriaUpdate(Class<T> targetEntity) {
		return new SqmUpdateStatement<>( targetEntity, this );
	}

	@Override
	public <T> SqmDeleteStatement<T> createCriteriaDelete(Class<T> targetEntity) {
		return new SqmDeleteStatement<>( targetEntity, this );
	}

	@Override
	public <T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity) {
		return new SqmInsertSelectStatement<>( targetEntity, this );
	}

	@Override
	public 	<X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType) {
		//noinspection unchecked
		return new SqmCastFunction<>(
				(SqmExpression) expression,
				getTypeConfiguration().standardExpressableTypeForJavaType( castTargetJavaType )
		);
	}

	@Override
	public SqmPredicate wrap(Expression<Boolean> expression) {
		if ( expression instanceof SqmPredicate ) {
			return (SqmPredicate) expression;
		}

		return new SqmBooleanExpressionPredicate( (SqmExpression<Boolean>) expression, this );
	}

	// todo (6.0) : wrapping a non-`SqmPredicate` `Expression<Boolean>` expression should be as easy as an impl
	@Override
	public SqmPredicate wrap(Expression... expressions) {
		SqmPredicate lhs = (SqmPredicate) expressions[0];
		SqmPredicate rhs = (SqmPredicate) expressions[0];
		SqmPredicate predicate = new SqmAndPredicate( lhs, rhs, this );
		if ( expressions.length > 2 ) {
			for ( Expression expression : expressions ) {
				final SqmPredicate newRhs;
				if ( expression instanceof SqmPredicate ) {
					newRhs = (SqmPredicate) expression;
				}
				else {
					//noinspection unchecked
					return new SqmBooleanExpressionPredicate( (SqmExpression<Boolean>) expression, this );
				}

				predicate = new SqmAndPredicate( predicate, newRhs, this );
			}
		}
		return predicate;
	}

	@Override
	public <X, T extends X> SqmPath<T> treat(Path<X> path, Class<T> type) {
		//noinspection unchecked
		return ( (SqmPath) path ).treatAs( type );
	}

	@Override
	public <X, T extends X> SqmRoot<T> treat(Root<X> root, Class<T> type) {
		//noinspection unchecked
		return ( (SqmRoot) root ).treatAs( type );
	}

	@Override
	public <X, T, V extends T> SqmSingularJoin<X, V> treat(Join<X, T> join, Class<V> type) {
		//noinspection unchecked
		return ( (SqmSingularJoin) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmBagJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		//noinspection unchecked
		return ( (SqmBagJoin) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		//noinspection unchecked
		return ( (SqmSetJoin) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		//noinspection unchecked
		return ( (SqmListJoin) join ).treatAs( type );
	}

	@Override
	public <X, K, T, V extends T> SqmMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		//noinspection unchecked
		return ( (SqmMapJoin) join ).treatAs( type );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>[] arguments) {
		final SqmDynamicInstantiation instantiation;
		if ( List.class.equals( resultClass ) ) {
			instantiation = SqmDynamicInstantiation.forListInstantiation( this );
		}
		else if ( Map.class.equals( resultClass ) ) {
			instantiation = SqmDynamicInstantiation.forMapInstantiation( this );
		}
		else {
			instantiation = SqmDynamicInstantiation.forClassInstantiation( resultClass, this );
		}

		for ( Selection<?> argument : arguments ) {
			//noinspection unchecked
			instantiation.addArgument(
					new SqmDynamicInstantiationArgument(
							(SqmSelectableNode) argument,
							argument.getAlias(),
							this
					)
			);
		}

		//noinspection unchecked
		return instantiation;
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> arguments) {
		final SqmDynamicInstantiation instantiation;
		if ( List.class.equals( resultClass ) ) {
			instantiation = SqmDynamicInstantiation.forListInstantiation( this );
		}
		else if ( Map.class.equals( resultClass ) ) {
			instantiation = SqmDynamicInstantiation.forMapInstantiation( this );
		}
		else {
			instantiation = SqmDynamicInstantiation.forClassInstantiation( resultClass, this );
		}

		for ( Selection<?> argument : arguments ) {
			//noinspection unchecked
			instantiation.addArgument(
					new SqmDynamicInstantiationArgument(
							(SqmSelectableNode) argument,
							argument.getAlias(),
							this
					)
			);
		}

		//noinspection unchecked
		return instantiation;
	}



	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression) sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder) {
		return new SqmSortSpecification( (SqmExpression) sortExpression, sortOrder );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression) {
		return new SqmSortSpecification( (SqmExpression) sortExpression );
	}

	@Override
	public SqmSortSpecification asc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression) x, SortOrder.ASCENDING );
	}

	@Override
	public SqmSortSpecification desc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression) x, SortOrder.DESCENDING );
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(Selection<?>[] selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection(
				ArrayHelper.toList( selections ),
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Tuple.class ),
				this
		);
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(List<? extends JpaSelection<?>> selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection(
				selections,
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Tuple.class ),
				this
		);
	}

	@Override
	public JpaCompoundSelection<Object[]> array(Selection<?>[] selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection(
				ArrayHelper.toList( selections ),
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Object[].class ),
				this
		);
	}

	@Override
	public JpaCompoundSelection<Object[]> array(List<? extends JpaSelection<?>> selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection(
				selections,
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Object[].class ),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument) {
		final SqmExpression sqmExpression = (SqmExpression) argument;

		//noinspection unchecked
		return new SqmAvgFunction(
				sqmExpression,
				(AllowableFunctionReturnType) QueryHelper.highestPrecedenceType2( sqmExpression.getExpressableType(), StandardSpiBasicTypes.DOUBLE ),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<N> argument) {
		final SqmExpression sqmExpression = (SqmExpression) argument;

		//noinspection unchecked
		return new SqmSumFunction(
				sqmExpression,
				(AllowableFunctionReturnType) sqmExpression.getExpressableType(),
				this
		);
	}

	@Override
	public SqmExpression<Long> sumAsLong(Expression<Integer> argument) {
		return cast( sum( argument ), Long.class );
	}

	@Override
	public SqmExpression<Double> sumAsDouble(Expression<Float> argument) {
		return cast( sum( argument ), Double.class );
	}

	@Override
	public <N extends Number> SqmExpression<N> max(Expression<N> argument) {
		final SqmExpression<N> sqmExpression = (SqmExpression<N>) argument;
		//noinspection unchecked
		return new SqmMaxFunction(
				sqmExpression,
				(BasicValuedExpressableType) sqmExpression.getExpressableType(),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> min(Expression<N> argument) {
		final SqmExpression<N> sqmExpression = (SqmExpression<N>) argument;
		//noinspection unchecked
		return new SqmMinFunction(
				sqmExpression,
				(BasicValuedExpressableType) sqmExpression.getExpressableType(),
				this
		);
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> greatest(Expression<X> argument) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> least(Expression<X> argument) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmCountFunction<Long> count(Expression<?> argument) {
		return new SqmCountFunction<>(
				(SqmExpression) argument,
				StandardSpiBasicTypes.LONG,
				this
		);
	}

	@Override
	public SqmCountFunction<Long> countDistinct(Expression<?> argument) {
		final SqmCountFunction<Long> count = count( argument );
		count.makeDistinct();
		return count;
	}

	@Override
	public <N extends Number> SqmExpression<N> neg(Expression<N> x) {
		final SqmExpression<N> sqmExpression = (SqmExpression<N>) x;
		return new SqmUnaryOperation<>(
				UnaryArithmeticOperator.UNARY_MINUS,
				sqmExpression
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> abs(Expression<N> x) {
		final SqmExpression<N> sqmExpression = (SqmExpression<N>) x;
		//noinspection unchecked
		return new SqmAbsFunction(
				sqmExpression,
				(AllowableFunctionReturnType) sqmExpression.getExpressableType(),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD, (SqmExpression) x, (SqmExpression) y );
	}

	private <N extends Number> SqmExpression<N> createSqmArithmeticNode(
			BinaryArithmeticOperator operator,
			SqmExpression leftHandExpression,
			SqmExpression rightHandExpression) {
		//noinspection unchecked
		return new SqmBinaryArithmetic(
				operator,
				leftHandExpression,
				rightHandExpression,
				getDomainModel().getTypeConfiguration().resolveArithmeticType(
						(BasicValuedExpressableType) leftHandExpression.getExpressableType(),
						(BasicValuedExpressableType) rightHandExpression.getExpressableType(),
						operator
				),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD, (SqmExpression) x, literal( y ) );
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD, literal( x ), (SqmExpression) y );
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD, literal( x ), (SqmExpression) y );
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(Expression<? extends N> x, N y) {
		return sum( x, y );
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(N x, Expression<? extends N> y) {
		return sum( x, y );
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression) x,
				(SqmExpression) y
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression) x,
				literal( y )
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				literal( x ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression) x,
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Number> quot(Expression<? extends Number> x, Number y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression) x,
				literal( y )
		);
	}

	@Override
	public SqmExpression<Number> quot(Number x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				literal( x ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression) x,
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Integer> mod(Expression<Integer> x, Integer y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression) x,
				literal( y )
		);
	}

	@Override
	public SqmExpression<Integer> mod(Integer x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				literal( x ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Double> sqrt(Expression<? extends Number> x) {
		final SqmExpression xSqmExpression = (SqmExpression) x;
		//noinspection unchecked
		return new SqmSqrtFunction(
				xSqmExpression,
				(AllowableFunctionReturnType) QueryHelper.highestPrecedenceType2(
						xSqmExpression.getExpressableType(),
						StandardSpiBasicTypes.DOUBLE
				),
				this
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Long> toLong(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asLong();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Integer> toInteger(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Float> toFloat(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asFloat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Double> toDouble(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asDouble();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asBigDecimal();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigInteger> toBigInteger(Expression<? extends Number> number) {
		return ( (SqmExpression) number ).asBigInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<String> toString(Expression<Character> character) {
		return ( (SqmExpression) character ).asString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmLiteral<T> literal(T value) {
		if ( value == null ) {
			return (SqmLiteral<T>) new SqmLiteralNull( this );
		}

		return new SqmLiteral(
				value,
				getTypeConfiguration().standardExpressableTypeForJavaType( value.getClass() ),
				this
		);
	}

	@Override
	public <T> List<? extends SqmExpression<T>> literals(T[] values) {
		if ( values == null || values.length == 0 ) {
			return Collections.emptyList();
		}

		final List<SqmLiteral<T>> literals = new ArrayList<>();
		for ( T value : values ) {
			literals.add( literal( value ) );
		}
		return literals;
	}

	@Override
	public <T> List<? extends SqmExpression<T>> literals(List<T> values) {
		if ( values == null || values.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<SqmLiteral<T>> literals = new ArrayList<>();
		for ( T value : values ) {
			literals.add( literal( value ) );
		}
		return literals;
	}

	@Override
	public <T> SqmExpression<T> nullLiteral(Class<T> resultClass) {
		//noinspection unchecked
		return new SqmLiteralNull( getTypeConfiguration().standardExpressableTypeForJavaType( resultClass ), this );
	}

	@Override
	public <T> SqmCriteriaParameter<T> parameter(Class<T> paramClass) {
		return new SqmCriteriaParameter<>(
				getTypeConfiguration().standardExpressableTypeForJavaType( paramClass ),
				false,
				this
		);
	}

	@Override
	public <T> SqmCriteriaParameter<T> parameter(Class<T> paramClass, String name) {
		return new SqmCriteriaParameter<>(
				name,
				getTypeConfiguration().standardExpressableTypeForJavaType( paramClass ),
				false,
				this
		);
	}

	@Override
	public SqmExpression<String> concat(Expression<String> x, Expression<String> y) {
		final SqmExpression xSqmExpression = (SqmExpression) x;
		final SqmExpression ySqmExpression = (SqmExpression) y;
		//noinspection unchecked
		return new SqmConcat(
				xSqmExpression,
				ySqmExpression,
				(BasicValuedExpressableType) highestPrecedenceType(
						xSqmExpression.getExpressableType(),
						ySqmExpression.getExpressableType(),
						StandardSpiBasicTypes.STRING
				),
				this
		);
	}

	@Override
	public SqmExpression<String> concat(Expression<String> x, String y) {
		final SqmExpression xSqmExpression = (SqmExpression) x;
		final SqmExpression ySqmExpression = literal( y );
		//noinspection unchecked
		return new SqmConcat(
				xSqmExpression,
				ySqmExpression,
				(BasicValuedExpressableType) highestPrecedenceType(
						xSqmExpression.getExpressableType(),
						ySqmExpression.getExpressableType(),
						StandardSpiBasicTypes.STRING
				),
				this
		);
	}

	@Override
	public SqmExpression<String> concat(String x, Expression<String> y) {
		final SqmExpression xSqmExpression = literal( x );
		final SqmExpression ySqmExpression = (SqmExpression) y;
		//noinspection unchecked
		return new SqmConcat(
				xSqmExpression,
				ySqmExpression,
				(BasicValuedExpressableType) highestPrecedenceType(
						xSqmExpression.getExpressableType(),
						ySqmExpression.getExpressableType(),
						StandardSpiBasicTypes.STRING
				),
				this
		);
	}

	@Override
	public SqmExpression<String> concat(String x, String y) {
		final SqmExpression xSqmExpression = literal( x );
		final SqmExpression ySqmExpression = literal( y );
		//noinspection unchecked
		return new SqmConcat(
				xSqmExpression,
				ySqmExpression,
				(BasicValuedExpressableType) highestPrecedenceType(
						xSqmExpression.getExpressableType(),
						ySqmExpression.getExpressableType(),
						StandardSpiBasicTypes.STRING
				),
				this
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, Expression<Integer> from) {
		return createSubstringNode(
				(SqmExpression) source,
				(SqmExpression) from,
				null
		);
	}

	private SqmFunction<String> createSubstringNode(SqmExpression source, SqmExpression from, SqmExpression len) {
		//noinspection unchecked
		final BasicValuedExpressableType resultType = (BasicValuedExpressableType) QueryHelper.highestPrecedenceType2(
				source.getExpressableType(),
				StandardSpiBasicTypes.STRING
		);

		final SqmFunctionTemplate registeredProducer = getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmSubstringFunction.NAME );

		if ( registeredProducer != null ) {
			//noinspection unchecked
			return (SqmFunction<String>) registeredProducer.makeSqmFunctionExpression(
					Collections.singletonList( from ),
					resultType,
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmSubstringFunction(
					source,
					from,
					len,
					resultType,
					this
			);
		}
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, int from) {
		return createSubstringNode(
				(SqmExpression) source,
				literal( from ),
				null
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, Expression<Integer> from, Expression<Integer> len) {
		return createSubstringNode(
				(SqmExpression) source,
				(SqmExpression) from,
				(SqmExpression) len
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, int from, int len) {
		return createSubstringNode(
				(SqmExpression) source,
				literal( from ),
				literal( len )
		);
	}

	@Override
	public SqmFunction<String> trim(Expression<String> source) {
		return createTrimNode( null, null, (SqmExpression) source );
	}

	private SqmFunction<String> createTrimNode(TrimSpecification trimSpecification, SqmExpression trimCharacter, SqmExpression source) {
		final SqmFunctionTemplate functionTemplate = getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmTrimFunction.NAME );
		if ( functionTemplate != null ) {
			final ArrayList<SqmTypedNode> arguments = new ArrayList<>();
			if ( trimSpecification != null ) {
				arguments.add(
						TrimSpecificationExpressionWrapper.from( trimSpecification )
				);
			}

			if ( trimCharacter != null ) {
				arguments.add( trimCharacter );
			}

			arguments.add( source );

			//noinspection unchecked
			return (SqmFunction<String>) functionTemplate.makeSqmFunctionExpression(
					arguments,
					(BasicValuedExpressableType) QueryHelper.highestPrecedenceType2( source.getExpressableType(), StandardSpiBasicTypes.STRING ),
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmTrimFunction(
					trimSpecification,
					trimCharacter,
					source,
					(BasicValuedExpressableType) QueryHelper.highestPrecedenceType2(
							source.getExpressableType(),
							StandardSpiBasicTypes.STRING
					),
					this
			);
		}
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, Expression<String> source) {
		return createTrimNode( convertTrimSpec( ts ), null, (SqmExpression) source );
	}

	private static TrimSpecification convertTrimSpec(Trimspec jpaTs) {
		if ( jpaTs == null ) {
			return null;
		}

		switch ( jpaTs ) {
			case BOTH: {
				return TrimSpecification.BOTH;
			}
			case LEADING: {
				return TrimSpecification.LEADING;
			}
			case TRAILING: {
				return TrimSpecification.TRAILING;
			}
		}

		throw new QueryException( "Could not resolve JPA TrimSpec : " + jpaTs );
	}

	@Override
	public SqmFunction<String> trim(Expression<Character> trimChar, Expression<String> source) {
		return createTrimNode( null, (SqmExpression) trimChar, (SqmExpression) source );
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, Expression<Character> trimChar, Expression<String> source) {
		return createTrimNode( convertTrimSpec( ts ), (SqmExpression) trimChar, (SqmExpression) source );
	}

	@Override
	public SqmFunction<String> trim(char trimChar, Expression<String> source) {
		return createTrimNode( null, literal( trimChar ), (SqmExpression) source );
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, char trimChar, Expression<String> source) {
		return createTrimNode( convertTrimSpec( ts ), literal( trimChar ), (SqmExpression) source );
	}

	@Override
	public SqmFunction<String> lower(Expression<String> x) {
		final SqmFunctionTemplate functionTemplate = getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmLowerFunction.NAME );

		final SqmExpression xSqmExpression = (SqmExpression) x;
		final BasicValuedExpressableType type = (BasicValuedExpressableType) highestPrecedenceType(
				xSqmExpression.getExpressableType(),
				StandardSpiBasicTypes.STRING
		);

		if ( functionTemplate != null ) {
			//noinspection unchecked
			return (SqmFunction<String>) functionTemplate.makeSqmFunctionExpression(
					Collections.singletonList( xSqmExpression ),
					type,
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmLowerFunction<>(
					xSqmExpression,
					type,
					this
			);
		}
	}

	@Override
	public SqmFunction<String> upper(Expression<String> x) {
		final SqmFunctionTemplate functionTemplate = getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmUpperFunction.NAME );

		final SqmExpression xSqmExpression = (SqmExpression) x;
		final BasicValuedExpressableType type = (BasicValuedExpressableType) highestPrecedenceType(
				xSqmExpression.getExpressableType(),
				StandardSpiBasicTypes.STRING
		);

		if ( functionTemplate != null ) {
			//noinspection unchecked
			return (SqmFunction<String>) functionTemplate.makeSqmFunctionExpression(
					Collections.singletonList( xSqmExpression ),
					type,
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmUpperFunction<>(
					xSqmExpression,
					type,
					this
			);
		}
	}

	@Override
	public SqmFunction<Integer> length(Expression<String> argument) {
		final SqmExpression argumentSqmExpression = (SqmExpression) argument;
		final SqmFunctionTemplate functionTemplate = getQueryEngine().getSqmFunctionRegistry()
				.findFunctionTemplate( SqmLengthFunction.NAME );

		if ( functionTemplate != null ) {
			//noinspection unchecked
			return (SqmFunction<Integer>) functionTemplate.makeSqmFunctionExpression(
					Collections.singletonList( argumentSqmExpression ),
					(AllowableFunctionReturnType) highestPrecedenceType( argumentSqmExpression.getExpressableType(), StandardSpiBasicTypes.INTEGER ),
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmLengthFunction( argumentSqmExpression, StandardSpiBasicTypes.INTEGER, this );
		}
	}

	@Override
	public SqmFunction<Integer> locate(Expression<String> source, Expression<String> pattern) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				(SqmExpression<String>) pattern,
				null
		);
	}

	private SqmFunction<Integer> createLocateFunctionNode(
			SqmExpression<String> source,
			SqmExpression<String> pattern,
			SqmExpression<Integer> startPosition) {
		final AllowableFunctionReturnType type = (AllowableFunctionReturnType) highestPrecedenceType(
				source.getExpressableType(),
				StandardSpiBasicTypes.STRING
		);

		final SqmFunctionTemplate functionTemplate = queryEngine.getSqmFunctionRegistry().findFunctionTemplate( SqmLocateFunction.NAME );

		if ( functionTemplate != null ) {
			final List<SqmTypedNode> arguments;
			if ( startPosition == null ) {
				arguments = Arrays.asList(
						source,
						pattern
				);
			}
			else {
				arguments = Arrays.asList(
						source,
						pattern
				);
			}

			//noinspection unchecked
			return (SqmFunction<Integer>) functionTemplate.makeSqmFunctionExpression(
					arguments,
					type,
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmLocateFunction(
					pattern,
					source,
					startPosition,
					type,
					this
			);
		}
	}

	@Override
	public SqmFunction<Integer> locate(Expression<String> source, String pattern) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				literal( pattern ),
				null
		);
	}

	@Override
	public SqmFunction<Integer> locate(Expression<String> source, Expression<String> pattern, Expression<Integer> startPosition) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				(SqmExpression<String>) pattern,
				(SqmExpression<Integer>) startPosition
		);
	}

	@Override
	public SqmFunction<Integer> locate(Expression<String> source, String pattern, int startPosition) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				literal( pattern ),
				literal( startPosition )
		);
	}

	@Override
	public SqmFunction<Date> currentDate() {
		return new SqmCurrentDateFunction( (AllowableFunctionReturnType) StandardSpiBasicTypes.DATE, this );
	}

	@Override
	public SqmFunction<Timestamp> currentTimestamp() {
		return new SqmCurrentTimestampFunction( (AllowableFunctionReturnType) StandardSpiBasicTypes.TIMESTAMP, this );
	}

	@Override
	public SqmFunction<Time> currentTime() {
		return new SqmCurrentTimeFunction( (AllowableFunctionReturnType) StandardSpiBasicTypes.TIME, this );
	}

	@Override
	public SqmFunction<Instant> currentInstant() {
		return new SqmCurrentInstantFunction( (AllowableFunctionReturnType) StandardSpiBasicTypes.INSTANT, this );
	}

	@Override
	public <T> SqmFunction<T> function(String name, Class<T> type, Expression<?>[] args) {
		final SqmFunctionTemplate functionTemplate = queryEngine.getSqmFunctionRegistry().findFunctionTemplate( SqmLocateFunction.NAME );
		if ( functionTemplate == null ) {
			throw new SemanticException( "Could not resolve function named `" + name + "`" );
		}

		//noinspection unchecked
		return (SqmFunction<T>) functionTemplate.makeSqmFunctionExpression(
				(List) expressionList( args ),
				getTypeConfiguration().standardExpressableTypeForJavaType( type ),
				getQueryEngine()
		);
	}

	private static List<SqmExpression<?>> expressionList(Expression<?>[] jpaExpressions) {
		if ( jpaExpressions == null || jpaExpressions.length == 0 ) {
			return Collections.emptyList();
		}

		final ArrayList<SqmExpression<?>> sqmExpressions = new ArrayList<>();
		for ( Expression<?> jpaExpression : jpaExpressions ) {
			sqmExpressions.add( (SqmExpression<?>) jpaExpression );
		}
		return sqmExpressions;
	}

	@Override
	public <Y> SqmRestrictedSubQueryExpression<Y> all(Subquery<Y> subquery) {
		return new SqmRestrictedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmRestrictedSubQueryExpression.Modifier.ALL,
				this
		);
	}

	@Override
	public <Y> SqmRestrictedSubQueryExpression<Y> some(Subquery<Y> subquery) {
		return new SqmRestrictedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmRestrictedSubQueryExpression.Modifier.SOME,
				this
		);
	}

	@Override
	public <Y> SqmRestrictedSubQueryExpression<Y> any(Subquery<Y> subquery) {
		return new SqmRestrictedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmRestrictedSubQueryExpression.Modifier.ANY,
				this
		);
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
		return new SqmCollectionSize( (SqmPath) collection, this );
	}

	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(C collection) {
		return new SqmLiteral<>(
				collection.size(),
				StandardSpiBasicTypes.INTEGER,
				this
		);
	}

	@Override
	public <T> SqmCoalesceFunction<T> coalesce() {
		//noinspection unchecked
		return new SqmCoalesceFunction( this );
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		//noinspection unchecked
		final SqmExpression<Y> xSqmExpression = (SqmExpression) x;
		//noinspection unchecked
		final SqmExpression<Y> ySqmExpression = (SqmExpression) y;

		//noinspection unchecked
		final SqmCoalesceFunction<Y> coalesce = new SqmCoalesceFunction(
				(AllowableFunctionReturnType) highestPrecedenceType(
						xSqmExpression.getExpressableType(),
						ySqmExpression.getExpressableType()
				),
				this
		);

		coalesce.value( xSqmExpression );
		coalesce.value( ySqmExpression );

		return coalesce;
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y) {
		return coalesce( x, literal( y ) );
	}

	@Override
	public <Y> JpaExpression<Y> nullif(Expression<Y> x, Expression<?> y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression) x, (SqmExpression) y );
	}

	@Override
	public <Y> JpaExpression<Y> nullif(Expression<Y> x, Y y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression) x, literal( y ) );
	}

	private <Y> JpaExpression<Y> createNullifFunctionNode(SqmExpression<Y> first, SqmExpression<Y> second) {
		//noinspection unchecked
		final AllowableFunctionReturnType<Y> type = (AllowableFunctionReturnType<Y>) highestPrecedenceType(
				first.getExpressableType(),
				second.getExpressableType()
		);

		final SqmFunctionTemplate functionTemplate = queryEngine.getSqmFunctionRegistry()
				.findFunctionTemplate( SqmNullifFunction.NAME );

		if ( functionTemplate != null ) {
			//noinspection unchecked
			return functionTemplate.makeSqmFunctionExpression(
					Arrays.asList( first, second ),
					type,
					getQueryEngine()
			);
		}
		else {
			//noinspection unchecked
			return new SqmNullifFunction(
					first,
					second,
					type,
					this
			);
		}
	}

	@Override
	public <C, R> SqmCaseSimple<C, R> selectCase(Expression<? extends C> expression) {
		//noinspection unchecked
		return new SqmCaseSimple<>( (SqmExpression) expression, this );
	}

	@Override
	public <R> SqmCaseSearched<R> selectCase() {
		return new SqmCaseSearched<>( this );
	}

	@Override
	public <R> SqmTuple<R> tuple(Class<R> tupleType, JpaExpression<?>... expressions) {
		//noinspection unchecked
		return new SqmTuple<R>(
				(List) Arrays.asList( expressions ),
//				getTypeConfiguration().standardExpressableTypeForJavaType( tupleType ),
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(Class<R> tupleType, List<JpaExpression<?>> expressions) {
		//noinspection unchecked
		return new SqmTuple<R>(
				(List) expressions,
//				getTypeConfiguration().standardExpressableTypeForJavaType( tupleType ),
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(DomainType<R> tupleType, JpaExpression<?>... expressions) {
		//noinspection unchecked
		return new SqmTuple<R>(
				(List) Arrays.asList( expressions ),
				tupleType,
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(
			DomainType<R> tupleType, List<JpaExpression<?>> expressions) {
		//noinspection unchecked
		return new SqmTuple<R>(
				(List) Arrays.asList( expressions ),
				tupleType,
				this
		);
	}

	@Override
	public <M extends Map<?, ?>> SqmExpression<Integer> mapSize(JpaExpression<M> mapExpression) {
		return new SqmCollectionSize( (SqmPath<?>) mapExpression, this );
	}

	@Override
	public SqmExpression<Integer> mapSize(Map map) {
		return new SqmLiteral<>( map.size(), StandardSpiBasicTypes.INTEGER, this );
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SqmPredicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return new SqmAndPredicate(
				wrap( x ),
				wrap( y ),
				this
		);
	}

	@Override
	public SqmPredicate and(Predicate... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return conjunction();
		}

		SqmPredicate junction = (SqmPredicate) restrictions[0];
		for ( int i = 1; i < restrictions.length; i++ ) {
			junction = new SqmAndPredicate( junction, (SqmPredicate) restrictions[i], this );
		}

		return junction;
	}

	@Override
	public SqmPredicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return new SqmOrPredicate(
				wrap( x ),
				wrap( y ),
				this
		);
	}

	@Override
	public SqmPredicate or(Predicate... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return disjunction();
		}

		SqmPredicate junction = (SqmPredicate) restrictions[0];
		for ( int i = 1; i < restrictions.length; i++ ) {
			junction = new SqmOrPredicate( junction, (SqmPredicate) restrictions[i], this );
		}

		return junction;
	}

	@Override
	public SqmPredicate not(Expression<Boolean> restriction) {
		final SqmPredicate predicate = wrap( restriction );
		return predicate.not();
	}

	@Override
	public SqmPredicate conjunction() {
		//noinspection unchecked
		return new SqmComparisonPredicate(
				new SqmLiteral( 1, StandardSpiBasicTypes.INTEGER, this ),
				ComparisonOperator.EQUAL,
				new SqmLiteral( 1, StandardSpiBasicTypes.INTEGER, this ),
				this
		);
	}

	@Override
	public SqmPredicate disjunction() {
		//noinspection unchecked
		return new SqmComparisonPredicate(
				new SqmLiteral( 1, StandardSpiBasicTypes.INTEGER, this ),
				ComparisonOperator.NOT_EQUAL,
				new SqmLiteral( 1, StandardSpiBasicTypes.INTEGER, this ),
				this
		);
	}

	@Override
	public SqmPredicate isTrue(Expression<Boolean> x) {
		return wrap( x );
	}

	@Override
	public SqmPredicate isFalse(Expression<Boolean> x) {
		return wrap( x ).not();
	}

	@Override
	public SqmPredicate isNull(Expression<?> x) {
		return new SqmNullnessPredicate( (SqmExpression) x, this );
	}

	@Override
	public SqmPredicate isNotNull(Expression<?> x) {
		return new SqmNullnessPredicate( (SqmExpression) x, this ).not();
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Expression<? extends Y> lower, Expression<? extends Y> upper) {
		return new SqmBetweenPredicate(
				(SqmExpression) value,
				(SqmExpression) lower,
				(SqmExpression) upper,
				false,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Y lower, Y upper) {
		return new SqmBetweenPredicate(
				(SqmExpression) value,
				literal( lower ),
				literal( upper ),
				false,
				this
		);
	}

	@Override
	public SqmPredicate equal(Expression<?> x, Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate equal(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public SqmPredicate notEqual(Expression<?> x, Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate notEqual(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				literal( y ),
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				literal( y ),
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public SqmPredicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate gt(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				literal( y ),
				this
		);
	}

	@Override
	public SqmPredicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate ge(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public SqmPredicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate lt(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				literal( y ),
				this
		);
	}

	@Override
	public SqmPredicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate le(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				literal( y ),
				this
		);
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isEmpty(Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isNotEmpty(Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(Expression<E> elem, Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(E elem, Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(Expression<E> elem, Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(E elem, Expression<C> collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				(SqmExpression) pattern,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				literal( pattern ),
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern, Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				(SqmExpression) pattern,
				(SqmExpression) escapeChar,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				(SqmExpression) pattern,
				literal( escapeChar ),
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern, Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				literal( pattern ),
				(SqmExpression) escapeChar,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				literal( pattern ),
				literal( escapeChar ),
				this
		);
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, Expression<String> pattern) {
		return not( like( x, pattern ) );
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, String pattern) {
		return not( like( x, pattern ) );
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notLike(Expression<String> x, String pattern, char escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Override
	public <T> SqmInPredicate in(Expression<? extends T> expression) {
		//noinspection unchecked
		return new SqmInListPredicate( (SqmExpression) expression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate in(Expression<? extends T> expression, Expression<? extends T>... values) {
		final SqmInListPredicate predicate = new SqmInListPredicate( (SqmExpression) expression, this );
		for ( Expression<? extends T> value : values ) {
			predicate.addExpression( (SqmExpression) value );
		}
		return predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate in(Expression<? extends T> expression, T... values) {
		final SqmExpression sqmExpression = (SqmExpression) expression;
		final SqmInListPredicate predicate = new SqmInListPredicate( sqmExpression, this );
		for ( T value : values ) {
			//noinspection unchecked
			predicate.addExpression(
					new SqmLiteral( value, (BasicValuedExpressableType) sqmExpression.getExpressableType(), this )
			);
		}
		return predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate in(Expression<? extends T> expression, List<T> values) {
		final SqmExpression sqmExpression = (SqmExpression) expression;
		final SqmInListPredicate predicate = new SqmInListPredicate( sqmExpression, this );
		for ( T value : values ) {
			predicate.addExpression(
					new SqmLiteral( value, (BasicValuedExpressableType) sqmExpression.getExpressableType(), this )
			);
		}
		return predicate;
	}

	@Override
	public <T> SqmInPredicate in(Expression<? extends T> expression, SqmSubQuery<T> subQuery) {
		//noinspection unchecked
		return new SqmInSubQueryPredicate( (SqmExpression) expression, subQuery, this );
	}

	@Override
	public SqmPredicate exists(Subquery<?> subquery) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapEmpty(JpaExpression<M> mapExpression) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapNotEmpty(JpaExpression<M> mapExpression) {
		throw new NotYetImplementedFor6Exception();
	}
}
