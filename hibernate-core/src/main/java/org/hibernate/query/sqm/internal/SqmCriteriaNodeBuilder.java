/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.io.InvalidObjectException;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.function.AvgFunction;
import org.hibernate.dialect.function.SumReturnTypeResolver;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.BindingContext;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCriteriaSelect;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.criteria.ValueHandlingMode;
import org.hibernate.query.criteria.spi.CriteriaBuilderExtension;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmQuery;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.cte.SqmSearchClauseSpecification;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmTreatedRoot;
import org.hibernate.query.sqm.tree.domain.SqmTreatedSingularJoin;
import org.hibernate.query.sqm.tree.expression.*;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.TemporalField;
import jakarta.persistence.metamodel.Bindable;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Arrays.asList;
import static org.hibernate.internal.util.collections.CollectionHelper.determineProperSizing;
import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;
import static org.hibernate.query.sqm.TrimSpec.fromCriteriaTrimSpec;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.classInstantiation;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.listInstantiation;
import static org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation.mapInstantiation;

/**
 * Acts as a JPA {@link jakarta.persistence.criteria.CriteriaBuilder} by
 * using SQM nodes as the JPA Criteria nodes
 *
 * @author Steve Ebersole
 */
public class SqmCriteriaNodeBuilder implements NodeBuilder, Serializable {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SqmCriteriaNodeBuilder.class );

	private final String uuid;
	private final String name;
	private final transient JpaCompliance jpaCompliance;
	private final transient QueryEngine queryEngine;
	private final transient ValueHandlingMode criteriaValueHandlingMode;
	private final transient ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode;
	private final transient BindingContext bindingContext;
	private transient BasicType<Boolean> booleanType;
	private transient BasicType<Integer> integerType;
	private transient BasicType<Long> longType;
	private transient BasicType<Character> characterType;
	private transient BasicType<String> stringType;
	private transient FunctionReturnTypeResolver sumReturnTypeResolver;
	private transient FunctionReturnTypeResolver avgReturnTypeResolver;
	private final transient Map<Class<? extends HibernateCriteriaBuilder>, HibernateCriteriaBuilder> extensions;

	public SqmCriteriaNodeBuilder(
			String uuid, String name,
			QueryEngine queryEngine,
			QueryEngineOptions options,
			BindingContext bindingContext) {
		this.queryEngine = queryEngine;
		this.uuid = uuid;
		this.name = name;
		this.jpaCompliance = options.getJpaCompliance();
		this.criteriaValueHandlingMode = options.getCriteriaValueHandlingMode();
		this.immutableEntityUpdateQueryHandlingMode = options.getImmutableEntityUpdateQueryHandlingMode();
		this.bindingContext = bindingContext;
		this.extensions = loadExtensions();
	}

	private Map<Class<? extends HibernateCriteriaBuilder>, HibernateCriteriaBuilder> loadExtensions() {
		// load registered criteria builder extensions
		final Map<Class<? extends HibernateCriteriaBuilder>, HibernateCriteriaBuilder> extensions = new HashMap<>();
		for ( CriteriaBuilderExtension extension : ServiceLoader.load( CriteriaBuilderExtension.class ) ) {
			extensions.put( extension.getRegistrationKey(), extension.extend( this ) );
		}
		return extensions;
	}

	@Override
	public JpaMetamodel getDomainModel() {
		return bindingContext.getJpaMetamodel();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return bindingContext.getTypeConfiguration();
	}

	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return jpaCompliance.isJpaQueryComplianceEnabled();
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaCompliance;
	}

	@Override @Deprecated
	public ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode() {
		return immutableEntityUpdateQueryHandlingMode;
	}

	@Override
	public boolean allowImmutableEntityUpdate() {
		return immutableEntityUpdateQueryHandlingMode != ImmutableEntityUpdateQueryHandlingMode.EXCEPTION;
	}

	@Override
	public BasicType<Boolean> getBooleanType() {
		final BasicType<Boolean> booleanType = this.booleanType;
		if ( booleanType == null ) {
			return this.booleanType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.BOOLEAN );
		}
		return booleanType;
	}

	@Override
	public BasicType<Integer> getIntegerType() {
		final BasicType<Integer> integerType = this.integerType;
		if ( integerType == null ) {
			return this.integerType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.INTEGER );
		}
		return integerType;
	}

	@Override
	public BasicType<Long> getLongType() {
		final BasicType<Long> longType = this.longType;
		if ( longType == null ) {
			return this.longType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.LONG );
		}
		return longType;
	}

	@Override
	public BasicType<Character> getCharacterType() {
		final BasicType<Character> characterType = this.characterType;
		if ( characterType == null ) {
			return this.characterType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.CHARACTER );
		}
		return characterType;
	}

	public BasicType<String> getStringType() {
		final BasicType<String> stringType = this.stringType;
		if ( stringType == null ) {
			return this.stringType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.STRING );
		}
		return stringType;
	}

	public FunctionReturnTypeResolver getSumReturnTypeResolver() {
		final FunctionReturnTypeResolver resolver = sumReturnTypeResolver;
		if ( resolver == null ) {
			return this.sumReturnTypeResolver = new SumReturnTypeResolver( getTypeConfiguration() );
		}
		return resolver;
	}

	public FunctionReturnTypeResolver getAvgReturnTypeResolver() {
		final FunctionReturnTypeResolver resolver = avgReturnTypeResolver;
		if ( resolver == null ) {
			return this.avgReturnTypeResolver = new AvgFunction.ReturnTypeResolver( getTypeConfiguration() );
		}
		return resolver;
	}

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	@Override
	public JpaMetamodel getJpaMetamodel() {
		return bindingContext.getJpaMetamodel();
	}

	@Override
	public SqmSelectStatement<Object> createQuery() {
		// IMPORTANT: we want to pass null here for the result-type
		// to indicate that we do not know.  this will allow later
		// calls to `SqmSelectStatement#select`, `SqmSelectStatement#multiSelect`,
		// etc. to influence the result type
		return new SqmSelectStatement<>( Object.class, this );
	}

	@Override
	public <T> SqmSelectStatement<T> createQuery(Class<T> resultClass) {
		return new SqmSelectStatement<>( resultClass, this );
	}

	@Override
	public <T> SqmSelectStatement<T> createQuery(String hql, Class<T> resultClass) {
		final SqmStatement<T> statement =
				queryEngine.getHqlTranslator().translate( hql, resultClass );
		if ( statement instanceof SqmSelectStatement ) {
			return new SqmSelectStatement<>( (SqmSelectStatement<T>) statement );
		}
		else {
			throw new IllegalArgumentException("Not a 'select' statement");
		}
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
	public <T> SqmInsertValuesStatement<T> createCriteriaInsertValues(Class<T> targetEntity) {
		return new SqmInsertValuesStatement<>( targetEntity, this );
	}

	@Override
	public <T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity) {
		return new SqmInsertSelectStatement<>( targetEntity, this );
	}

	@Override
	public SqmValues values(Expression<?>... expressions) {
		return values( asList( expressions ) );
	}

	@Override
	public SqmValues values(List<? extends Expression<?>> expressions) {
		//noinspection unchecked
		return new SqmValues( (List<SqmExpression<?>>) expressions );
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

	@Override
	public <T> JpaCriteriaSelect<T> union(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.UNION, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.UNION, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Override
	public <T> JpaSubQuery<T> union(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return setOperation( all ? SetOperator.UNION_ALL : SetOperator.UNION, query1, queries );
	}

	@Override
	public <T> CriteriaSelect<T> unionAll(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.UNION_ALL, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.UNION_ALL, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Override
	public <T> JpaSubQuery<T> intersect(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return setOperation( all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT, query1, queries );
	}

	@Override
	public <T> CriteriaSelect<T> except(CriteriaSelect<T> left, CriteriaSelect<?> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.EXCEPT, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Override
	public <T> CriteriaSelect<T> exceptAll(CriteriaSelect<T> left, CriteriaSelect<?> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT_ALL, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.EXCEPT_ALL, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Override
	public <T> JpaSubQuery<T> except(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return setOperation( all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT, query1, queries );
	}

	@SuppressWarnings("unchecked")
	private <T> JpaCriteriaQuery<T> setOperation(
			SetOperator operator,
			CriteriaQuery<? extends T> criteriaQuery,
			CriteriaQuery<?>... queries) {
		final Class<T> resultType = (Class<T>) criteriaQuery.getResultType();
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( queries.length + 1 );
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>();
		final SqmSelectStatement<T> selectStatement = (SqmSelectStatement<T>) criteriaQuery;
		collectQueryPartsAndCtes( selectStatement, queryParts, cteStatements );
		for ( CriteriaQuery<?> query : queries ) {
			if ( query.getResultType() != resultType ) {
				throw new IllegalArgumentException( "Result type of all operands must match" );
			}
			collectQueryPartsAndCtes( (SqmSelectQuery<T>) query, queryParts, cteStatements );
		}
		return new SqmSelectStatement<>(
				new SqmQueryGroup<>( this, operator, queryParts ),
				resultType,
				cteStatements,
				selectStatement.getQuerySource(),
				this
		);
	}

	@SuppressWarnings("unchecked")
	private <T> JpaSubQuery<T> setOperation(
			SetOperator operator,
			Subquery<? extends T> subquery,
			Subquery<?>... queries) {
		final Class<T> resultType = (Class<T>) subquery.getResultType();
		final SqmQuery<T> parent = (SqmQuery<T>) subquery.getParent();
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( queries.length + 1 );
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>();
		collectQueryPartsAndCtes( (SqmSelectQuery<T>) subquery, queryParts, cteStatements );
		for ( Subquery<?> query : queries ) {
			if ( query.getResultType() != resultType ) {
				throw new IllegalArgumentException( "Result type of all operands must match" );
			}
			if ( query.getParent() != parent ) {
				throw new IllegalArgumentException( "Subquery parent of all operands must match" );
			}
			collectQueryPartsAndCtes( (SqmSelectQuery<T>) query, queryParts, cteStatements );
		}
		return new SqmSubQuery<>(
				parent,
				new SqmQueryGroup<>( this, operator, queryParts ),
				resultType,
				cteStatements,
				this
		);
	}

	private <T> void collectQueryPartsAndCtes(
			SqmSelectQuery<T> query,
			List<SqmQueryPart<T>> queryParts,
			Map<String, SqmCteStatement<?>> cteStatements) {
		queryParts.add( query.getQueryPart() );
		for ( SqmCteStatement<?> cteStatement : query.getCteStatements() ) {
			final String name = cteStatement.getCteTable().getCteName();
			final SqmCteStatement<?> old = cteStatements.put( name, cteStatement );
			if ( old != null && old != cteStatement ) {
				throw new IllegalArgumentException(
						String.format( "Different CTE with same name [%s] found in different set operands!", name )
				);
			}
		}
	}

	@Override
	public <X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType) {
		return cast( expression, castTarget( castTargetJavaType ) );
	}

	@Override
	public <X, T> SqmExpression<X> cast(JpaExpression<T> expression, JpaCastTarget<X> castTarget) {
		final SqmCastTarget<X> sqmCastTarget = (SqmCastTarget<X>) castTarget;
		return getFunctionDescriptor( "cast" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) expression, sqmCastTarget ),
				sqmCastTarget.getType(),
				queryEngine
		);
	}

	@Override
	public <X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType) {
		return castTarget( castTargetJavaType, null, null, null );
	}

	@Override
	public <X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType, long length) {
		return castTarget( castTargetJavaType, length, null, null );
	}

	@Override
	public <X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType, int precision, int scale) {
		return castTarget( castTargetJavaType, null, precision, scale );
	}

	private <X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType, @Nullable Long length, @Nullable Integer precision, @Nullable Integer scale) {
		final BasicType<X> type = getTypeConfiguration().standardBasicTypeForJavaType( castTargetJavaType );
		return new SqmCastTarget<>( type, length, precision, scale, this );
	}

	@Override
	public SqmPredicate wrap(Expression<Boolean> expression) {
		return expression instanceof SqmPredicate predicate
				? predicate
				: new SqmBooleanExpressionPredicate( (SqmExpression<Boolean>) expression, this );
	}

	@Override
	@SafeVarargs
	public final SqmPredicate wrap(Expression<Boolean>... expressions) {
		if ( expressions.length == 1 ) {
			return wrap( expressions[0] );
		}

		final List<SqmPredicate> predicates = new ArrayList<>( expressions.length );
		for ( Expression<Boolean> expression : expressions ) {
			predicates.add( wrap( expression ) );
		}
		return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
	}

	@Override @SuppressWarnings("unchecked")
	public <T extends HibernateCriteriaBuilder> T unwrap(Class<T> clazz) {
		return (T) extensions.get( clazz );
	}

	@Override
	public <P, F> SqmExpression<F> fk(Path<P> path) {
		final SqmPath<P> sqmPath = (SqmPath<P>) path;
		final SqmPathSource<?> toOneReference = sqmPath.getReferencedPathSource();
		final boolean validToOneRef =
				toOneReference.getBindableType() == Bindable.BindableType.SINGULAR_ATTRIBUTE
						&& toOneReference instanceof EntitySqmPathSource;
		if ( !validToOneRef ) {
			throw new FunctionArgumentException(
					String.format(
							Locale.ROOT,
							"Argument '%s' of 'fk()' function is not a single-valued association",
							sqmPath.getNavigablePath()
					)
			);
		}

		return new SqmFkExpression<>( sqmPath );
	}

	@Override
	public <X, T extends X> SqmPath<T> treat(Path<X> path, Class<T> type) {
		return ( (SqmPath<X>) path ).treatAs( type );
	}

	@Override
	public <X, T extends X> SqmRoot<T> treat(Root<X> root, Class<T> type) {
		//noinspection unchecked
		return (SqmTreatedRoot) ( (SqmRoot<X>) root ).treatAs( type );
	}

	@Override
	public <T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right) {
		return createUnionSet( SetOperator.UNION, left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right) {
		return createUnionSet( SetOperator.UNION_ALL, left, right );
	}

	@Override
	public <T> CriteriaSelect<T> intersect(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.INTERSECT, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.INTERSECT, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Override
	public <T> CriteriaSelect<T> intersectAll(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.INTERSECT_ALL, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.INTERSECT_ALL, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> JpaCriteriaQuery<T> createUnionSet(
			SetOperator operator,
			CriteriaQuery<? extends T> left,
			CriteriaQuery<? extends T> right) {
		assert operator == SetOperator.UNION || operator == SetOperator.UNION_ALL;
		final SqmSelectStatement<? extends T> leftSqm = (SqmSelectStatement<? extends T>) left;
		final SqmSelectStatement<? extends T> rightSqm = (SqmSelectStatement<? extends T>) right;

		// SqmQueryGroup is the UNION ALL between the two
		final SqmQueryGroup sqmQueryGroup = new SqmQueryGroup(
				leftSqm.nodeBuilder(),
				operator,
				List.of( leftSqm.getQueryPart(), rightSqm.getQueryPart() )
		);

		final SqmSelectStatement sqmSelectStatement = new SqmSelectStatement<>(
				leftSqm.getResultType(),
				SqmQuerySource.CRITERIA,
				leftSqm.nodeBuilder()
		);
		sqmSelectStatement.setQueryPart( sqmQueryGroup );
		return sqmSelectStatement;
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right) {
		return createIntersectSet( SetOperator.INTERSECT, left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right) {
		return createIntersectSet( SetOperator.INTERSECT_ALL, left, right );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> JpaCriteriaQuery<T> createIntersectSet(
			SetOperator operator,
			CriteriaQuery<? super T> left,
			CriteriaQuery<? super T> right) {
		assert operator == SetOperator.INTERSECT || operator == SetOperator.INTERSECT_ALL;
		final SqmSelectStatement<? extends T> leftSqm = (SqmSelectStatement<? extends T>) left;
		final SqmSelectStatement<? extends T> rightSqm = (SqmSelectStatement<? extends T>) right;

		// SqmQueryGroup is the UNION ALL between the two
		final SqmQueryGroup sqmQueryGroup = new SqmQueryGroup(
				leftSqm.nodeBuilder(),
				operator,
				List.of( leftSqm.getQueryPart(), rightSqm.getQueryPart() )
		);

		final SqmSelectStatement sqmSelectStatement = new SqmSelectStatement<>(
				leftSqm.getResultType(),
				SqmQuerySource.CRITERIA,
				leftSqm.nodeBuilder()
		);
		sqmSelectStatement.setQueryPart( sqmQueryGroup );
		return sqmSelectStatement;
	}

	@Override
	public <T> JpaCriteriaQuery<T> except(CriteriaQuery<T> left, CriteriaQuery<?> right) {
		return createExceptSet( SetOperator.EXCEPT, left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<T> left, CriteriaQuery<?> right) {
		return createExceptSet( SetOperator.EXCEPT_ALL, left, right );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> JpaCriteriaQuery<T> createExceptSet(
			SetOperator operator,
			CriteriaQuery<T> left,
			CriteriaQuery<?> right) {
		assert operator == SetOperator.EXCEPT || operator == SetOperator.EXCEPT_ALL;
		final SqmSelectStatement<? extends T> leftSqm = (SqmSelectStatement<? extends T>) left;
		final SqmSelectStatement<? extends T> rightSqm = (SqmSelectStatement<? extends T>) right;

		// SqmQueryGroup is the UNION ALL between the two
		final SqmQueryGroup sqmQueryGroup = new SqmQueryGroup(
				leftSqm.nodeBuilder(),
				operator,
				List.of( leftSqm.getQueryPart(), rightSqm.getQueryPart() )
		);

		final SqmSelectStatement sqmSelectStatement = new SqmSelectStatement<>(
				leftSqm.getResultType(),
				SqmQuerySource.CRITERIA,
				leftSqm.nodeBuilder()
		);
		sqmSelectStatement.setQueryPart( sqmQueryGroup );
		return sqmSelectStatement;
	}

	@Override
	public <X, T, V extends T> SqmSingularJoin<X, V> treat(Join<X, T> join, Class<V> type) {
		return (SqmTreatedSingularJoin<X,T,V>) ( (SqmSingularJoin<X, T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmBagJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return ( (SqmBagJoin<X, T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return ( (SqmSetJoin<X, T>) join ).treatAs( type );
	}

	@Override
	public <X, T, E extends T> SqmListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return ( (SqmListJoin<X, T>) join ).treatAs( type );
	}

	@Override
	public <X, K, T, V extends T> SqmMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		return ( (SqmMapJoin<X, K, T>) join ).treatAs( type );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression, SortDirection sortOrder, Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public SqmSortSpecification sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			boolean ignoreCase) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression, SortDirection sortOrder) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder );
	}

	@Override
	public SqmSortSpecification sort(JpaExpression<?> sortExpression) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression );
	}

	@Override
	public SqmSortSpecification asc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortDirection.ASCENDING );
	}

	@Override
	public SqmSortSpecification desc(Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortDirection.DESCENDING );
	}

	@Override
	public Order asc(Expression<?> expression, Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) expression, SortDirection.ASCENDING, NullPrecedence.fromJpaValue( nullPrecedence ) );
	}

	@Override
	public Order desc(Expression<?> expression, Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) expression, SortDirection.DESCENDING, NullPrecedence.fromJpaValue( nullPrecedence ) );
	}

	@Override
	public JpaOrder asc(Expression<?> x, boolean nullsFirst) {
		return new SqmSortSpecification(
				(SqmExpression<?>) x,
				SortDirection.ASCENDING,
				nullsFirst ? NullPrecedence.FIRST : NullPrecedence.LAST
		);
	}

	@Override
	public JpaOrder desc(Expression<?> x, boolean nullsFirst) {
		return new SqmSortSpecification(
				(SqmExpression<?>) x,
				SortDirection.DESCENDING,
				nullsFirst ? NullPrecedence.FIRST : NullPrecedence.LAST
		);
	}

	@Override
	public JpaSearchOrder search(JpaCteCriteriaAttribute sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public JpaSearchOrder search(JpaCteCriteriaAttribute sortExpression, SortDirection sortOrder) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, sortOrder, NullPrecedence.NONE );
	}

	@Override
	public JpaSearchOrder search(JpaCteCriteriaAttribute sortExpression) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, SortDirection.ASCENDING, NullPrecedence.NONE );
	}

	@Override
	public JpaSearchOrder asc(JpaCteCriteriaAttribute x) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) x, SortDirection.ASCENDING, NullPrecedence.NONE );
	}

	@Override
	public JpaSearchOrder desc(JpaCteCriteriaAttribute x) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) x, SortDirection.DESCENDING, NullPrecedence.NONE );
	}

	@Override
	public JpaSearchOrder asc(JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return new SqmSearchClauseSpecification(
				(SqmCteTableColumn) x,
				SortDirection.ASCENDING,
				nullsFirst ? NullPrecedence.FIRST : NullPrecedence.LAST
		);
	}

	@Override
	public JpaSearchOrder desc(JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return new SqmSearchClauseSpecification(
				(SqmCteTableColumn) x,
				SortDirection.DESCENDING,
				nullsFirst ? NullPrecedence.FIRST : NullPrecedence.LAST
		);
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(Selection<?>... selections) {
		return tuple( asList( selections ) );
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new SqmJpaCompoundSelection<>(
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList(),
				getTypeConfiguration().getJavaTypeRegistry().getDescriptor( Tuple.class ),
				this
		);
	}

	@Override @Deprecated(since = "7", forRemoval = true)
	public <R> SqmTuple<R> tuple(Class<R> tupleType, SqmExpression<?>... expressions) {
		return tuple( tupleType, asList( expressions ) );
	}

	@Override @Deprecated(since = "7", forRemoval = true)
	public <R> SqmTuple<R> tuple(SqmExpressible<R> tupleType, SqmExpression<?>... expressions) {
		return tuple( tupleType, asList( expressions ) );
	}

	@Override @Deprecated(since = "7", forRemoval = true)
	public <R> SqmTuple<R> tuple(Class<R> tupleType, List<? extends SqmExpression<?>> expressions) {
		@SuppressWarnings("unchecked")
		final SqmExpressible<R> expressibleType =
				tupleType == null || tupleType == Object[].class
						? (SqmDomainType<R>) getTypeConfiguration().resolveTupleType( expressions )
						: (SqmEmbeddableDomainType<R>) getDomainModel().embeddable( tupleType );
		return tuple( expressibleType, expressions );
	}

	@Override @Deprecated(since = "7", forRemoval = true)
	public <R> SqmTuple<R> tuple(SqmExpressible<R> tupleType, List<? extends SqmExpression<?>> sqmExpressions) {
		if ( tupleType == null ) {
			//noinspection unchecked
			tupleType = (SqmDomainType<R>) getTypeConfiguration().resolveTupleType( sqmExpressions );
		}
		return new SqmTuple<>( new ArrayList<>( sqmExpressions ), tupleType, this );
	}

	@Override
	public JpaCompoundSelection<Object[]> array(Selection<?>... selections) {
		return array( Object[].class,
				Arrays.stream( selections ).map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Override
	public JpaCompoundSelection<Object[]> array(List<Selection<?>> selections) {
		return arrayInternal( Object[].class,
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, Selection<?>... selections) {
		return arrayInternal( resultClass,
				Arrays.stream( selections ).map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, List<? extends Selection<?>> selections) {
		return arrayInternal( resultClass,
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	public <Y> JpaCompoundSelection<Y> arrayInternal(Class<Y> resultClass, List<? extends SqmSelectableNode<?>> selections) {
		checkMultiselect( selections );
		final JavaType<Y> javaType = getTypeConfiguration().getJavaTypeRegistry().getDescriptor( resultClass );
		return new SqmJpaCompoundSelection<>( selections, javaType, this );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>... arguments) {
		return constructInternal( resultClass,
				Arrays.stream( arguments ).map( arg -> (SqmSelectableNode<?>) arg ).toList() );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends Selection<?>> arguments) {
		return constructInternal( resultClass,
				arguments.stream().map( arg -> (SqmSelectableNode<?>) arg ).toList() );
	}

	@SuppressWarnings("unchecked")
	private <Y> JpaCompoundSelection<Y> constructInternal(Class<Y> resultClass, List<? extends SqmSelectableNode<?>> arguments) {
		checkMultiselect( arguments );
		if ( List.class.equals( resultClass ) ) {
			return (SqmDynamicInstantiation<Y>) listInstantiation( arguments, this );
		}
		else if ( Map.class.equals( resultClass ) ) {
			return (SqmDynamicInstantiation<Y>) mapInstantiation( arguments, this );
		}
		else {
			return classInstantiation( resultClass, arguments, this );
		}
	}

	/**
	 * Check the arguments of {@link jakarta.persistence.criteria.CriteriaBuilder#array},
	 * {@link jakarta.persistence.criteria.CriteriaBuilder#construct}, or
	 * {@link jakarta.persistence.criteria.CriteriaBuilder#tuple}.
	 *
	 * @param selections The selection varargs to check
	 *
	 * @throws IllegalArgumentException If the selection items are not valid per
	 *         according to {@linkplain CriteriaQuery#multiselect this documentation}.
	 *         <i>&quot;An argument to the multiselect method must not be a tuple-
	 *         or array-valued compound selection item.&quot;</i>
	 */
	private void checkMultiselect(List<? extends Selection<?>> selections) {
		final HashSet<String> aliases = new HashSet<>( determineProperSizing( selections.size() ) );
		for ( Selection<?> selection : selections ) {
			if ( selection.isCompoundSelection() ) {
				final Class<?> javaType = selection.getJavaType();
				if ( javaType.isArray() ) {
					throw new IllegalArgumentException(
							"Selection item in a multi-select cannot contain compound array-valued elements"
					);
				}
				if ( Tuple.class.isAssignableFrom( javaType ) ) {
					throw new IllegalArgumentException(
							"Selection item in a multi-select cannot contain compound tuple-valued elements"
					);
				}
			}
			final String alias = selection.getAlias();
			if ( StringHelper.isNotEmpty( alias ) && !aliases.add( alias ) ) {
				throw new IllegalArgumentException( "Multi-select expressions have duplicate alias '" + alias + "'" );
			}
		}
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument) {
		return getFunctionDescriptor( "avg" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N extends Number> SqmExpression<N> sum(Expression<N> argument) {
		final SqmTypedNode<N> typedNode = (SqmTypedNode<N>) argument;
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				typedNode,
				(ReturnableType<N>) typedNode.getExpressible().getSqmType(),
				queryEngine
		);
	}

	@Override
	public SqmExpression<Long> sumAsLong(Expression<Integer> argument) {
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Double> sumAsDouble(Expression<Float> argument) {
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> max(Expression<N> argument) {
		return getFunctionDescriptor( "max" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> min(Expression<N> argument) {
		return getFunctionDescriptor( "min" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> greatest(Expression<X> argument) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "max" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine);
	}

	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> least(Expression<X> argument) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "min" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine);
	}

	@Override
	public SqmExpression<Long> count(Expression<?> argument) {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Long> countDistinct(Expression<?> argument) {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				new SqmDistinct<>( (SqmExpression<?>) argument, this ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Long> count() {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				new SqmStar( this ),
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Integer> sign(Expression<? extends Number> x) {
		return getFunctionDescriptor( "sign" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public <N extends Number> JpaExpression<N> ceiling(Expression<N> x) {
		return getFunctionDescriptor( "ceiling" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public <N extends Number> JpaExpression<N> floor(Expression<N> x) {
		return getFunctionDescriptor( "floor" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Double> exp(Expression<? extends Number> x) {
		return getFunctionDescriptor( "exp" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Double> ln(Expression<? extends Number> x) {
		return getFunctionDescriptor( "ln" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Double> power(Expression<? extends Number> x, Expression<? extends Number> y) {
		return getFunctionDescriptor( "power" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, (SqmExpression<?>) y),
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Double> power(Expression<? extends Number> x, Number y) {
		return getFunctionDescriptor( "power" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( y ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T extends Number> JpaExpression<T> round(Expression<T> x, Integer n) {
		return getFunctionDescriptor( "round" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( n ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T extends Number> JpaExpression<T> truncate(Expression<T> x, Integer n) {
		return getFunctionDescriptor( "truncate" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( n ) ),
				null,
				queryEngine
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
		return getFunctionDescriptor( "abs" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public JpaExpression<Duration> duration(long magnitude, TemporalUnit unit) {
		return new SqmToDuration<>(
				literal( magnitude ),
				new SqmDurationUnit<>( unit, getLongType(), this ),
				getTypeConfiguration().standardBasicTypeForJavaType( Duration.class ),
				this
		);
	}

	@Override
	public JpaExpression<Long> durationByUnit(TemporalUnit unit, Expression<Duration> duration) {
		return new SqmByUnit(
				new SqmDurationUnit<>( unit, getLongType(), this ),
				(SqmExpression<Duration>) duration,
				getLongType(),
				this
		);
	}

	@Override
	public JpaExpression<Duration> durationSum(Expression<Duration> x, Expression<Duration> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<Duration>) x, (SqmExpression<Duration>) y );
	}

	@Override
	public JpaExpression<Duration> durationSum(Expression<Duration> x, Duration y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<Duration>) x, value( y ) );
	}

	@Override
	public JpaExpression<Duration> durationDiff(Expression<Duration> x, Expression<Duration> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<Duration>) x, (SqmExpression<Duration>) y );
	}

	@Override
	public JpaExpression<Duration> durationDiff(Expression<Duration> x, Duration y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<Duration>) x, value( y ) );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends Number>) number, (SqmExpression<Duration>) duration );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Number number, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				value( number ), (SqmExpression<Duration>) duration );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends Number>) number, value( duration ) );
	}

	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, Expression<T> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) x, (SqmExpression<T>) y );
	}

	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, T y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) x, value( y ) );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<T>) datetime, (SqmExpression<Duration>) duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<T>) datetime, value( duration ) );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(T datetime, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				value( datetime ), (SqmExpression<Duration>) duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) datetime, (SqmExpression<Duration>) duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) datetime, value( duration ) );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(T datetime, Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				value( datetime ), (SqmExpression<Duration>) duration );
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<? extends N>) x, (SqmExpression<? extends N>) y );
	}

	private <N> SqmExpression<N> createSqmArithmeticNode(
			BinaryArithmeticOperator operator,
			SqmExpression<?> leftHandExpression,
			SqmExpression<?> rightHandExpression) {
		final SqmExpressible<?> arithmeticType =
				getTypeConfiguration()
						.resolveArithmeticType(
								leftHandExpression.getNodeType(),
								rightHandExpression.getNodeType(),
								operator
						);
		//noinspection unchecked
		final SqmExpressible<N> castType =
				(SqmExpressible<N>) arithmeticType;
		return new SqmBinaryArithmetic<>(
				operator,
				leftHandExpression,
				rightHandExpression,
				castType,
				this
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> sum(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends N>) x,
				(SqmExpression<? extends N>) y
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> prod(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<? extends N>) x,
				(SqmExpression<? extends N>) y
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(Expression<? extends N> x, N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Override
	public <N extends Number> SqmExpression<N> diff(N x, Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Override
	public SqmExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression<? extends Number>) x,
				(SqmExpression<? extends Number>) y
		);
	}

	@Override
	public SqmExpression<Number> quot(Expression<? extends Number> x, Number y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression<? extends Number>) x,
				value( y )
		);
	}

	@Override
	public SqmExpression<Number> quot(Number x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				value( x ),
				(SqmExpression<? extends Number>) y
		);
	}

	@Override
	public SqmExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression<Integer>) x,
				(SqmExpression<Integer>) y
		);
	}

	@Override
	public SqmExpression<Integer> mod(Expression<Integer> x, Integer y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression<Integer>) x,
				value( y )
		);
	}

	@Override
	public SqmExpression<Integer> mod(Integer x, Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				value( x ),
				(SqmExpression<Integer>) y
		);
	}

	@Override
	public SqmExpression<Double> sqrt(Expression<? extends Number> x) {
		return getFunctionDescriptor( "sqrt" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Long> toLong(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asLong();
	}

	@Override
	public SqmExpression<Integer> toInteger(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asInteger();
	}

	@Override
	public SqmExpression<Float> toFloat(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asFloat();
	}

	@Override
	public SqmExpression<Double> toDouble(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asDouble();
	}

	@Override
	public SqmExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigDecimal();
	}

	@Override
	public SqmExpression<BigInteger> toBigInteger(Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigInteger();
	}

	@Override
	public SqmExpression<String> toString(Expression<Character> character) {
		return ( (SqmExpression<?>) character ).asString();
	}

	public <T> SqmLiteral<T> literal(T value, SqmExpression<? extends T> typeInferenceSource) {
		return value == null
				? new SqmLiteralNull<T>( this )
				: createLiteral( value, resolveInferredType( value, typeInferenceSource ) );
	}

	private <T> SqmLiteral<T> createLiteral(T value, SqmExpressible<T> expressible) {
		if ( expressible.getExpressibleJavaType().isInstance( value ) ) {
			return new SqmLiteral<>( value, expressible, this );
		}
		else {
			// Just like in HQL, we allow coercion of literal values to the inferred type
			final T coercedValue =
					expressible.getExpressibleJavaType()
							.coerce( value, this::getTypeConfiguration );
			// ignore typeInferenceSource and fallback the value type
			return expressible.getExpressibleJavaType().isInstance( coercedValue )
					? new SqmLiteral<>( coercedValue, expressible, this )
					: literal( value );
		}
	}

	private <T> SqmExpressible<? extends T> resolveInferredType(
			T value, SqmExpression<? extends T> typeInferenceSource) {
		if ( typeInferenceSource != null ) {
			return typeInferenceSource.getNodeType();
		}
		else if ( value == null ) {
			return null;
		}
		else {
			return resolveInferredType( value );
		}
	}

	private <T> BasicType<T> resolveInferredType(T value) {
		final TypeConfiguration typeConfiguration = getTypeConfiguration();
		final Class<T> type = ReflectHelper.getClass( value );
		final BasicType<T> result = typeConfiguration.getBasicTypeForJavaType( type );
		if ( result == null && value instanceof Enum<?> enumValue ) {
			return (BasicType<T>) resolveEnumType( typeConfiguration, enumValue );
		}
		else {
			return result;
		}
	}

	private static <E extends Enum<E>> BasicType<E> resolveEnumType(TypeConfiguration configuration, Enum<E> enumValue) {
		final EnumJavaType<E> javaType = new EnumJavaType<>( ReflectHelper.getClass( enumValue ) );
		final JdbcType jdbcType = javaType.getRecommendedJdbcType( configuration.getCurrentBaseSqlTypeIndicators() );
		return configuration.getBasicTypeRegistry().resolve( javaType, jdbcType );
	}

	@Override
	public <T> SqmLiteral<T> literal(T value) {
		if ( value == null ) {
			if ( jpaCompliance.isJpaQueryComplianceEnabled() ) {
				throw new IllegalArgumentException( "literal value cannot be null" );
			}
			return new SqmLiteralNull<>( this );
		}
		else {
			final BindableType<? super T> valueParamType = getParameterBindType( value );
			final SqmExpressible<? super T> sqmExpressible =
					valueParamType == null ? null : valueParamType.resolveExpressible( this );
			return new SqmLiteral<>( value, sqmExpressible, this );
		}
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return (MappingMetamodelImplementor) bindingContext.getMappingMetamodel();
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
		if ( resultClass.isEnum() ) {
			// No basic types are registered for enum java types, we have to use an untyped null literal in this case
			return new SqmLiteralNull<>( this );
		}
		else {
			final BasicType<T> basicTypeForJavaType = getTypeConfiguration().getBasicTypeForJavaType( resultClass );
			// if there's no basic type, it might be an entity type
			final SqmExpressible<T> sqmExpressible =
					basicTypeForJavaType == null
							? getDomainModel().managedType( resultClass ).resolveExpressible( this )
							: basicTypeForJavaType;
			return new SqmLiteralNull<>( sqmExpressible, this );
		}
	}

	class MultiValueParameterType<T> implements SqmExpressible<T> {
		private final JavaType<T> javaType;

		public MultiValueParameterType(Class<T> type) {
			this.javaType = getTypeConfiguration().getJavaTypeRegistry().getDescriptor( type );
		}

		@Override
		public JavaType<T> getExpressibleJavaType() {
			return javaType;
		}

		@Override
		public Class<T> getBindableJavaType() {
			return javaType.getJavaTypeClass();
		}

		@Override
		public SqmDomainType<T> getSqmType() {
			return null;
		}
	}

	@Override
	public <T> JpaCriteriaParameter<T> parameter(Class<T> paramClass) {
		return parameter( paramClass, null );
	}

	@Override
	public <T> JpaCriteriaParameter<T> parameter(Class<T> paramClass, String name) {
		final BasicType<T> basicType = getTypeConfiguration().getBasicTypeForJavaType( paramClass );
		boolean notBasic = basicType == null;
		final BindableType<T> parameterType =
				notBasic && Collection.class.isAssignableFrom( paramClass )
						// a Collection-valued, multi-valued parameter
						? new MultiValueParameterType<>( (Class<T>) Collection.class )
						: basicType;
		return new JpaCriteriaParameter<>( name, parameterType, notBasic, this );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(Class<T> paramClass) {
		return listParameter( paramClass, null );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(Class<T> paramClass, String name) {
		final BindableType<List<T>> parameterType = new MultiValueParameterType<>( (Class<List<T>>) (Class) List.class );
		return new JpaCriteriaParameter<>( name, parameterType, true, this );
	}

	@Override
	public SqmExpression<String> concat(List<Expression<String>> expressions) {
		//noinspection RedundantCast, unchecked
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) expressions,
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmExpression<String> concat(Expression<String> x, Expression<String> y) {
		final SqmExpression<String> xSqmExpression = (SqmExpression<String>) x;
		final SqmExpression<String> ySqmExpression = (SqmExpression<String>) y;
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmExpression<String> concat(Expression<String> x, String y) {
		final SqmExpression<String> xSqmExpression = (SqmExpression<String>) x;
		final SqmExpression<String> ySqmExpression = value( y, xSqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmExpression<String> concat(String x, Expression<String> y) {
		final SqmExpression<String> ySqmExpression = (SqmExpression<String>) y;
		final SqmExpression<String> xSqmExpression = value( x, ySqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmExpression<String> concat(String x, String y) {
		final SqmExpression<String> xSqmExpression = value( x );
		final SqmExpression<String> ySqmExpression = value( y, xSqmExpression );

		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( xSqmExpression, ySqmExpression ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, Expression<Integer> from) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				(SqmExpression<Integer>) from,
				null
		);
	}

	private SqmFunction<String> createSubstringNode(
			SqmExpression<String> source,
			SqmExpression<Integer> from,
			SqmExpression<Integer> len) {
		return getFunctionDescriptor( "substring" ).generateSqmExpression(
				len == null ? asList( source, from ) : asList( source, from, len ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, int from) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				value( from ),
				null
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, Expression<Integer> from, Expression<Integer> len) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				(SqmExpression<Integer>) from,
				(SqmExpression<Integer>) len
		);
	}

	@Override
	public SqmFunction<String> substring(Expression<String> source, int from, int len) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				value( from ),
				value( len )
		);
	}

	@Override
	public SqmFunction<String> trim(Expression<String> source) {
		return createTrimNode( null, null, (SqmExpression<String>) source );
	}

	private SqmFunction<String> createTrimNode(
			TrimSpec trimSpecification,
			SqmExpression<Character> trimCharacter,
			SqmExpression<String> source) {
		if ( trimSpecification == null ) {
			trimSpecification = TrimSpec.BOTH;
		}
		if ( trimCharacter == null ) {
			trimCharacter = new SqmLiteral<>(
					' ',
					getTypeConfiguration().standardBasicTypeForJavaType( Character.class ),
					this
			);
		}
		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>( 3 );
		arguments.add( new SqmTrimSpecification( trimSpecification, this ) );
		arguments.add( trimCharacter );
		arguments.add( source );

		return getFunctionDescriptor( "trim" ).generateSqmExpression(
				arguments,
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), null, (SqmExpression<String>) source );
	}

	@Override
	public SqmFunction<String> trim(Expression<Character> trimChar, Expression<String> source) {
		return createTrimNode( null, (SqmExpression<Character>) trimChar, (SqmExpression<String>) source );
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, Expression<Character> trimChar, Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), (SqmExpression<Character>) trimChar, (SqmExpression<String>) source );
	}

	@Override
	public SqmFunction<String> trim(char trimChar, Expression<String> source) {
		return createTrimNode( null, literal( trimChar ), (SqmExpression<String>) source );
	}

	@Override
	public SqmFunction<String> trim(Trimspec ts, char trimChar, Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), literal( trimChar ), (SqmExpression<String>) source );
	}

	@Override
	public SqmFunction<String> lower(Expression<String> x) {
		return getFunctionDescriptor( "lower" ).generateSqmExpression(
				(SqmExpression<String>) x,
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> upper(Expression<String> x) {
		return getFunctionDescriptor( "upper" ).generateSqmExpression(
				(SqmExpression<String>) x,
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<Integer> length(Expression<String> argument) {
		return getFunctionDescriptor( "length" ).generateSqmExpression(
				(SqmExpression<String>) argument,
				null,
				getQueryEngine()
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
		final List<SqmTypedNode<?>> arguments;
		if ( startPosition == null ) {
			arguments = asList(
					pattern,
					source
					);
		}
		else {
			arguments = asList(
					pattern,
					source,
					startPosition
			);
		}

		return getFunctionDescriptor("locate").generateSqmExpression(
				arguments,
				null,
				getQueryEngine()
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
		return getFunctionDescriptor("current_date")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public SqmFunction<Timestamp> currentTimestamp() {
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public SqmFunction<Time> currentTime() {
		return getFunctionDescriptor("current_time")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public SqmFunction<Instant> currentInstant() {
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.INSTANT ),
						queryEngine
				);
	}

	@Override
	public JpaExpression<LocalDate> localDate() {
		return getFunctionDescriptor("local_date")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public JpaExpression<LocalDateTime> localDateTime() {
		return getFunctionDescriptor("local_datetime")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public JpaExpression<LocalTime> localTime() {
		return getFunctionDescriptor("local_time")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Override
	public <T> SqmFunction<T> function(String name, Class<T> type, Expression<?>[] args) {
		final BasicType<T> resultType = getTypeConfiguration().standardBasicTypeForJavaType( type );
		return getFunctionTemplate( name, resultType ).generateSqmExpression(
				expressionList( args ),
				resultType,
				getQueryEngine()
		);
	}

	private <T> SqmFunctionDescriptor getFunctionTemplate(String name, BasicType<T> resultType) {
		final SqmFunctionDescriptor functionTemplate = getFunctionDescriptor( name );
		if ( functionTemplate == null ) {
			return new NamedSqmFunctionDescriptor(
					name,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant( resultType ),
					null
			);
		}
		else {
			return functionTemplate;
		}
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
	public <Y> SqmModifiedSubQueryExpression<Y> all(Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.ALL,
				this
		);
	}

	@Override
	public <Y> SqmModifiedSubQueryExpression<Y> some(Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.SOME,
				this
		);
	}

	@Override
	public <Y> SqmModifiedSubQueryExpression<Y> any(Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.ANY,
				this
		);
	}

	@Override
	public <K, M extends Map<K, ?>> SqmExpression<Set<K>> keys(M map) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <K, L extends List<?>> SqmExpression<Set<K>> indexes(L list) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates an expression for the value with the given "type inference" information
	 */
	public <T> SqmExpression<T> value(T value, SqmExpression<? extends T> typeInferenceSource) {
		if ( value instanceof SqmExpression<?> ) {
			//noinspection unchecked
			return (SqmExpression<T>) value;
		}
		return inlineValue( value ) ? literal( value, typeInferenceSource ) : valueParameter( value, typeInferenceSource );
	}

	private <E> SqmExpression<? extends Collection<?>> collectionValue(Collection<E> value, SqmExpression<E> typeInferenceSource) {
		return inlineValue( value ) ? collectionLiteral( value.toArray() ) : collectionValueParameter( value, typeInferenceSource );
	}

	@Override
	public <T> SqmExpression<T> value(T value) {
		if ( value instanceof Duration duration ) {
			final JpaExpression<Duration> expression = duration.getNano() == 0
					? duration( duration.getSeconds(), TemporalUnit.SECOND )
					: duration( duration.getNano() + duration.getSeconds() * 1_000_000_000, TemporalUnit.NANOSECOND );
			//noinspection unchecked
			return (SqmExpression<T>) expression;
		}
		else if ( value instanceof SqmExpression<?> ) {
			//noinspection unchecked
			return (SqmExpression<T>) value;
		}
		else {
			return inlineValue( value ) ? literal( value ) : valueParameter( value );
		}
	}

	private <T> boolean isInstance(BindableType<T> bindableType, T value) {
		if ( bindableType instanceof SqmExpressible<?> expressible ) {
			return expressible.getExpressibleJavaType().isInstance( value );
		}
		else {
			return bindableType.getBindableJavaType().isInstance( value )
				|| bindableType.resolveExpressible( this ).getExpressibleJavaType().isInstance( value );
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> BindableType<T> resolveInferredParameterType(
			T value,
			SqmExpression<? extends T> typeInferenceSource,
			TypeConfiguration typeConfiguration) {

		if ( typeInferenceSource != null ) {
			if ( typeInferenceSource instanceof BindableType ) {
				return (BindableType<T>) typeInferenceSource;
			}
			final SqmExpressible<?> nodeType = getNodeType( typeInferenceSource );
			if ( nodeType != null ) {
				return (BindableType<T>) nodeType;
			}
		}

		return value == null ? null : (BasicType<T>) typeConfiguration.getBasicTypeForJavaType( value.getClass() );
	}

	private static SqmExpressible<?> getNodeType(SqmExpression<?> expression) {
		if ( expression instanceof SqmPath<?> sqmPath ) {
			return sqmPath.getResolvedModel() instanceof SqmSingularPersistentAttribute<?,?> attribute
					? attribute.getSqmPathSource()
					: sqmPath.getResolvedModel();
		}
		else {
			return expression.getNodeType();
		}
	}

	private <T> ValueBindJpaCriteriaParameter<T> valueParameter(T value, SqmExpression<? extends T> typeInferenceSource) {
		final BindableType<T> bindableType =
				resolveInferredParameterType( value, typeInferenceSource, getTypeConfiguration() );
		if ( bindableType == null || isInstance( bindableType, value) ) {
			return new ValueBindJpaCriteriaParameter<>( bindableType, value, this );
		}
		final T coercedValue =
				bindableType.resolveExpressible( this ).getExpressibleJavaType()
						.coerce(value, this::getTypeConfiguration );
		if ( isInstance( bindableType, coercedValue ) ) {
			return new ValueBindJpaCriteriaParameter<>( bindableType, coercedValue, this );
		}
		else {
			// ignore typeInferenceSource and fall back the value type
			return new ValueBindJpaCriteriaParameter<>( getParameterBindType( value ), value, this );
		}
	}

	private <E> ValueBindJpaCriteriaParameter<? extends Collection<E>> collectionValueParameter(Collection<E> value, SqmExpression<E> elementTypeInferenceSource) {
		BindableType<E> bindableType = null;
		if ( elementTypeInferenceSource != null ) {
			if ( elementTypeInferenceSource instanceof BindableType ) {
				//noinspection unchecked
				bindableType = (BindableType<E>) elementTypeInferenceSource;
			}
			else if ( elementTypeInferenceSource.getNodeType() != null ) {
				bindableType = elementTypeInferenceSource.getNodeType();
			}
		}
		DomainType<E> elementType = null;
		if ( bindableType != null ) {
			final SqmExpressible<E> sqmExpressible = bindableType.resolveExpressible( this );
			elementType = sqmExpressible.getSqmType();
		}
		if ( elementType == null ) {
			throw new UnsupportedOperationException( "Can't infer collection type based on element expression: " + elementTypeInferenceSource );
		}
		final BasicType<?> collectionType = DdlTypeHelper.resolveListType( elementType, getTypeConfiguration() );
		//noinspection unchecked
		return new ValueBindJpaCriteriaParameter<>( (BasicType<Collection<E>>) collectionType, value, this );
	}

	private <T> ValueBindJpaCriteriaParameter<T> valueParameter(T value) {
		return new ValueBindJpaCriteriaParameter<>( getParameterBindType( value ), value, this );
	}

	private <T> BindableType<? super T> getParameterBindType(T value) {
		return getMappingMetamodel().resolveParameterBindType( value );
	}

	private <T> boolean inlineValue(T value) {
		return criteriaValueHandlingMode == ValueHandlingMode.INLINE;
//			|| is a literal enum mapped to a PostgreSQL named 'enum' type
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		return value( map.values() );
	}

	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(Expression<C> collection) {
		return new SqmCollectionSize( (SqmPath<C>) collection, this );
	}

	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(C collection) {
		return new SqmLiteral<>( collection.size(), getIntegerType(), this );
	}

	@Override
	public <T> SqmCoalesce<T> coalesce() {
		return new SqmCoalesce<>( this );
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		@SuppressWarnings("unchecked")
		final SqmExpressible<Y> sqmExpressible = (SqmExpressible<Y>) highestPrecedenceType(
				( (SqmExpression<? extends Y>) x ).getExpressible(),
				( (SqmExpression<? extends Y>) y ).getExpressible()
		);
		return new SqmCoalesce<>( sqmExpressible, 2, this ).value(x).value(y);
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y) {
		return coalesce( x, value( y, (SqmExpression<? extends Y>) x ) );
	}

	@Override
	public <Y> SqmExpression<Y> nullif(Expression<Y> x, Expression<?> y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression<Y>) x, (SqmExpression<Y>) y );
	}

	@Override
	public <Y> SqmExpression<Y> nullif(Expression<Y> x, Y y) {
		return createNullifFunctionNode( (SqmExpression<Y>) x, value( y, (SqmExpression<Y>) x ) );
	}

	private <Y> SqmExpression<Y> createNullifFunctionNode(SqmExpression<Y> first, SqmExpression<Y> second) {
		final DomainType<? extends Y> type =
				highestPrecedenceType( first.getExpressible(), second.getExpressible() )
						.getSqmType();
		@SuppressWarnings("unchecked")
		final ReturnableType<Y> resultType = (ReturnableType<Y>) type;
		return getFunctionDescriptor("nullif").generateSqmExpression(
				asList( first, second ),
				resultType,
				getQueryEngine()
		);
	}

	private SqmFunctionDescriptor getFunctionDescriptor(String name) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( name );
	}

	private SqmSetReturningFunctionDescriptor getSetReturningFunctionDescriptor(String name) {
		return queryEngine.getSqmFunctionRegistry().findSetReturningFunctionDescriptor( name );
	}

	@Override
	public <C, R> SqmCaseSimple<C, R> selectCase(Expression<? extends C> expression) {
		//noinspection unchecked
		return new SqmCaseSimple<>( (SqmExpression<C>) expression, this );
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
	public <M extends Map<?, ?>> SqmExpression<Integer> mapSize(M map) {
		return new SqmLiteral<>( map.size(), getIntegerType(), this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SqmPredicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return new SqmJunctionPredicate(
				Predicate.BooleanOperator.AND,
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

		final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
		for ( Predicate expression : restrictions ) {
			predicates.add( (SqmPredicate) expression );
		}
		return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
	}

	@Override
	public SqmPredicate and(List<Predicate> restrictions) {
		if ( restrictions == null || restrictions.isEmpty() ) {
			return conjunction();
		}

		final List<SqmPredicate> predicates = new ArrayList<>( restrictions.size() );
		for ( Predicate expression : restrictions ) {
			predicates.add( (SqmPredicate) expression );
		}
		return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
	}

	@Override
	public SqmPredicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return new SqmJunctionPredicate(
				Predicate.BooleanOperator.OR,
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

		final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
		for ( Predicate expression : restrictions ) {
			predicates.add( (SqmPredicate) expression );
		}
		return new SqmJunctionPredicate( Predicate.BooleanOperator.OR, predicates, this );
	}

	@Override
	public SqmPredicate or(List<Predicate> restrictions) {
		if ( restrictions == null || restrictions.isEmpty() ) {
			return disjunction();
		}

		final List<SqmPredicate> predicates = new ArrayList<>( restrictions.size() );
		for ( Predicate expression : restrictions ) {
			predicates.add( (SqmPredicate) expression );
		}
		return new SqmJunctionPredicate( Predicate.BooleanOperator.OR, predicates, this );
	}

	@Override
	public SqmPredicate not(Expression<Boolean> restriction) {
		return wrap( restriction ).not();
	}

	@Override
	public SqmPredicate conjunction() {
		return new SqmComparisonPredicate(
				new SqmLiteral<>( 1, getIntegerType(), this ),
				ComparisonOperator.EQUAL,
				new SqmLiteral<>( 1, getIntegerType(), this ),
				this
		);
	}

	@Override
	public SqmPredicate disjunction() {
		return new SqmComparisonPredicate(
				new SqmLiteral<>( 1, getIntegerType(), this ),
				ComparisonOperator.NOT_EQUAL,
				new SqmLiteral<>( 1, getIntegerType(), this ),
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
		return new SqmNullnessPredicate( (SqmExpression<?>) x, this );
	}

	@Override
	public SqmPredicate isNotNull(Expression<?> x) {
		return new SqmNullnessPredicate( (SqmExpression<?>) x, this ).not();
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Expression<? extends Y> lower, Expression<? extends Y> upper) {
		return new SqmBetweenPredicate(
				(SqmExpression<? extends Y>) value,
				(SqmExpression<? extends Y>) lower,
				(SqmExpression<? extends Y>) upper,
				false,
				this
		);
	}

	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Y lower, Y upper) {
		final SqmExpression<? extends Y> valueExpression = (SqmExpression<? extends Y>) value;
		final SqmExpression<?> lowerExpr = value( lower, valueExpression );
		final SqmExpression<?> upperExpr = value( upper, valueExpression );
		return new SqmBetweenPredicate(
				valueExpression,
				lowerExpr,
				upperExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_EQUAL,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.DISTINCT_FROM,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_DISTINCT_FROM,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				yExpr,
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
		final SqmExpression<?> yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				yExpr,
				this
		);
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isEmpty(Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<C>) collection, false, this );
	}

	@Override
	public <C extends Collection<?>> SqmPredicate isNotEmpty(Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<C>) collection, true, this );
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(Expression<E> elem, Expression<C> collection) {
		return createSqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, false);
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(E elem, Expression<C> collection) {
		return createSqmMemberOfPredicate( value( elem ), (SqmPath<?>) collection, false);
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(Expression<E> elem, Expression<C> collection) {
		return createSqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, true);
	}

	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(E elem, Expression<C> collection) {
		return createSqmMemberOfPredicate( value( elem ), (SqmPath<?>) collection, true);
	}

	private SqmMemberOfPredicate createSqmMemberOfPredicate(SqmExpression<?> elem, SqmPath<?> collection, boolean negated) {
		if ( collection instanceof SqmPluralValuedSimplePath<?> pluralValuedSimplePath ) {
			return new SqmMemberOfPredicate( elem, pluralValuedSimplePath, negated, this );
		}
		else {
			throw new SemanticException( "Operand of 'member of' operator must be a plural path" );
		}
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern, Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				(SqmExpression<?>) escapeChar,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, Expression<String> pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				literal( escapeChar ),
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern, Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				(SqmExpression<?>) escapeChar,
				this
		);
	}

	@Override
	public SqmPredicate like(Expression<String> searchString, String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				literal( escapeChar ),
				this
		);
	}

	@Override
	public SqmPredicate ilike(Expression<String> searchString, Expression<String> pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				false,
				false,
				this
		);
	}

	@Override
	public SqmPredicate ilike(Expression<String> searchString, String pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				false,
				false,
				this
		);
	}

	@Override
	public SqmPredicate ilike(
			Expression<String> searchString,
			Expression<String> pattern,
			Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				(SqmExpression<?>) escapeChar,
				false,
				false,
				this
		);
	}

	@Override
	public SqmPredicate ilike(Expression<String> searchString, Expression<String> pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				literal( escapeChar ),
				false,
				false,
				this
		);
	}

	@Override
	public SqmPredicate ilike(Expression<String> searchString, String pattern, Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				(SqmExpression<?>) escapeChar,
				false,
				false,
				this
		);
	}

	@Override
	public SqmPredicate ilike(Expression<String> searchString, String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				literal( escapeChar ),
				false,
				false,
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
	public SqmPredicate notIlike(Expression<String> x, Expression<String> pattern) {
		return not( ilike( x, pattern ) );
	}

	@Override
	public SqmPredicate notIlike(Expression<String> x, String pattern) {
		return not( ilike( x, pattern ) );
	}

	@Override
	public SqmPredicate notIlike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notIlike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notIlike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Override
	public SqmPredicate notIlike(Expression<String> x, String pattern, char escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression) {
		return new SqmInListPredicate<>( (SqmExpression<T>) expression, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		final List<SqmExpression<T>> listExpressions = new ArrayList<>( values.length );
		for ( Expression<? extends T> value : values ) {
			listExpressions.add( (SqmExpression<T>) value );
		}
		return new SqmInListPredicate<>( (SqmExpression<T>) expression, listExpressions, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, T... values) {
		final SqmExpression<T> sqmExpression = (SqmExpression<T>) expression;
		final List<SqmExpression<T>> listExpressions = new ArrayList<>( values.length );
		for ( T value : values ) {
			listExpressions.add( value( value, sqmExpression ) );
		}
		return new SqmInListPredicate<>( sqmExpression, listExpressions, this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(Expression<? extends T> expression, Collection<T> values) {
		final SqmExpression<T> sqmExpression = (SqmExpression<T>) expression;
		final List<SqmExpression<T>> listExpressions = new ArrayList<>( values.size() );
		for ( T value : values ) {
			listExpressions.add( value( value, sqmExpression ) );
		}
		return new SqmInListPredicate<>( sqmExpression, listExpressions, this );
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

	/**
	 * Custom serialization hook defined by Java spec.  Used when the node builder is directly deserialized.
	 * Here we resolve the uuid/name read from the stream previously to resolve the SessionFactory
	 * instance to use based on the registrations with the {@link SessionFactoryRegistry}
	 *
	 * @return The resolved node builder to use.
	 *
	 * @throws InvalidObjectException Thrown if we could not resolve the factory by uuid/name.
	 */
	@Serial
	private Object readResolve() throws InvalidObjectException {
		LOG.trace( "Resolving serialized SqmCriteriaNodeBuilder" );
		return locateSessionFactoryOnDeserialization( uuid, name ).getCriteriaBuilder();
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name) throws InvalidObjectException{
		final SessionFactory uuidResult = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
		if ( uuidResult != null ) {
			LOG.debugf( "Resolved SessionFactory by UUID [%s]", uuid );
			return uuidResult;
		}

		// in case we were deserialized in a different JVM, look for an instance with the same name
		// (provided we were given a name)
		if ( name != null ) {
			final SessionFactory namedResult = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( namedResult != null ) {
				LOG.debugf( "Resolved SessionFactory by name [%s]", name );
				return namedResult;
			}
		}

		throw new InvalidObjectException( "Could not find a SessionFactory [uuid=" + uuid + ",name=" + name + "]" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-standard HQL functions

	@Override
	public <T> SqmFunction<T> sql(String pattern, Class<T> type, Expression<?>... arguments) {
		final List<SqmExpression<?>> sqmArguments = new ArrayList<>( expressionList( arguments ) );
		sqmArguments.add( 0, literal( pattern ) );
		return getFunctionDescriptor( "sql" ).generateSqmExpression(
				sqmArguments,
				getTypeConfiguration().standardBasicTypeForJavaType( type ),
				queryEngine
		);
	}

	@Override
	public SqmFunction<String> format(Expression<? extends TemporalAccessor> datetime, String pattern) {
		final SqmFormat sqmFormat = new SqmFormat( pattern, null, this );
		return getFunctionDescriptor( "format" ).generateSqmExpression(
				asList( (SqmExpression<?>) datetime, sqmFormat ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public <N, T extends Temporal> SqmExpression<N> extract(TemporalField<N, T> field, Expression<T> temporal) {
		Class<?> resultType = Integer.class;
		final TemporalUnit temporalUnit;
		switch ( field.toString() ) {
			case "year":
				temporalUnit = TemporalUnit.YEAR;
				break;
			case "quarter":
				temporalUnit = TemporalUnit.QUARTER;
				break;
			case "month":
				temporalUnit = TemporalUnit.MONTH;
				break;
			case "week":
				temporalUnit = TemporalUnit.WEEK;
				break;
			case "day":
				temporalUnit = TemporalUnit.DAY;
				break;
			case "hour":
				temporalUnit = TemporalUnit.HOUR;
				break;
			case "minute":
				temporalUnit = TemporalUnit.MINUTE;
				break;
			case "second":
				temporalUnit = TemporalUnit.SECOND;
				resultType = Double.class;
				break;
			case "date":
				temporalUnit = TemporalUnit.DATE;
				resultType = LocalDate.class;
				break;
			case "time":
				temporalUnit = TemporalUnit.TIME;
				resultType = LocalTime.class;
				break;
			default:
				throw new IllegalArgumentException( "Invalid temporal field [" + field + "]" );
		}
		//noinspection unchecked
		return extract( temporal, temporalUnit, (Class<N>) resultType );
	}

	private <T> SqmFunction<T> extract(
			Expression<? extends TemporalAccessor> datetime,
			TemporalUnit temporalUnit,
			Class<T> type) {
		return getFunctionDescriptor( "extract" ).generateSqmExpression(
				asList(
						new SqmExtractUnit<>(
								temporalUnit,
								getTypeConfiguration().standardBasicTypeForJavaType( type ),
								this
						),
						(SqmTypedNode<?>) datetime
				),
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Integer> year(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.YEAR, Integer.class );
	}

	@Override
	public SqmFunction<Integer> month(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.MONTH, Integer.class );
	}

	@Override
	public SqmFunction<Integer> day(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.DAY, Integer.class );
	}

	@Override
	public SqmFunction<Integer> hour(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.HOUR, Integer.class );
	}

	@Override
	public SqmFunction<Integer> minute(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.MINUTE, Integer.class );
	}

	@Override
	public SqmFunction<Float> second(Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.SECOND, Float.class );
	}

	@Override
	public <T extends TemporalAccessor> SqmFunction<T> truncate(Expression<T> datetime, TemporalUnit temporalUnit) {
		return getFunctionDescriptor( "trunc" ).generateSqmExpression(
				asList(
						(SqmTypedNode<?>) datetime,
						new SqmExtractUnit<>( temporalUnit, getIntegerType(), this )
				),
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<String> overlay(Expression<String> string, String replacement, int start) {
		return overlay( string, replacement, value( start ), null );
	}

	@Override
	public SqmFunction<String> overlay(Expression<String> string, Expression<String> replacement, int start) {
		return overlay( string, replacement, value( start ), null );
	}

	@Override
	public SqmFunction<String> overlay(Expression<String> string, String replacement, Expression<Integer> start) {
		return overlay( string, value( replacement ), start, null );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start) {
		return overlay( string, replacement, start, null );
	}

	@Override
	public SqmFunction<String> overlay(Expression<String> string, String replacement, int start, int length) {
		return overlay( string, value( replacement ), value( start ), value( length ) );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			int start,
			int length) {
		return overlay( string, replacement, value( start ), value( length ) );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			String replacement,
			Expression<Integer> start,
			int length) {
		return overlay( string, value( replacement ), start, value( length ) );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			int length) {
		return overlay( string, replacement, start, value( length ) );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			String replacement,
			int start,
			Expression<Integer> length) {
		return overlay( string, value( replacement ), value( start ), length );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			int start,
			Expression<Integer> length) {
		return overlay( string, replacement, value( start ), length );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			String replacement,
			Expression<Integer> start,
			Expression<Integer> length) {
		return overlay( string, value( replacement ), start, length );
	}

	@Override
	public SqmFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			Expression<Integer> length) {
		SqmExpression<String> sqmString = (SqmExpression<String>) string;
		SqmExpression<String> sqmReplacement = (SqmExpression<String>) replacement;
		SqmExpression<Integer> sqmStart = (SqmExpression<Integer>) start;
		return getFunctionDescriptor( "overlay" ).generateSqmExpression(
				( length == null
						? asList( sqmString, sqmReplacement, sqmStart )
						: asList( sqmString, sqmReplacement, sqmStart, (SqmExpression<Integer>) length ) ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, int length) {
		return pad( null, x, value( length ), null );
	}

	@Override
	public SqmFunction<String> pad(Trimspec ts, Expression<String> x, int length) {
		return pad( ts, x, value( length ), null );
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, Expression<Integer> length) {
		return pad( null, x, length, null );
	}

	@Override
	public SqmFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length) {
		return pad( ts, x, length, null );
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, int length, char padChar) {
		return pad( null, x, value( length ), value( padChar ) );
	}

	@Override
	public SqmFunction<String> pad(Trimspec ts, Expression<String> x, int length, char padChar) {
		return pad( ts, x, value( length ), value( padChar ) );
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, int length, Expression<Character> padChar) {
		return pad( null, x, value( length ), padChar );
	}

	@Override
	public SqmFunction<String> pad(Trimspec ts, Expression<String> x, int length, Expression<Character> padChar) {
		return pad( ts, x, value( length ), padChar );
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, Expression<Integer> length, char padChar) {
		return pad( null, x, length, value( padChar ) );
	}

	@Override
	public SqmFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length, char padChar) {
		return pad( ts, x, length, value( padChar ) );
	}

	@Override
	public SqmFunction<String> pad(Expression<String> x, Expression<Integer> length, Expression<Character> padChar) {
		return pad( null, x, length, padChar );
	}

	@Override
	public SqmFunction<String> pad(
			Trimspec ts,
			Expression<String> x,
			Expression<Integer> length,
			Expression<Character> padChar) {
		SqmExpression<String> source = (SqmExpression<String>) x;
		SqmExpression<Integer> sqmLength = (SqmExpression<Integer>) length;
		SqmTrimSpecification padSpec = new SqmTrimSpecification(
				ts == null ? TrimSpec.TRAILING : fromCriteriaTrimSpec( ts ),
				this
		);
		return getFunctionDescriptor( "pad" ).generateSqmExpression(
				padChar != null
						? asList( source, sqmLength, padSpec, (SqmExpression<Character>) padChar )
						: asList( source, sqmLength, padSpec ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public JpaFunction<String> repeat(Expression<String> x, Expression<Integer> times) {
		return getFunctionDescriptor( "repeat" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) times ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public JpaFunction<String> repeat(Expression<String> x, int times) {
		return repeat( x, value( times ) );
	}

	@Override
	public JpaFunction<String> repeat(String x, Expression<Integer> times) {
		return repeat( value( x), times );
	}

	@Override
	public SqmFunction<String> left(Expression<String> x, int length) {
		return left( x, value( length ) );
	}

	@Override
	public SqmFunction<String> left(Expression<String> x, Expression<Integer> length) {
		return getFunctionDescriptor( "left" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) length ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> right(Expression<String> x, int length) {
		return right( x, value( length ) );
	}

	@Override
	public SqmFunction<String> right(Expression<String> x, Expression<Integer> length) {
		return getFunctionDescriptor( "right" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) length ),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> replace(Expression<String> x, String pattern, String replacement) {
		SqmExpression<String> sqmPattern = value( pattern );
		return replace( x, sqmPattern, value( replacement, sqmPattern ) );
	}

	@Override
	public SqmFunction<String> replace(Expression<String> x, String pattern, Expression<String> replacement) {
		return replace( x, value( pattern ), replacement );
	}

	@Override
	public SqmFunction<String> replace(Expression<String> x, Expression<String> pattern, String replacement) {
		return replace( x, pattern, value( replacement ) );
	}

	@Override
	public SqmFunction<String> replace(
			Expression<String> x,
			Expression<String> pattern,
			Expression<String> replacement) {
		return getFunctionDescriptor( "replace" ).generateSqmExpression(
				asList(
						(SqmExpression<String>) x,
						(SqmExpression<String>) pattern,
						(SqmExpression<String>) replacement
				),
				null,
				getQueryEngine()
		);
	}

	@Override
	public SqmFunction<String> collate(Expression<String> x, String collation) {
		SqmCollation sqmCollation = new SqmCollation( collation, null, this );
		return getFunctionDescriptor( "collate" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, sqmCollation ),
				null,
				getQueryEngine()
		);
	}


	@Override
	public SqmFunction<Double> log10(Expression<? extends Number> x) {
		return getFunctionDescriptor( "log10" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> log(Number b, Expression<? extends Number> x) {
		return log( value( b ), x );
	}

	@Override
	public SqmFunction<Double> log(Expression<? extends Number> b, Expression<? extends Number> x) {
		return getFunctionDescriptor( "log" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) b, (SqmTypedNode<?>) x ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> pi() {
		return getFunctionDescriptor( "pi" ).generateSqmExpression(
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> sin(Expression<? extends Number> x) {
		return getFunctionDescriptor( "sin" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> cos(Expression<? extends Number> x) {
		return getFunctionDescriptor( "cos" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> tan(Expression<? extends Number> x) {
		return getFunctionDescriptor( "tan" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> asin(Expression<? extends Number> x) {
		return getFunctionDescriptor( "asin" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> acos(Expression<? extends Number> x) {
		return getFunctionDescriptor( "acos" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> atan(Expression<? extends Number> x) {
		return getFunctionDescriptor( "atan" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> atan2(Number y, Expression<? extends Number> x) {
		return atan2( value( y ), x );
	}

	@Override
	public SqmFunction<Double> atan2(Expression<? extends Number> y, Number x) {
		return atan2( y, value( x ) );
	}

	@Override
	public SqmFunction<Double> atan2(Expression<? extends Number> y, Expression<? extends Number> x) {
		return getFunctionDescriptor( "atan2" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) y, (SqmTypedNode<?>) x ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> sinh(Expression<? extends Number> x) {
		return getFunctionDescriptor( "sinh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> cosh(Expression<? extends Number> x) {
		return getFunctionDescriptor( "cosh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> tanh(Expression<? extends Number> x) {
		return getFunctionDescriptor( "tanh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> degrees(Expression<? extends Number> x) {
		return getFunctionDescriptor( "degrees" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Override
	public SqmFunction<Double> radians(Expression<? extends Number> x) {
		return getFunctionDescriptor( "radians" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Window functions

	@Override
	public SqmWindow createWindow() {
		return new SqmWindow( this );
	}

	@Override
	public SqmWindowFrame frameUnboundedPreceding() {
		return new SqmWindowFrame( this, FrameKind.UNBOUNDED_PRECEDING );
	}

	@Override
	public SqmWindowFrame frameBetweenPreceding(int offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_PRECEDING, literal( offset ) );
	}

	@Override
	public SqmWindowFrame frameBetweenPreceding(Expression<?> offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_PRECEDING, (SqmExpression<?>) offset );
	}

	@Override
	public SqmWindowFrame frameCurrentRow() {
		return new SqmWindowFrame( this, FrameKind.CURRENT_ROW );
	}

	@Override
	public SqmWindowFrame frameBetweenFollowing(int offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_FOLLOWING, literal( offset ) );
	}

	@Override
	public SqmWindowFrame frameBetweenFollowing(Expression<?> offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_FOLLOWING, (SqmExpression<?>) offset );
	}

	@Override
	public SqmWindowFrame frameUnboundedFollowing() {
		return new SqmWindowFrame( this, FrameKind.UNBOUNDED_FOLLOWING );
	}

	@Override
	public <T> SqmExpression<T> windowFunction(String name, Class<T> type, JpaWindow window, Expression<?>... args) {
		SqmExpression<T> function = getFunctionDescriptor( name ).generateSqmExpression(
				expressionList( args ),
				null,
				queryEngine
		);
		return new SqmOver<>( function, (SqmWindow) window );
	}

	@Override
	public SqmExpression<Long> rowNumber(JpaWindow window) {
		return windowFunction( "row_number", Long.class, window );
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> firstValue(Expression<T> argument, JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "first_value", argument.getJavaType(), window, argument );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> lastValue(Expression<T> argument, JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "last_value", argument.getJavaType(), window, argument );
	}

	@Override
	public <T> SqmExpression<T> nthValue(Expression<T> argument, int n, JpaWindow window) {
		return nthValue( argument, literal( n ), window );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> nthValue(Expression<T> argument, Expression<Integer> n, JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "nth_value", argument.getJavaType(), window, argument, n );
	}

	@Override
	public SqmExpression<Long> rank(JpaWindow window) {
		return windowFunction( "rank", Long.class, window );
	}

	@Override
	public SqmExpression<Long> denseRank(JpaWindow window) {
		return windowFunction( "dense_rank", Long.class, window );
	}

	@Override
	public SqmExpression<Double> percentRank(JpaWindow window) {
		return windowFunction( "percent_rank", Double.class, window );
	}

	@Override
	public SqmExpression<Double> cumeDist(JpaWindow window) {
		return windowFunction( "cume_dist", Double.class, window );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Aggregate functions

	@Override
	public <T> SqmExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			Expression<?>... args) {
		return functionAggregate( name, type, filter, null, args );
	}

	@Override
	public <T> SqmExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaWindow window,
			Expression<?>... args) {
		return functionAggregate( name, type, null, window, args );
	}

	@Override
	public <T> SqmExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args) {
		SqmPredicate sqmFilter = filter != null ? (SqmPredicate) filter : null;
		SqmExpression<T> function = getFunctionDescriptor( name ).generateAggregateSqmExpression(
				expressionList( args ),
				sqmFilter,
				null,
				queryEngine
		);
		if ( window == null ) {
			return function;
		}
		else {
			return new SqmOver<>( function, (SqmWindow) window );
		}
	}

	@Override
	public <N extends Number> SqmExpression<Number> sum(Expression<N> argument, JpaPredicate filter) {
		return sum( argument, filter, null );
	}

	@Override
	public <N extends Number> SqmExpression<Number> sum(Expression<N> argument, JpaWindow window) {
		return sum( argument, null, window );
	}

	@Override
	public <N extends Number> SqmExpression<Number> sum(Expression<N> argument, JpaPredicate filter, JpaWindow window) {
		return functionAggregate( "sum", Number.class, filter, window, argument );
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument, JpaPredicate filter) {
		return avg( argument, filter, null );
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument, JpaWindow window) {
		return avg( argument, null, window );
	}

	@Override
	public <N extends Number> SqmExpression<Double> avg(Expression<N> argument, JpaPredicate filter, JpaWindow window) {
		return functionAggregate( "avg", Double.class, filter, window, argument );
	}

	@Override
	public SqmExpression<Long> count(Expression<?> argument, JpaPredicate filter) {
		return count( argument, filter, null );
	}

	@Override
	public SqmExpression<Long> count(Expression<?> argument, JpaWindow window) {
		return count( argument, null, window );
	}

	@Override
	public SqmExpression<Long> count(Expression<?> argument, JpaPredicate filter, JpaWindow window) {
		return functionAggregate( "count", Long.class, filter, window, argument );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordered-Set Aggregate functions

	@Override
	public <T> SqmExpression<T> functionWithinGroup(String name, Class<T> type, JpaOrder order, Expression<?>... args) {
		return functionWithinGroup( name, type, order, null, null, args );
	}

	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			Expression<?>... args) {
		return functionWithinGroup( name, type, order, filter, null, args );
	}

	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaWindow window,
			Expression<?>... args) {
		return functionWithinGroup( name, type, order, null, window, args );
	}

	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args) {
		SqmOrderByClause withinGroupClause = new SqmOrderByClause();
		if ( order != null ) {
			withinGroupClause.addSortSpecification( (SqmSortSpecification) order );
		}
		SqmPredicate sqmFilter = filter != null ? (SqmPredicate) filter : null;
		SqmExpression<T> function = getFunctionDescriptor( name ).generateOrderedSetAggregateSqmExpression(
				expressionList( args ),
				sqmFilter,
				withinGroupClause,
				null,
				queryEngine
		);
		if ( window == null ) {
			return function;
		}
		else {
			return new SqmOver<>( function, (SqmWindow) window );
		}
	}

	@Override
	public SqmExpression<String> listagg(JpaOrder order, Expression<String> argument, String separator) {
		return listagg( order, null, null, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(JpaOrder order, Expression<String> argument, Expression<String> separator) {
		return listagg( order, null, null, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			Expression<String> argument,
			String separator) {
		return listagg( order, filter, null, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			Expression<String> argument,
			Expression<String> separator) {
		return listagg( order, filter, null, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaWindow window,
			Expression<String> argument,
			String separator) {
		return listagg( order, null, window, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator) {
		return listagg( order, null, window, argument, separator );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			String separator) {
		return listagg( order, filter, window, argument, literal( separator ) );
	}

	@Override
	public SqmExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator) {
		return functionWithinGroup( "listagg", String.class, order, filter, window, argument, separator );
	}

	@Override
	public <T> SqmExpression<T> mode(Expression<T> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		return mode( null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> mode(
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return mode( filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> mode(
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return mode( null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> mode(
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"mode",
				sortExpression.getJavaType(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window
		);
	}

	@Override
	public <T> SqmExpression<T> percentileCont(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileCont( argument, null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileCont( argument, filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileCont( argument, null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"percentile_cont",
				sortExpression.getJavaType(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window,
				argument
		);
	}

	@Override
	public <T> SqmExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileDisc( argument, null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileDisc( argument, filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> SqmExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return percentileDisc( argument, null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"percentile_disc",
				sortExpression.getJavaType(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window,
				argument
		);
	}

	@Override
	public SqmExpression<Long> rank(JpaOrder order, Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, null, null, arguments );
	}

	@Override
	public SqmExpression<Long> rank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, filter, null, arguments );
	}

	@Override
	public SqmExpression<Long> rank(JpaOrder order, JpaWindow window, Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, null, window, arguments );
	}

	@Override
	public SqmExpression<Long> rank(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, filter, window, arguments );
	}

	@Override
	public SqmExpression<Double> percentRank(JpaOrder order, Expression<?>... arguments) {
		return percentRank( order, null, null, arguments );
	}

	@Override
	public SqmExpression<Double> percentRank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments) {
		return percentRank( order, filter, null, arguments );
	}

	@Override
	public SqmExpression<Double> percentRank(JpaOrder order, JpaWindow window, Expression<?>... arguments) {
		return percentRank( order, null, window, arguments );
	}

	@Override
	public SqmExpression<Double> percentRank(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... arguments) {
		return functionWithinGroup( "percent_rank", Double.class, order, filter, window, arguments );
	}

	@Override
	public <T> SqmExpression<T[]> arrayAgg(JpaOrder order, Expression<? extends T> argument) {
		return arrayAgg( order, null, null, argument );
	}

	@Override
	public <T> SqmExpression<T[]> arrayAgg(JpaOrder order, JpaPredicate filter, Expression<? extends T> argument) {
		return arrayAgg( order, filter, null, argument );
	}

	@Override
	public <T> SqmExpression<T[]> arrayAgg(JpaOrder order, JpaWindow window, Expression<? extends T> argument) {
		return arrayAgg( order, null, window, argument );
	}

	@Override
	public <T> SqmExpression<T[]> arrayAgg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<? extends T> argument) {
		return functionWithinGroup( "array_agg", null, order, filter, window, argument );
	}

	@Override
	public <T> SqmExpression<T[]> arrayLiteral(T... elements) {
		return getFunctionDescriptor( "array" ).generateSqmExpression(
				literals( elements ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, T element) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Integer> arrayPosition(
			Expression<T[]> arrayExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<int[]> arrayPositions(
			Expression<T[]> arrayExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, T element) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<List<Integer>> arrayPositionsList(
			Expression<T[]> arrayExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, T element) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Integer> arrayLength(Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_length" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayConcat(
			Expression<T[]> arrayExpression1,
			Expression<T[]> arrayExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, T[] array2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayConcat(T[] array1, Expression<T[]> arrayExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, T element) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayPrepend(Expression<T> elementExpression, Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayPrepend(T element, Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmPredicate arrayContains(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayContains(Expression<T[]> arrayExpression, T element) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayContains(T[] array, Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( value( array ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayContainsNullable(
			Expression<T[]> arrayExpression,
			Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayContainsNullable(Expression<T[]> arrayExpression, T element) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayContainsNullable(T[] array, Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( value( array ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludes(
			Expression<T[]> arrayExpression,
			Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludes(Expression<T[]> arrayExpression, T[] subArray) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( subArray, (SqmExpression<?>) arrayExpression ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludes(T[] array, Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( value( array, (SqmExpression<?>) subArrayExpression ), (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludesNullable(
			Expression<T[]> arrayExpression,
			Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( subArray, (SqmExpression<?>) arrayExpression ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIncludesNullable(T[] array, Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( value( array, (SqmExpression<?>) subArrayExpression ), (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersects(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersects(Expression<T[]> arrayExpression1, T[] array2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersects(T[] array1, Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersectsNullable(
			Expression<T[]> arrayExpression1,
			Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmPredicate arrayIntersectsNullable(T[] array1, Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <T> SqmExpression<T> arrayGet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T> arrayGet(Expression<T[]> arrayExpression, Integer index) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression,
			T element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Integer index,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, T element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ), value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, T element) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayRemoveIndex(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Integer index) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Expression<Integer> lowerIndexExpression,
			Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) lowerIndexExpression, (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Expression<Integer> lowerIndexExpression,
			Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) lowerIndexExpression, value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Integer lowerIndex,
			Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( lowerIndex ), (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Integer lowerIndex,
			Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( lowerIndex ), value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			Expression<T> oldElementExpression,
			Expression<T> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) oldElementExpression, (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			Expression<T> oldElementExpression,
			T newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) oldElementExpression, value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			T oldElement,
			Expression<T> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( oldElement ), (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, T newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( oldElement ), value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayTrim(
			Expression<T[]> arrayExpression,
			Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Integer elementCount) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayFill(
			Expression<T> elementExpression,
			Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayFill(Expression<T> elementExpression, Integer elementCount) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayFill(T element, Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T[]> arrayFill(T element, Integer elementCount) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( value( element ), value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> arrayToString(
			Expression<? extends Object[]> arrayExpression,
			Expression<String> separatorExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) separatorExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> arrayToString(
			Expression<? extends Object[]> arrayExpression,
			String separator) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( separator ) ),
				null,
				queryEngine
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types


	@Override
	public <E, C extends Collection<E>> SqmExpression<C> collectionLiteral(E... elements) {
		return getFunctionDescriptor( "array_list" ).generateSqmExpression(
				literals( elements ),
				null,
				queryEngine
		);
	}

	@Override
	public <E> SqmExpression<Integer> collectionPosition(
			Expression<? extends Collection<? extends E>> collectionExpression,
			E element) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E> SqmExpression<Integer> collectionPosition(
			Expression<? extends Collection<? extends E>> collectionExpression,
			Expression<E> elementExpression) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<int[]> collectionPositions(
			Expression<? extends Collection<? super T>> collectionExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<int[]> collectionPositions(
			Expression<? extends Collection<? super T>> collectionExpression,
			T element) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<List<Integer>> collectionPositionsList(
			Expression<? extends Collection<? super T>> collectionExpression,
			Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<List<Integer>> collectionPositionsList(
			Expression<? extends Collection<? super T>> collectionExpression,
			T element) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Integer> collectionLength(Expression<? extends Collection<?>> collectionExpression) {
		return getFunctionDescriptor( "array_length" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			Expression<C> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			Expression<C> collectionExpression1,
			Collection<? extends E> collection2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			C collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(
			Expression<C> collectionExpression,
			Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(
			Expression<C> collectionExpression,
			E element) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(
			Expression<? extends E> elementExpression,
			Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(
			E element,
			Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E> SqmPredicate collectionContains(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionContains(
			Expression<? extends Collection<E>> collectionExpression,
			E element) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionContains(
			Collection<E> collection,
			Expression<E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( collectionValue( collection, (SqmExpression<E>) elementExpression ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionContainsNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionContainsNullable(
			Expression<? extends Collection<E>> collectionExpression,
			E element) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionContainsNullable(
			Collection<E> collection,
			Expression<E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( collectionValue( collection, (SqmExpression<E>) elementExpression ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludes(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludes(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( subCollection, (SqmExpression<?>) collectionExpression ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludes(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( value( collection, (SqmExpression<?>) subCollectionExpression ), (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( subCollection, (SqmExpression<?>) collectionExpression ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( value( collection, (SqmExpression<?>) subCollectionExpression ), (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersects(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersects(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersects(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Override
	public <E> SqmExpression<E> collectionGet(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E> SqmExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Integer index) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression,
			Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression,
			E element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Integer index,
			Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Integer index,
			E element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ), value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(
			Expression<C> collectionExpression,
			Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(
			Expression<C> collectionExpression,
			E element) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(
			Expression<C> collectionExpression,
			Integer index) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Expression<Integer> lowerIndexExpression,
			Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) lowerIndexExpression, (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Expression<Integer> lowerIndexExpression,
			Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) lowerIndexExpression, value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Integer lowerIndex,
			Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( lowerIndex ), (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Integer lowerIndex,
			Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( lowerIndex ), value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			Expression<? extends E> oldElementExpression,
			Expression<? extends E> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) oldElementExpression, (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			Expression<? extends E> oldElementExpression,
			E newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) oldElementExpression, value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			E oldElement,
			Expression<? extends E> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( oldElement ), (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			E oldElement,
			E newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( oldElement ), value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionTrim(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionTrim(
			Expression<C> collectionExpression,
			Integer index) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(
			Expression<T> elementExpression,
			Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Integer elementCount) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(T element, Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(T element, Integer elementCount) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( value( element ), value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			Expression<String> separatorExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) separatorExpression ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			String separator) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( separator ) ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, String jsonPath) {
		return jsonValue( jsonDocument, value( jsonPath ), null );
	}

	@Override
	public <T> SqmJsonValueExpression<T> jsonValue(
			Expression<?> jsonDocument,
			String jsonPath,
			Class<T> returningType) {
		return jsonValue( jsonDocument, value( jsonPath ), returningType );
	}

	@Override
	public SqmJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return jsonValue( jsonDocument, jsonPath, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmJsonValueExpression<T> jsonValue(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Class<T> returningType) {
		if ( returningType == null ) {
			return (SqmJsonValueExpression<T>) getFunctionDescriptor( "json_value" ).generateSqmExpression(
					asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
					null,
					queryEngine
			);
		}
		else {
			final BasicType<T> type = getTypeConfiguration().standardBasicTypeForJavaType( returningType );
			return (SqmJsonValueExpression<T>) getFunctionDescriptor( "json_value" ).generateSqmExpression(
					asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath, new SqmCastTarget<>( type, this ) ),
					type,
					queryEngine
			);
		}
	}

	@Override
	public SqmJsonQueryExpression jsonQuery(Expression<?> jsonDocument, String jsonPath) {
		return jsonQuery( jsonDocument, value( jsonPath ) );
	}

	@Override
	public SqmJsonQueryExpression jsonQuery(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return (SqmJsonQueryExpression) getFunctionDescriptor( "json_query" ).<String>generateSqmExpression(
				asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmJsonExistsExpression jsonExists(Expression<?> jsonDocument, String jsonPath) {
		return jsonExists( jsonDocument, value( jsonPath ) );
	}

	@Override
	public SqmJsonExistsExpression jsonExists(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return (SqmJsonExistsExpression) getFunctionDescriptor( "json_exists" ).<Boolean>generateSqmExpression(
				asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonArrayWithNulls(Expression<?>... values) {
		final var arguments = new ArrayList<SqmTypedNode<?>>( values.length + 1 );
		for ( Expression<?> expression : values ) {
			arguments.add( (SqmTypedNode<?>) expression );
		}
		arguments.add( SqmJsonNullBehavior.NULL );
		return getFunctionDescriptor( "json_array" ).generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonArray(Expression<?>... values) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_array" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( values ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonArrayAgg(Expression<?> value) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, null, null );
	}

	@Override
	public SqmExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter, JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, (SqmPredicate) filter, orderByClause( orderBy ) );
	}

	@Override
	public SqmExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, (SqmPredicate) filter, null );
	}

	@Override
	public SqmExpression<String> jsonArrayAgg(Expression<?> value, JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, null, orderByClause( orderBy ) );
	}

	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, null, null );
	}

	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter, JpaOrder... orderBy) {
		return jsonArrayAgg(
				(SqmExpression<?>) value,
				SqmJsonNullBehavior.NULL,
				(SqmPredicate) filter,
				orderByClause( orderBy )
		);
	}

	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, (SqmPredicate) filter, null );
	}

	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, null, orderByClause( orderBy ) );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(Expression<?> key, Expression<?> value) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, SqmJsonObjectAggUniqueKeysBehavior.WITH, null );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value) {
		return jsonObjectAgg( key, value, null, SqmJsonObjectAggUniqueKeysBehavior.WITH, null );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, null, null );
	}

	@Override
	public SqmExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value) {
		return jsonObjectAgg( key, value, null, null, null );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(
			Expression<?> key,
			Expression<?> value,
			Predicate filter) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, SqmJsonObjectAggUniqueKeysBehavior.WITH, filter );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value, Predicate filter) {
		return jsonObjectAgg( key, value, null, SqmJsonObjectAggUniqueKeysBehavior.WITH, filter );
	}

	@Override
	public SqmExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value, Predicate filter) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, null, filter );
	}

	@Override
	public SqmExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value, Predicate filter) {
		return jsonObjectAgg( key, value, null, null, filter );
	}

	private SqmExpression<String> jsonObjectAgg(
			Expression<?> key,
			Expression<?> value,
			@Nullable SqmJsonNullBehavior nullBehavior,
			@Nullable SqmJsonObjectAggUniqueKeysBehavior uniqueKeysBehavior,
			@Nullable Predicate filterPredicate) {
		final ArrayList<SqmTypedNode<?>> arguments = new ArrayList<>( 4 );
		arguments.add( (SqmTypedNode<?>) key );
		arguments.add( (SqmTypedNode<?>) value );
		if ( nullBehavior != null ) {
			arguments.add( nullBehavior );
		}
		if ( uniqueKeysBehavior != null ) {
			arguments.add( uniqueKeysBehavior );
		}
		return getFunctionDescriptor( "json_objectagg" ).generateAggregateSqmExpression(
				arguments,
				(SqmPredicate) filterPredicate,
				null,
				queryEngine
		);
	}

	private @Nullable SqmOrderByClause orderByClause(JpaOrder[] orderBy) {
		if ( orderBy.length == 0 ) {
			return null;
		}
		final SqmOrderByClause sqmOrderByClause = new SqmOrderByClause( orderBy.length );
		for ( JpaOrder jpaOrder : orderBy ) {
			sqmOrderByClause.addSortSpecification( (SqmSortSpecification) jpaOrder );
		}
		return sqmOrderByClause;
	}

	private SqmExpression<String> jsonArrayAgg(
			SqmExpression<?> value,
			@Nullable SqmJsonNullBehavior nullBehavior,
			@Nullable SqmPredicate filterPredicate,
			@Nullable SqmOrderByClause orderByClause) {
		return getFunctionDescriptor( "json_arrayagg" ).generateOrderedSetAggregateSqmExpression(
				nullBehavior == null
						? Collections.singletonList( value )
						: asList( value, SqmJsonNullBehavior.NULL ),
				filterPredicate,
				orderByClause,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonObjectWithNulls(Map<?, ? extends Expression<?>> keyValues) {
		final var arguments = keyValuesAsAlternatingList( keyValues );
		arguments.add( SqmJsonNullBehavior.NULL );
		return getFunctionDescriptor( "json_object" ).generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonObject(Map<?, ? extends Expression<?>> keyValues) {
		return getFunctionDescriptor( "json_object" ).generateSqmExpression(
				keyValuesAsAlternatingList( keyValues ),
				null,
				queryEngine
		);
	}

	private ArrayList<SqmTypedNode<?>> keyValuesAsAlternatingList(Map<?, ? extends Expression<?>> keyValues) {
		final var list = new ArrayList<SqmTypedNode<?>>( keyValues.size() );
		for ( Map.Entry<?, ? extends Expression<?>> entry : keyValues.entrySet() ) {
			list.add( value( entry.getKey() ) );
			list.add( (SqmTypedNode<?>) entry.getValue() );
		}
		return list;
	}

	@Override
	public SqmExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return jsonSet( jsonDocument, jsonPath, value( value ) );
	}

	@Override
	public SqmExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Object value) {
		return jsonSet( jsonDocument, value( jsonPath ), value( value ) );
	}

	@Override
	public SqmExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return jsonSet( jsonDocument, value( jsonPath ), value );
	}

	@Override
	public SqmExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_set" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonRemove(Expression<?> jsonDocument, String jsonPath) {
		return jsonRemove( jsonDocument, value( jsonPath ) );
	}

	@Override
	public SqmExpression<String> jsonRemove(Expression<?> jsonDocument, Expression<String> jsonPath) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_remove" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonInsert(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return jsonInsert( jsonDocument, jsonPath, value( value ) );
	}

	@Override
	public SqmExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Object value) {
		return jsonInsert( jsonDocument, value( jsonPath ), value );
	}

	@Override
	public SqmExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return jsonInsert( jsonDocument, value( jsonPath ), value );
	}

	@Override
	public SqmExpression<String> jsonInsert(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_insert" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonReplace(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return jsonReplace( jsonDocument, jsonPath, value( value ) );
	}

	@Override
	public SqmExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Object value) {
		return jsonReplace( jsonDocument, value( jsonPath ), value );
	}

	@Override
	public SqmExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return jsonReplace( jsonDocument, value( jsonPath ), value );
	}

	@Override
	public SqmExpression<String> jsonReplace(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_replace" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> jsonMergepatch(String document, Expression<?> patch) {
		return jsonMergepatch( value( document ), patch );
	}

	@Override
	public SqmExpression<String> jsonMergepatch(Expression<?> document, String patch) {
		return jsonMergepatch( document, value( patch ) );
	}

	@Override
	public SqmExpression<String> jsonMergepatch(Expression<?> document, Expression<?> patch) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_mergepatch" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( document, patch ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmXmlElementExpression xmlelement(String elementName) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( 3 );
		arguments.add( new SqmLiteral<>( elementName, getStringType(), this ) );
		return (SqmXmlElementExpression) getFunctionDescriptor( "xmlelement" ).<String>generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlcomment(String comment) {
		return getFunctionDescriptor( "xmlcomment" ).generateSqmExpression(
				List.of( value( comment ) ),
				null,
				queryEngine
		);
	}

	@Override
	public <T> SqmExpression<T> named(Expression<T> expression, String name) {
		return new SqmNamedExpression<>( (SqmExpression<T>) expression, name );
	}

	@Override
	public SqmExpression<String> xmlforest(Expression<?>... elements) {
		return xmlforest( asList( elements ) );
	}

	@Override
	public SqmExpression<String> xmlforest(List<? extends Expression<?>> elements) {
		final ArrayList<SqmExpression<?>> arguments = new ArrayList<>( elements.size() );
		for ( Expression<?> expression : elements ) {
			if ( expression instanceof SqmNamedExpression<?> ) {
				arguments.add( (SqmNamedExpression<?>) expression );
			}
			else {
				if ( !( expression instanceof SqmPath<?> path ) || !( path.getModel() instanceof PersistentAttribute<?, ?> attribute ) ) {
					throw new SemanticException(
							"Can't use expression '" + expression + " without explicit name in xmlforest function"+
									", because XML element names can only be derived from path expressions."
					);
				}
				arguments.add( new SqmNamedExpression<>( (SqmExpression<?>) expression, attribute.getName() ) );
			}
		}
		return getFunctionDescriptor( "xmlforest" ).generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlconcat(Expression<?>... elements) {
		return xmlconcat( asList( elements ) );
	}

	@Override
	public SqmExpression<String> xmlconcat(List<? extends Expression<?>> elements) {
		return getFunctionDescriptor( "xmlforest" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) elements,
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlpi(String elementName) {
		return getFunctionDescriptor( "xmlpi" ).generateSqmExpression(
				Collections.singletonList( literal( elementName ) ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlpi(String elementName, Expression<String> content) {
		return getFunctionDescriptor( "xmlpi" ).generateSqmExpression(
				asList( literal( elementName ), (SqmTypedNode<?>) content ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlquery(String query, Expression<?> xmlDocument) {
		return xmlquery( value( query ), xmlDocument );
	}

	@Override
	public SqmExpression<String> xmlquery(Expression<String> query, Expression<?> xmlDocument) {
		return getFunctionDescriptor( "xmlquery" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) query, (SqmTypedNode<?>) xmlDocument ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<Boolean> xmlexists(String query, Expression<?> xmlDocument) {
		return xmlexists( value( query ), xmlDocument );
	}

	@Override
	public SqmExpression<Boolean> xmlexists(Expression<String> query, Expression<?> xmlDocument) {
		return getFunctionDescriptor( "xmlexists" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) query, (SqmTypedNode<?>) xmlDocument ),
				null,
				queryEngine
		);
	}

	@Override
	public SqmExpression<String> xmlagg(JpaOrder order, Expression<?> argument) {
		return xmlagg( order, null, null, argument );
	}

	@Override
	public SqmExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, Expression<?> argument) {
		return xmlagg( order, filter, null, argument );
	}

	@Override
	public SqmExpression<String> xmlagg(JpaOrder order, JpaWindow window, Expression<?> argument) {
		return xmlagg( order, null, window, argument );
	}

	@Override
	public SqmExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?> argument) {
		return functionWithinGroup( "xmlagg", String.class, order, filter, window, argument );
	}

	@Override
	public <E> SqmSetReturningFunction<E> setReturningFunction(String name, Expression<?>... args) {
		return getSetReturningFunctionDescriptor( name ).generateSqmExpression(
				expressionList( args ),
				queryEngine
		);
	}

	@Override
	public <E> SqmSetReturningFunction<E> unnestArray(Expression<E[]> array) {
		return getSetReturningFunctionDescriptor( "unnest" ).generateSqmExpression(
				Collections.singletonList( (SqmTypedNode<?>) array ),
				queryEngine
		);
	}

	@Override
	public <E> SqmSetReturningFunction<E> unnestCollection(Expression<? extends Collection<E>> collection) {
		return getSetReturningFunctionDescriptor( "unnest" ).generateSqmExpression(
				Collections.singletonList( (SqmTypedNode<?>) collection ),
				queryEngine
		);
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, Expression<? extends TemporalAmount> step) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop, (SqmTypedNode<?>) step ),
				queryEngine
		);
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, E stop, TemporalAmount step) {
		return generateTimeSeries( value( start ), value( stop ), value( step ) );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, TemporalAmount step) {
		return generateTimeSeries( value( start ), stop, value( step ) );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, TemporalAmount step) {
		return generateTimeSeries( start, value( stop ), value( step ) );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, TemporalAmount step) {
		return generateTimeSeries( start, stop, value( step ) );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, E stop, Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( value( start ), value( stop ), step );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( start, value( stop ), step );
	}

	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( value( start ), stop, step );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, Expression<E> step) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop, (SqmTypedNode<?>) step ),
				queryEngine
		);
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop, E step) {
		return generateSeries( value( start ), value( stop ), value( step ) );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop, Expression<E> step) {
		return generateSeries( value( start ), value( stop ), step );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop, E step) {
		return generateSeries( start, value( stop ), value( step ) );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop, E step) {
		return generateSeries( value( start ), stop, value( step ) );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, E step) {
		return generateSeries( start, stop, value( step ) );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop, Expression<E> step) {
		return generateSeries( start, value( stop ), step );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop, Expression<E> step) {
		return generateSeries( value( start ), stop, step );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop ),
				queryEngine
		);
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop) {
		return generateSeries( start, value( stop ) );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop) {
		return generateSeries( value( start ), stop );
	}

	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop) {
		return generateSeries( value( start ), value( stop ) );
	}

	@Override
	public SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument) {
		return jsonTable( jsonDocument, (Expression<String>) null );
	}

	@Override
	public SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument, String jsonPath) {
		return jsonTable( jsonDocument, value( jsonPath ) );
	}

	@Override
	public SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument, @Nullable Expression<String> jsonPath) {
		return (SqmJsonTableFunction<?>) getSetReturningFunctionDescriptor( "json_table" ).generateSqmExpression(
				jsonPath == null
						? Collections.singletonList( (SqmTypedNode<?>) jsonDocument )
						: asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				queryEngine
		);
	}

	@Override
	public SqmXmlTableFunction<?> xmlTable(String xpath, Expression<?> xmlDocument) {
		return xmlTable( value( xpath ), xmlDocument );
	}

	@Override
	public SqmXmlTableFunction<?> xmlTable(Expression<String> xpath, Expression<?> xmlDocument) {
		return (SqmXmlTableFunction<?>) getSetReturningFunctionDescriptor( "xmltable" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) xpath, (SqmTypedNode<?>) xmlDocument ),
				queryEngine
		);
	}
}
