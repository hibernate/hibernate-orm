/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaQuery;
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
import org.hibernate.query.NullPrecedence;
import org.hibernate.QueryException;
import org.hibernate.query.SetOperator;
import org.hibernate.query.SortOrder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Arrays.asList;
import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;

/**
 * Acts as a JPA {@link javax.persistence.criteria.CriteriaBuilder} by
 * using SQM nodes as the JPA Criteria nodes
 * 
 * @author Steve Ebersole
 */
public class SqmCriteriaNodeBuilder implements NodeBuilder, SqmCreationContext {
	/**
	 * Simplified creation from a SessionFactory
	 */
	public static SqmCriteriaNodeBuilder create(SessionFactoryImplementor sf) {
		return new SqmCriteriaNodeBuilder( 
				sf.getQueryEngine(),
				() -> sf.getRuntimeMetamodels().getJpaMetamodel(),
				sf.getServiceRegistry(),
				sf.getSessionFactoryOptions().getCriteriaValueHandlingMode()
		);
	}

	private final QueryEngine queryEngine;
	private final Supplier<JpaMetamodel> domainModelAccess;
	private final ServiceRegistry serviceRegistry;
	private final ValueHandlingMode criteriaValueHandlingMode;

	public SqmCriteriaNodeBuilder(
			QueryEngine queryEngine,
			Supplier<JpaMetamodel> domainModelAccess,
			ServiceRegistry serviceRegistry,
			ValueHandlingMode criteriaValueHandlingMode) {
		this.queryEngine = queryEngine;
		this.domainModelAccess = domainModelAccess;
		this.serviceRegistry = serviceRegistry;
		this.criteriaValueHandlingMode = criteriaValueHandlingMode;
	}

	@Override
	public JpaMetamodel getDomainModel() {
		return domainModelAccess.get();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return domainModelAccess.get();
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
		return new SqmDeleteStatement<>( targetEntity, SqmQuerySource.CRITERIA, this );
	}

	@Override
	public <T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity) {
		return new SqmInsertSelectStatement<>( targetEntity, this );
	}

	@Override
	public <T> JpaCriteriaQuery<T> union(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.UNION_ALL : SetOperator.UNION, query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersect(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT, query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> except(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT, query1, queries );
	}

	private <T> JpaCriteriaQuery<T> setOperation(
			SetOperator operator,
			CriteriaQuery<? extends T> query1,
			CriteriaQuery<?>... queries) {
		final Class<T> resultType = (Class<T>) query1.getResultType();
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( queries.length + 1 );
		queryParts.add( ( (SqmSelectQuery<T>) query1 ).getQueryPart() );
		for ( CriteriaQuery<?> query : queries ) {
			if ( query.getResultType() != resultType ) {
				throw new IllegalArgumentException( "Result type of all operands must match!" );
			}
			queryParts.add( ( (SqmSelectQuery<T>) query ).getQueryPart() );
		}
		return new SqmSelectStatement<>(
				new SqmQueryGroup<>( this, operator, queryParts ),
				resultType,
				SqmQuerySource.CRITERIA,
				this
		);
	}

	@Override
	public <X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType) {
		final BasicDomainType<X> type = getTypeConfiguration().standardBasicTypeForJavaType( castTargetJavaType );
		return getFunctionDescriptor("cast").generateSqmExpression(
				asList( (SqmTypedNode<?>) expression, new SqmCastTarget<>( type, this ) ),
				type,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
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
		final SqmDynamicInstantiation<Y> instantiation;
		if ( List.class.equals( resultClass ) ) {
			//noinspection unchecked
			instantiation = (SqmDynamicInstantiation<Y>) SqmDynamicInstantiation.forListInstantiation( this );
		}
		else if ( Map.class.equals( resultClass ) ) {
			//noinspection unchecked
			instantiation = (SqmDynamicInstantiation<Y>) SqmDynamicInstantiation.forMapInstantiation( this );
		}
		else {
			instantiation = SqmDynamicInstantiation.forClassInstantiation( resultClass, this );
		}

		for ( Selection<?> argument : arguments ) {
			instantiation.addArgument(
					new SqmDynamicInstantiationArgument<>(
							(SqmSelectableNode<?>) argument,
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
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression );
	}

	@Override
	public SqmSortSpecification asc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortOrder.ASCENDING );
	}

	@Override
	public SqmSortSpecification desc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortOrder.DESCENDING );
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(Selection<?>[] selections) {
		return new SqmJpaCompoundSelection<>(
				ArrayHelper.toList( selections ),
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Tuple.class ),
				this
		);
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(List<? extends JpaSelection<?>> selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection<>(
				(List<SqmSelectableNode<?>>) selections,
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Tuple.class ),
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(Class<R> tupleType, JpaExpression<?>... expressions) {
		//noinspection unchecked
		return new SqmTuple<>(
				(List<SqmExpression<?>>) (List<?>) asList( expressions ),
//				getTypeConfiguration().standardExpressableTypeForJavaType( tupleType ),
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(Class<R> tupleType, List<JpaExpression<?>> expressions) {
		//noinspection unchecked
		return new SqmTuple<>(
				(List<SqmExpression<?>>) (List<?>) expressions,
//				getTypeConfiguration().standardExpressableTypeForJavaType( tupleType ),
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(DomainType<R> tupleType, JpaExpression<?>... expressions) {
		//noinspection unchecked
		return new SqmTuple<>(
				(List<SqmExpression<?>>) (List<?>) asList( expressions ),
				tupleType,
				this
		);
	}

	@Override
	public <R> SqmTuple<R> tuple(DomainType<R> tupleType, List<JpaExpression<?>> expressions) {
		//noinspection unchecked
		return new SqmTuple<>(
				new ArrayList<>( (List<SqmExpression<?>>) (List<?>) expressions ),
				tupleType,
				this
		);
	}

	@Override
	public JpaCompoundSelection<Object[]> array(Selection<?>[] selections) {
		return new SqmJpaCompoundSelection<>(
				ArrayHelper.toList( selections ),
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Object[].class ),
				this
		);
	}

	@Override
	public JpaCompoundSelection<Object[]> array(List<? extends JpaSelection<?>> selections) {
		//noinspection unchecked
		return new SqmJpaCompoundSelection<>(
				(List<SqmSelectableNode<?>>) selections,
				getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Object[].class ),
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument) {
		return getFunctionDescriptor("avg").generateSqmExpression(
				(SqmTypedNode<?>) argument,
				StandardBasicTypes.DOUBLE,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<N> argument) {
		return getFunctionDescriptor("sum").generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
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
		return getFunctionDescriptor("max").generateSqmExpression(
				(SqmTypedNode<?>) argument,
				(AllowableFunctionReturnType<N>) ((SqmExpression<N>) argument).getNodeType(),
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> min(Expression<N> argument) {
		return getFunctionDescriptor("min").generateSqmExpression(
				(SqmTypedNode<?>) argument,
				(AllowableFunctionReturnType<N>) ((SqmExpression<N>) argument).getNodeType(),
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> greatest(Expression<X> argument) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "max" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine, getTypeConfiguration() );
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> least(Expression<X> argument) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "min" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine, getTypeConfiguration() );
	}

	@Override
	public SqmExpression<Long> count(Expression<?> argument) {
		return getFunctionDescriptor("count").generateSqmExpression(
				(SqmTypedNode<?>) argument,
				StandardBasicTypes.LONG,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmExpression<Long> countDistinct(Expression<?> argument) {
		return getFunctionDescriptor("count").generateSqmExpression(
				new SqmDistinct<>( (SqmExpression<?>) argument, getQueryEngine().getCriteriaBuilder() ),
				StandardBasicTypes.LONG,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
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
		return getFunctionDescriptor("abs").generateSqmExpression(
				(SqmTypedNode<?>) x,
				(AllowableFunctionReturnType<N>) ((SqmExpression<N>) x).getNodeType(),
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD, (SqmExpression<?>) x, (SqmExpression<?>) y );
	}

	private <N extends Number> SqmExpression<N> createSqmArithmeticNode(
			BinaryArithmeticOperator operator,
			SqmExpression<?> leftHandExpression,
			SqmExpression<?> rightHandExpression) {
		//noinspection unchecked
		return new SqmBinaryArithmetic<>(
				operator,
				leftHandExpression,
				rightHandExpression,
				(SqmExpressable<N>) getDomainModel().getTypeConfiguration().resolveArithmeticType(
						leftHandExpression.getNodeType(),
						rightHandExpression.getNodeType(),
						operator
				),
				this
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				(SqmExpression<?>) x,
				value( y, (SqmExpression) x )
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <N extends Number> SqmExpression<N> sum(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				value( x, (SqmExpression) y ),
				(SqmExpression<?>) y
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <N extends Number> SqmExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				value( x, (SqmExpression) y ),
				(SqmExpression<?>) y
		);
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
				(SqmExpression<?>) x,
				(SqmExpression<?>) y
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <N extends Number> SqmExpression<N> diff(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression) x,
				value( y, (SqmExpression) x )
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <N extends Number> SqmExpression<N> diff(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				value( x, (SqmExpression) y ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression<?>) x,
				(SqmExpression<?>) y
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmExpression<Number> quot(Expression<? extends Number> x, Number y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression) x,
				value( y, (SqmExpression) x )
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmExpression<Number> quot(Number x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				value( x, (SqmExpression) y ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression<?>) x,
				(SqmExpression<?>) y
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmExpression<Integer> mod(Expression<Integer> x, Integer y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression) x,
				value( y, (SqmExpression) x )
		);
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmExpression<Integer> mod(Integer x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				value( x, (SqmExpression) y ),
				(SqmExpression) y
		);
	}

	@Override
	public SqmExpression<Double> sqrt(Expression<? extends Number> x) {
		//noinspection unchecked
		return getFunctionDescriptor("sqrt").generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine,
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Long> toLong(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asLong();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Integer> toInteger(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asInteger();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Float> toFloat(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asFloat();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Double> toDouble(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asDouble();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigDecimal();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<BigInteger> toBigInteger(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigInteger();
	}

	@Override
	public SqmExpression<String> toString(Expression<Character> character) {
		return ( (SqmExpression<?>) character ).asString();
	}

	@Override
	public <T> SqmLiteral<T> literal(T value, SqmExpression<T> typeInferenceSource) {
		if ( value == null ) {
			return new SqmLiteralNull<>( this );
		}

		final SqmExpressable<T> expressable;
		if ( value instanceof Enum ) {
			final Enum enumValue = (Enum) value;
			//noinspection unchecked
			expressable = new SqmEnumLiteral(
					enumValue,
					(EnumJavaTypeDescriptor) getTypeConfiguration().getJavaTypeDescriptorRegistry().resolveDescriptor( value.getClass() ),
					enumValue.name(),
					this
			);
		}
		else {
			expressable = resolveInferredType( value, typeInferenceSource, getTypeConfiguration() );
		}

		return new SqmLiteral<>( value, expressable, this );
	}

	private static <T> SqmExpressable<T> resolveInferredType(
			T value,
			SqmExpression<T> typeInferenceSource,
			TypeConfiguration typeConfiguration) {
		if ( typeInferenceSource != null ) {
			return typeInferenceSource.getNodeType();
		}

		if ( value == null ) {
			return null;
		}

		//noinspection unchecked
		return (BasicType<T>) typeConfiguration.getBasicTypeForJavaType( value.getClass() );
	}

	@Override
	public <T> SqmLiteral<T> literal(T value) {
		if ( value == null ) {
			return new SqmLiteralNull<>( this );
		}

		final SqmExpressable<T> expressable;
		if ( value instanceof Enum ) {
			final Enum enumValue = (Enum) value;
			//noinspection unchecked
			expressable = new SqmEnumLiteral(
					enumValue,
					(EnumJavaTypeDescriptor) getTypeConfiguration().getJavaTypeDescriptorRegistry().resolveDescriptor( value.getClass() ),
					enumValue.name(),
					this
			);
		}
		else {
			//noinspection unchecked
			expressable = getTypeConfiguration().standardBasicTypeForJavaType( (Class<T>) value.getClass() );
		}

		return new SqmLiteral<>(
				value,
				expressable,
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
		return new SqmLiteralNull<>( getTypeConfiguration().standardBasicTypeForJavaType( resultClass ), this );
	}

	class MultiValueParameterType<T> implements AllowableParameterType<T> {
		private final JavaTypeDescriptor<T> javaTypeDescriptor;

		public MultiValueParameterType(Class<T> type) {
			this.javaTypeDescriptor = domainModelAccess.get()
					.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( type );
		}

		@Override
		public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}

		@Override
		public PersistenceType getPersistenceType() {
			return PersistenceType.BASIC;
		}

		@Override
		public Class<T> getJavaType() {
			return javaTypeDescriptor.getJavaTypeClass();
		}
	}

	@Override
	public <T> JpaCriteriaParameter<T> parameter(Class<T> paramClass) {
		if ( Collection.class.isAssignableFrom( paramClass ) ) {
			// a Collection-valued, multi-valued parameter
			return new JpaCriteriaParameter(
					new MultiValueParameterType( Collection.class ),
					true,
					this
			);
		}

		if ( paramClass.isArray() ) {
			// an array-valued, multi-valued parameter
			return new JpaCriteriaParameter(
					new MultiValueParameterType( Object[].class ),
					true,
					this
			);
		}

		//noinspection unchecked
		return new JpaCriteriaParameter<>(
				getTypeConfiguration().standardBasicTypeForJavaType( paramClass ),
				false,
				this
		);
	}

	@Override
	public <T> JpaCriteriaParameter<T> parameter(Class<T> paramClass, String name) {
		if ( Collection.class.isAssignableFrom( paramClass ) ) {
			// a multi-valued parameter
			return new JpaCriteriaParameter(
					name,
					new MultiValueParameterType<>( Collection.class ),
					true,
					this
			);
		}

		if ( paramClass.isArray() ) {
			// an array-valued, multi-valued parameter
			return new JpaCriteriaParameter(
					name,
					new MultiValueParameterType( Object[].class ),
					true,
					this
			);
		}

		//noinspection unchecked
		return new JpaCriteriaParameter<>(
				name,
				getTypeConfiguration().standardBasicTypeForJavaType( paramClass ),
				false,
				this
		);
	}

	@Override
	public SqmExpression<String> concat(Expression<String> x, Expression<String> y) {
		final SqmExpression xSqmExpression = (SqmExpression) x;
		final SqmExpression ySqmExpression = (SqmExpression) y;
		//noinspection unchecked
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				(AllowableFunctionReturnType<String>) highestPrecedenceType(
						xSqmExpression.getNodeType(),
						ySqmExpression.getNodeType(),
						StandardBasicTypes.STRING
				),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SqmExpression<String> concat(Expression<String> x, String y) {
		final SqmExpression xSqmExpression = (SqmExpression) x;
		final SqmExpression ySqmExpression = value( y, xSqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				(AllowableFunctionReturnType<String>) highestPrecedenceType(
						xSqmExpression.getNodeType(),
						ySqmExpression.getNodeType(),
						StandardBasicTypes.STRING
				),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SqmExpression<String> concat(String x, Expression<String> y) {
		final SqmExpression ySqmExpression = (SqmExpression) y;
		final SqmExpression xSqmExpression = value( x, ySqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				(AllowableFunctionReturnType<String>) highestPrecedenceType(
						xSqmExpression.getNodeType(),
						ySqmExpression.getNodeType(),
						StandardBasicTypes.STRING
				),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SqmExpression<String> concat(String x, String y) {
		final SqmExpression xSqmExpression = value( x );
		final SqmExpression ySqmExpression = value( y, xSqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				(AllowableFunctionReturnType<String>) highestPrecedenceType(
						xSqmExpression.getNodeType(),
						ySqmExpression.getNodeType(),
						StandardBasicTypes.STRING
				),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
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
		final AllowableFunctionReturnType resultType = (AllowableFunctionReturnType)  QueryHelper.highestPrecedenceType2(
				source.getNodeType(),
				StandardBasicTypes.STRING
		);

		//noinspection unchecked
		return getFunctionDescriptor( "substring" ).generateSqmExpression(
				len==null ? asList( source, from ) : asList( source, from, len ),
				resultType,
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, int from) {
		return createSubstringNode(
				(SqmExpression) source,
				value( from ),
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
				value( from ),
				value( len )
		);
	}

	@Override
	public SqmFunction<String> trim(Expression<String> source) {
		return createTrimNode( null, null, (SqmExpression) source );
	}

	private SqmFunction<String> createTrimNode(TrimSpec trimSpecification, SqmExpression trimCharacter, SqmExpression source) {

		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>();

		if ( trimSpecification != null ) {
			arguments.add(
					new SqmTrimSpecification( trimSpecification, this )
			);
		}

		if ( trimCharacter != null ) {
			arguments.add( trimCharacter );
		}

		arguments.add( source );

		//noinspection unchecked
		return getFunctionDescriptor( "trim" ).generateSqmExpression(
				arguments,
				(AllowableFunctionReturnType<String>) QueryHelper.highestPrecedenceType2( source.getNodeType(), StandardBasicTypes.STRING ),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, Expression<String> source) {
		return createTrimNode( convertTrimSpec( ts ), null, (SqmExpression) source );
	}

	private static TrimSpec convertTrimSpec(Trimspec jpaTs) {
		if ( jpaTs == null ) {
			return null;
		}

		switch ( jpaTs ) {
			case BOTH: {
				return TrimSpec.BOTH;
			}
			case LEADING: {
				return TrimSpec.LEADING;
			}
			case TRAILING: {
				return TrimSpec.TRAILING;
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

		final AllowableFunctionReturnType type = (AllowableFunctionReturnType)  highestPrecedenceType(
				((SqmExpression) x).getNodeType(),
				StandardBasicTypes.STRING
		);

		//noinspection unchecked
		return getFunctionDescriptor( "lower" ).generateSqmExpression(
				(SqmExpression) x,
				type,
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmFunction<String> upper(Expression<String> x) {
		final AllowableFunctionReturnType type = (AllowableFunctionReturnType) highestPrecedenceType(
				((SqmExpression) x).getNodeType(),
				StandardBasicTypes.STRING
		);

		//noinspection unchecked
		return getFunctionDescriptor( "upper" ).generateSqmExpression(
				(SqmExpression) x,
				type,
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmFunction<Integer> length(Expression<String> argument) {
		//noinspection unchecked
		return getFunctionDescriptor( "length" ).generateSqmExpression(
				(SqmExpression) argument,
				(AllowableFunctionReturnType) highestPrecedenceType(
						((SqmExpression) argument).getNodeType(),
						StandardBasicTypes.INTEGER
				),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
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
				source.getNodeType(),
				StandardBasicTypes.INTEGER
		);

		final List<SqmTypedNode<?>> arguments;
		if ( startPosition == null ) {
			arguments = asList(
					source,
					pattern
			);
		}
		else {
			arguments = asList(
					source,
					pattern
			);
		}

		//noinspection unchecked
		return getFunctionDescriptor("locate").generateSqmExpression(
				arguments,
				type,
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	@Override
	public SqmFunction<Integer> locate(Expression<String> source, String pattern) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				value( pattern ),
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
				value( pattern ),
				value( startPosition )
		);
	}

	@Override
	public SqmFunction<Date> currentDate() {
		//noinspection unchecked
		return getFunctionDescriptor("current_date")
				.generateSqmExpression(
						(AllowableFunctionReturnType) StandardBasicTypes.DATE,
						queryEngine,
						getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmFunction<Timestamp> currentTimestamp() {
		//noinspection unchecked
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						(AllowableFunctionReturnType) StandardBasicTypes.TIMESTAMP,
						queryEngine,
						getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmFunction<Time> currentTime() {
		//noinspection unchecked
		return getFunctionDescriptor("current_time")
				.generateSqmExpression(
						(AllowableFunctionReturnType) StandardBasicTypes.TIME,
						queryEngine,
						getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public SqmFunction<Instant> currentInstant() {
		//noinspection unchecked
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						StandardBasicTypes.INSTANT,
						queryEngine,
						getJpaMetamodel().getTypeConfiguration()
				);
	}

	@Override
	public <T> SqmFunction<T> function(String name, Class<T> type, Expression<?>[] args) {
		final SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( name);
		if ( functionTemplate == null ) {
			throw new SemanticException( "Could not resolve function named `" + name + "`" );
		}

		//noinspection unchecked
		return functionTemplate.generateSqmExpression(
				(List) expressionList( args ),
				getTypeConfiguration().getBasicTypeForJavaType( type ),
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
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
	public <K, M extends Map<K, ?>> SqmExpression<Set<K>> keys(M map) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <K, L extends List<?>> SqmExpression<Set<K>> indexes(L list) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Creates an expression for the value with the given "type inference" information
	 */
	@Override
	public <T> SqmExpression<T> value(T value, SqmExpression<T> typeInferenceSource) {
		if ( typeInferenceSource == null ) {
			return value( value );
		}

		if ( criteriaValueHandlingMode == ValueHandlingMode.INLINE ) {
			return literal( value, typeInferenceSource );
		}

		return new JpaCriteriaParameter<>(
				resolveInferredParameterType( value, typeInferenceSource, getTypeConfiguration() ),
				value,
				this
		);
	}

	private static <T> AllowableParameterType<T> resolveInferredParameterType(
			T value,
			SqmExpression<T> typeInferenceSource,
			TypeConfiguration typeConfiguration) {
		if ( typeInferenceSource != null ) {
			if ( typeInferenceSource instanceof AllowableParameterType ) {
				//noinspection unchecked
				return (AllowableParameterType<T>) typeInferenceSource;
			}

			if ( typeInferenceSource.getNodeType() instanceof AllowableParameterType ) {
				return (AllowableParameterType<T>) typeInferenceSource.getNodeType();
			}
		}

		if ( value == null ) {
			return null;
		}

		//noinspection unchecked
		return (BasicType<T>) typeConfiguration.getBasicTypeForJavaType( value.getClass() );
	}

	@Override
	public <T> SqmExpression<T> value(T value) {
		if ( criteriaValueHandlingMode == ValueHandlingMode.INLINE ) {
			return literal( value );
		}
		else {
			final BasicType basicType;
			if ( value == null ) {
				basicType = null;
			}
			else {
				basicType = getTypeConfiguration().getBasicTypeForJavaType( value.getClass() );
			}
			return new JpaCriteriaParameter<>(
					basicType,
					value,
					this
			);
		}
	}

	@Override
	public <V, C extends Collection<V>> SqmExpression<Collection<V>> values(C collection) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(Expression<C> collection) {
		return new SqmCollectionSize( (SqmPath) collection, this );
	}

	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(C collection) {
		//noinspection unchecked
		return new SqmLiteral<>(
				collection.size(),
				StandardBasicTypes.INTEGER,
				this
		);
	}

	@Override
	public <T> SqmCoalesce<T> coalesce() {
		return new SqmCoalesce<>( this );
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		//noinspection unchecked
		return new SqmCoalesce<>(
				highestPrecedenceType(
						((SqmExpression) x).getNodeType(),
						((SqmExpression) y).getNodeType()
				),
				this
		)
				.value(x)
				.value(y);
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y) {
		return coalesce( x, value( y, (SqmExpression) x ) );
	}

	@Override
	public <Y> SqmExpression<Y> nullif(Expression<Y> x, Expression<?> y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression) x, (SqmExpression) y );
	}

	@Override
	public <Y> SqmExpression<Y> nullif(Expression<Y> x, Y y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression) x, value( y, (SqmExpression) x ) );
	}

	private <Y> SqmExpression<Y> createNullifFunctionNode(SqmExpression<Y> first, SqmExpression<Y> second) {
		//noinspection unchecked
		final AllowableFunctionReturnType<Y> type = (AllowableFunctionReturnType<Y>) highestPrecedenceType(
				first.getNodeType(),
				second.getNodeType()
		);

		return getFunctionDescriptor("nullif").generateSqmExpression(
				asList( first, second ),
				type,
				getQueryEngine(),
				getJpaMetamodel().getTypeConfiguration()
		);
	}

	private SqmFunctionDescriptor getFunctionDescriptor(String name) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( name);
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
	public <M extends Map<?, ?>> SqmExpression<Integer> mapSize(JpaExpression<M> mapExpression) {
		return new SqmCollectionSize( (SqmPath<?>) mapExpression, this );
	}

	@Override
	public SqmExpression<Integer> mapSize(Map map) {
		//noinspection unchecked
		return new SqmLiteral<>( map.size(), StandardBasicTypes.INTEGER, this );
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
				new SqmLiteral( 1, StandardBasicTypes.INTEGER, this ),
				ComparisonOperator.EQUAL,
				new SqmLiteral( 1, StandardBasicTypes.INTEGER, this ),
				this
		);
	}

	@Override
	public SqmPredicate disjunction() {
		//noinspection unchecked
		return new SqmComparisonPredicate(
				new SqmLiteral( 1, StandardBasicTypes.INTEGER, this ),
				ComparisonOperator.NOT_EQUAL,
				new SqmLiteral( 1, StandardBasicTypes.INTEGER, this ),
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Y lower, Y upper) {
		final SqmExpression valueExpression = (SqmExpression) value;
		return new SqmBetweenPredicate(
				valueExpression,
				value( lower, valueExpression ),
				value( upper, valueExpression ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate equal(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public SqmPredicate notEqual(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression) x,
				ComparisonOperator.NOT_EQUAL,
				value( y, (SqmExpression) x ),
				this
		);
	}

	@Override
	public SqmPredicate distinctFrom(Expression<?> x, Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.DISTINCT_FROM,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate distinctFrom(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.DISTINCT_FROM,
				value( y ),
				this
		);
	}

	@Override
	public SqmPredicate notDistinctFrom(Expression<?> x, Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_DISTINCT_FROM,
				(SqmExpression<?>) y,
				this
		);
	}

	@Override
	public SqmPredicate notDistinctFrom(Expression<?> x, Object y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_DISTINCT_FROM,
				value( y ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Y y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				value( y, (SqmExpression) x ),
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
				(SqmExpression<?>) y,
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate gt(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate ge(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate lt(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				value( y, (SqmExpression) x ),
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SqmPredicate le(Expression<? extends Number> x, Number y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				value( y, (SqmExpression) x ),
				this
		);
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isEmpty(Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath) collection, false, this );
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isNotEmpty(Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath) collection, true, this );
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(Expression<E> elem, Expression<C> collection) {
		return new SqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, false, this );
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(E elem, Expression<C> collection) {
		return new SqmMemberOfPredicate( value( elem ), (SqmPath<?>) collection, false, this );
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(Expression<E> elem, Expression<C> collection) {
		return new SqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, true, this );
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(E elem, Expression<C> collection) {
		return new SqmMemberOfPredicate( value( elem ), (SqmPath<?>) collection, true, this );
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
				value( pattern, (SqmExpression) searchString ),
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
				value( pattern, (SqmExpression) searchString ),
				(SqmExpression) escapeChar,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression) searchString,
				value( pattern, (SqmExpression) searchString ),
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
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression) {
		return new SqmInListPredicate<>( (SqmExpression<T>) expression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		final SqmInListPredicate<T> predicate = new SqmInListPredicate<>( (SqmExpression<T>) expression, this );
		for ( Expression<? extends T> value : values ) {
			predicate.addExpression( (SqmExpression<T>) value );
		}
		return predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, T... values) {
		final SqmExpression<T> sqmExpression = (SqmExpression<T>) expression;
		final SqmInListPredicate<T> predicate = new SqmInListPredicate<>( sqmExpression, this );
		for ( T value : values ) {
			predicate.addExpression(
					new SqmLiteral<>( value, sqmExpression.getNodeType(), this )
			);
		}
		return predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, List<T> values) {
		final SqmExpression<T> sqmExpression = (SqmExpression<T>) expression;
		final SqmInListPredicate<T> predicate = new SqmInListPredicate<>( sqmExpression, this );
		for ( T value : values ) {
			predicate.addExpression(
					new SqmLiteral<>( value, sqmExpression.getNodeType(), this )
			);
		}
		return predicate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, SqmSubQuery<T> subQuery) {
		return new SqmInSubQueryPredicate<>( (SqmExpression<T>) expression, subQuery, this );
	}

	@Override
	public SqmPredicate exists(Subquery<?> subQuery) {
		return new SqmExistsPredicate( (SqmExpression<?>) subQuery, this );
	}

	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapEmpty(JpaExpression<M> mapExpression) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<?>) mapExpression, false, this );
	}

	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapNotEmpty(JpaExpression<M> mapExpression) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<?>) mapExpression, true, this );
	}
}
