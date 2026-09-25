package org.hibernate.query.sqm.internal;

import jakarta.annotation.Nullable;
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
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.StatementReference;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.TemporalExpression;
import jakarta.persistence.criteria.TextExpression;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.function.AvgFunction;
import org.hibernate.dialect.function.SumReturnTypeResolver;
import org.hibernate.dialect.function.array.DdlTypeHelper;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.spi.expression.SqmBooleanExpressionWrapper;
import org.hibernate.query.sqm.tree.spi.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.spi.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.spi.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.spi.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.spi.expression.SqmCollation;
import org.hibernate.query.sqm.tree.spi.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.spi.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.spi.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmFormat;
import org.hibernate.query.sqm.tree.spi.expression.SqmFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonExistsExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonNullBehavior;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonObjectAggUniqueKeysBehavior;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonQueryExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonTableFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonValueExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.spi.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNamedExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpressionWrapper;
import org.hibernate.query.sqm.tree.spi.expression.SqmOver;
import org.hibernate.query.sqm.tree.spi.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmStar;
import org.hibernate.query.sqm.tree.spi.expression.SqmTemporalExpressionWrapper;
import org.hibernate.query.sqm.tree.spi.expression.SqmTextExpressionWrapper;
import org.hibernate.query.sqm.tree.spi.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.spi.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.spi.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.spi.expression.SqmWindow;
import org.hibernate.query.sqm.tree.spi.expression.SqmWindowFrame;
import org.hibernate.query.sqm.tree.spi.expression.SqmXmlElementExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmXmlTableFunction;
import org.hibernate.query.sqm.tree.spi.expression.ValueBindJpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.from.SqmJoin;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.type.BindingContext;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.criteria.JpaCastTarget;
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
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryEngineOptions;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmExpressible;
import org.hibernate.query.sqm.spi.SqmQuerySource;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.spi.SqmQuery;
import org.hibernate.query.sqm.tree.spi.SqmTypedNode;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.spi.cte.SqmSearchClauseSpecification;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.spi.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.spi.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmValues;
import org.hibernate.query.sqm.tree.spi.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.spi.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.spi.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.spi.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.spi.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.spi.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.TemporalField;
import jakarta.persistence.metamodel.Bindable;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.determineProperSizing;
import static org.hibernate.query.internal.QueryHelper.highestPrecedenceType;
import static org.hibernate.query.sqm.internal.SqmUtil.failIfSafeModeEnabled;
import static org.hibernate.query.sqm.TrimSpec.fromCriteriaTrimSpec;
import static org.hibernate.query.sqm.tree.spi.select.SqmDynamicInstantiation.classInstantiation;
import static org.hibernate.query.sqm.tree.spi.select.SqmDynamicInstantiation.listInstantiation;
import static org.hibernate.query.sqm.tree.spi.select.SqmDynamicInstantiation.mapInstantiation;
import static org.hibernate.query.sqm.tree.spi.SqmCopyContext.noParamCopyContext;
import static org.hibernate.query.sqm.tree.spi.SqmCopyContext.simpleContext;
import static org.hibernate.type.descriptor.converter.internal.ConverterHelper.createConvertedParameterType;

/**
 * Acts as a JPA {@link jakarta.persistence.criteria.CriteriaBuilder} by
 * using SQM nodes as the JPA Criteria nodes
 *
 * @author Steve Ebersole
 * @author Yoobin Yoon
 */
public class SqmCriteriaNodeBuilder implements NodeBuilder, Serializable {

	private final String uuid;
	private final String name;
	private final transient JpaCompliance jpaCompliance;
	private final transient QueryEngine queryEngine;
	private final transient ValueHandlingMode criteriaValueHandlingMode;
	private final transient ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode;
	private final transient BindingContext bindingContext;
	private final transient ServiceRegistry serviceRegistry;
	private final transient boolean safeModeEnabled;
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
			BindingContext bindingContext,
			ServiceRegistry serviceRegistry) {
		this.queryEngine = queryEngine;
		this.safeModeEnabled = options.isSafeModeEnabled();
		this.uuid = uuid;
		this.name = name;
		this.jpaCompliance = options.getJpaCompliance();
		this.criteriaValueHandlingMode = options.getCriteriaValueHandlingMode();
		this.immutableEntityUpdateQueryHandlingMode = options.getImmutableEntityUpdateQueryHandlingMode();
		this.bindingContext = bindingContext;
		this.serviceRegistry = serviceRegistry;
		this.extensions = loadExtensions();
	}

	private Map<Class<? extends HibernateCriteriaBuilder>, HibernateCriteriaBuilder> loadExtensions() {
		// load registered criteria builder extensions
		final Map<Class<? extends HibernateCriteriaBuilder>, HibernateCriteriaBuilder> extensions = new HashMap<>();
		for ( var extension : ServiceLoader.load( CriteriaBuilderExtension.class ) ) {
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
		final var integerType = this.integerType;
		if ( integerType == null ) {
			return this.integerType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.INTEGER );
		}
		return integerType;
	}

	@Override
	public BasicType<Long> getLongType() {
		final var longType = this.longType;
		if ( longType == null ) {
			return this.longType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.LONG );
		}
		return longType;
	}

	@Override
	public BasicType<Character> getCharacterType() {
		final var characterType = this.characterType;
		if ( characterType == null ) {
			return this.characterType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.CHARACTER );
		}
		return characterType;
	}

	@Override
	public BasicType<String> getStringType() {
		final var stringType = this.stringType;
		if ( stringType == null ) {
			return this.stringType =
					getTypeConfiguration().getBasicTypeRegistry()
							.resolve( StandardBasicTypes.STRING );
		}
		return stringType;
	}

	public FunctionReturnTypeResolver getSumReturnTypeResolver() {
		final var resolver = sumReturnTypeResolver;
		if ( resolver == null ) {
			return sumReturnTypeResolver =
					new SumReturnTypeResolver( getTypeConfiguration() );
		}
		return resolver;
	}

	public FunctionReturnTypeResolver getAvgReturnTypeResolver() {
		final var resolver = avgReturnTypeResolver;
		if ( resolver == null ) {
			return avgReturnTypeResolver =
					new AvgFunction.ReturnTypeResolver( getTypeConfiguration() );
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

	@Nonnull
	@Override
	public SqmSelectStatement<Object> createQuery() {
		// IMPORTANT: we want to pass null here for the result-type
		// to indicate that we do not know.  this will allow later
		// calls to `SqmSelectStatement#select`, `SqmSelectStatement#multiSelect`,
		// etc. to influence the result type
		return new SqmSelectStatement<>( Object.class, this );
	}

	@Nonnull
	@Override
	public <T> SqmSelectStatement<T> createQuery(@Nonnull Class<T> resultClass) {
		return new SqmSelectStatement<>( resultClass, this );
	}

	@Nonnull
	@Override
	public <T> SqmSelectStatement<T> createQuery(@Nonnull String hql, @Nonnull Class<T> resultClass) {
		if ( queryEngine.getHqlTranslator().translate( hql, resultClass )
				instanceof SqmSelectStatement<T> selectStatement ) {
			return new SqmSelectStatement<>( selectStatement, resultClass );
		}
		else {
			throw new IllegalArgumentException("Not a 'select' statement");
		}
	}

	@Nonnull
	@Override
	public CriteriaQuery<?> createQuery(@Nonnull String jpql) {
		if ( queryEngine.getHqlTranslator().translate( jpql, null )
				instanceof SqmSelectStatement<?> selectStatement ) {
			return new SqmSelectStatement<>( selectStatement );
		}
		else {
			throw new IllegalArgumentException("Not a 'select' statement");
		}
	}

	@Nonnull
	@Override
	public <T> CriteriaQuery<T> createQuery(@Nonnull Class<T> resultClass, @Nonnull String jpql) {
		return createQuery( jpql, resultClass );
	}

	@Nonnull
	@Override
	public SqmSelectStatement<Tuple> createTupleQuery() {
		return new SqmSelectStatement<>( Tuple.class, this );
	}

	@Nonnull
	@Override
	public <T> SqmUpdateStatement<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity) {
		return new SqmUpdateStatement<>( targetEntity, this );
	}

	@Nonnull
	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity, @Nonnull String jpql) {
		if ( queryEngine.getHqlTranslator().translate( jpql, targetEntity )
				instanceof SqmUpdateStatement<?> updateStatement ) {
			return new SqmUpdateStatement<>( updateStatement );
		}
		else {
			throw new IllegalArgumentException("Not an 'update' statement");
		}
	}

	@Nonnull
	@Override
	public CriteriaUpdate<?> createCriteriaUpdate(@Nonnull String jpql) {
		if ( queryEngine.getHqlTranslator().translate( jpql, null )
				instanceof SqmUpdateStatement<?> updateStatement ) {
			return new SqmUpdateStatement<>( updateStatement );
		}
		else {
			throw new IllegalArgumentException("Not an 'update' statement");
		}
	}

	@Nonnull
	@Override
	public <T> SqmDeleteStatement<T> createCriteriaDelete(@Nonnull Class<T> targetEntity) {
		return new SqmDeleteStatement<>( targetEntity, this );
	}

	@Nonnull
	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(@Nonnull Class<T> targetEntity, @Nonnull String jpql) {
		if ( queryEngine.getHqlTranslator().translate( jpql, targetEntity )
				instanceof SqmDeleteStatement<?> deleteStatement ) {
			return new SqmDeleteStatement<>( deleteStatement );
		}
		else {
			throw new IllegalArgumentException("Not a 'delete' statement");
		}
	}

	@Nonnull
	@Override
	public CriteriaDelete<?> createCriteriaDelete(@Nonnull String jpql) {
		if ( queryEngine.getHqlTranslator().translate( jpql, null )
				instanceof SqmDeleteStatement<?> deleteStatement ) {
			return new SqmDeleteStatement<>( deleteStatement );
		}
		else {
			throw new IllegalArgumentException("Not a 'delete' statement");
		}
	}

	@Nonnull
	@Override
	public <T> TypedQueryReference<T> augment(
			@Nonnull TypedQueryReference<T> reference,
			@Nonnull Consumer<CriteriaQuery<T>> augmentation) {
		final var buildResult = buildCriteriaQuery( reference );
		augmentation.accept( buildResult.sqmStatement );
		return new AugmentedTypedQueryReference<>(
				reference,
				buildResult.sqmStatement,
				buildResult.sqmMemento
		);
	}

	@Override
	@Nonnull
	public <T> TypedQueryReference<T> augment(@Nonnull TypedQueryReference<?> reference,
											@Nonnull Class<T> augmentedResultType,
											@Nonnull Consumer<CriteriaQuery<T>> augmentation) {
		final var buildResult = buildCriteriaQuery( reference, augmentedResultType );
		augmentation.accept( buildResult.sqmStatement );
		return new AugmentedTypedQueryReference<>(
				reference,
				augmentedResultType,
				buildResult.sqmStatement,
				buildResult.sqmMemento
		);
	}

	private <T> SqmBuildResult<T> buildCriteriaQuery(TypedQueryReference<T> reference) {
		@SuppressWarnings("unchecked")
		final var resultType = (Class<T>) reference.getResultType();
		final var namedMemento = queryEngine.getNamedObjectRepository()
				.getQueryMementoByName( reference.getName(), false );
		if ( namedMemento instanceof NamedSqmQueryMemento<?> sqmMemento ) {
			return new SqmBuildResult<>( createCriteriaQuery( sqmMemento, resultType ), sqmMemento );
		}
		else {
			throw new IllegalSelectQueryException(
					"CriteriaBuilder.augment() only supports HQL query references: " + reference.getName()
			);
		}
	}

	private <T> SqmBuildResult<T> buildCriteriaQuery(TypedQueryReference<?> reference, Class<T> resultType) {
		final var namedMemento = queryEngine.getNamedObjectRepository()
				.getQueryMementoByName( reference.getName(), false );
		if ( namedMemento instanceof NamedSqmQueryMemento<?> sqmMemento ) {
			return new SqmBuildResult<>( createCriteriaQueryNoSelect( sqmMemento, resultType ), sqmMemento );
		}
		else {
			throw new IllegalSelectQueryException(
					"CriteriaBuilder.augment() only supports HQL query references: " + reference.getName()
			);
		}
	}

	private <T> SqmSelectStatement<T> createCriteriaQuery(NamedSqmQueryMemento<?> sqmMemento, Class<T> resultType) {
		final var sqmStatement = sqmMemento.getSqmStatement();
		if ( sqmStatement == null ) {
			return createCriteriaQuery( sqmMemento.getHqlString(), resultType );
		}
		else if ( sqmStatement instanceof SqmSelectStatement<?> selectStatement ) {
			return selectStatement.createCopy( simpleContext( SqmQuerySource.CRITERIA ), resultType );
		}
		else {
			throw new IllegalSelectQueryException(
					"Expecting a selection query, but found '" + sqmMemento.getHqlString() + "'",
					sqmMemento.getHqlString()
			);
		}
	}

	private <T> SqmSelectStatement<T> createCriteriaQueryNoSelect(NamedSqmQueryMemento<?> sqmMemento, Class<T> resultType) {
		final var sqmStatement = sqmMemento.getSqmStatement();
		if ( sqmStatement == null ) {
			final var query = createCriteriaQuery( sqmMemento.getHqlString(), resultType );
			clearSelection( query );
			return query;
		}
		else if ( sqmStatement instanceof SqmSelectStatement<?> selectStatement ) {
			final var copy = selectStatement.createCopy( simpleContext( SqmQuerySource.CRITERIA ), resultType );
			clearSelection( copy );
			return copy;
		}
		else {
			throw new IllegalSelectQueryException(
					"Expecting a selection query, but found '" + sqmMemento.getHqlString() + "'",
					sqmMemento.getHqlString()
			);
		}
	}

	private void clearSelection(SqmSelectStatement<?> query) {
		final var querySpec = query.getQuerySpec();
		querySpec.setSelectClause( new SqmSelectClause( querySpec.getSelectClause().isDistinct(), this ) );
	}

	private <T> SqmSelectStatement<T> createCriteriaQuery(String hql, Class<T> resultType) {
		final var hqlInterpretation =
				queryEngine.getInterpretationCache()
						.resolveHqlInterpretation( hql, resultType, queryEngine.getHqlTranslator() );
		if ( hqlInterpretation.getSqmStatement() instanceof SqmSelectStatement<?> selectStatement ) {
			return selectStatement.createCopy( noParamCopyContext( SqmQuerySource.CRITERIA ), resultType );
		}
		else {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found '" + hql + "'", hql );
		}
	}

	private record SqmBuildResult<T>(
			SqmSelectStatement<T> sqmStatement,
			@Nullable NamedSqmQueryMemento<?> sqmMemento) {
	}

	@Nonnull
	@Override
	public StatementReference augment(
			@Nonnull StatementReference reference,
			@Nonnull Consumer<CriteriaStatement<?>> augmentation) {
		return MutationSpecification.create( reference )
				.augment( (builder, statement, root) -> augmentation.accept( (CriteriaStatement<?>) statement ) )
				.reference();
	}

	@Nonnull
	@Override
	public <T> SqmInsertValuesStatement<T> createCriteriaInsertValues(@Nonnull Class<T> targetEntity) {
		return new SqmInsertValuesStatement<>( targetEntity, this );
	}

	@Nonnull
	@Override
	public <T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(@Nonnull Class<T> targetEntity) {
		return new SqmInsertSelectStatement<>( targetEntity, this );
	}

	@Nonnull
	@Override
	public SqmValues values(@Nonnull Expression<?>... expressions) {
		return values( asList( expressions ) );
	}

	@Nonnull
	@Override
	public SqmValues values(@Nonnull List<? extends Expression<?>> expressions) {
		//noinspection unchecked
		return new SqmValues( (List<SqmExpression<?>>) expressions );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> union(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.UNION_ALL : SetOperator.UNION, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersect(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> except(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return setOperation( all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaSelect<T> union(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.UNION,
					(Subquery<T>) left,
					(Subquery<T>) right );
		}
		else {
			//noinspection unchecked
			return setOperation( SetOperator.UNION,
					(JpaCriteriaQuery<T>) left,
					(JpaCriteriaQuery<T>) right );
		}
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> union(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return setOperation( all ? SetOperator.UNION_ALL : SetOperator.UNION, query1, queries );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> unionAll(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.UNION_ALL, (Subquery<T>) left, (Subquery<T>) right );
		}
		else {
			//noinspection unchecked
			return setOperation( SetOperator.UNION_ALL,
					(JpaCriteriaQuery<T>) left,
					(JpaCriteriaQuery<T>) right );
		}
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> intersect(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return setOperation( all ? SetOperator.INTERSECT_ALL : SetOperator.INTERSECT, query1, queries );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> except(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT,
					(Subquery<T>) left,
					(Subquery<T>) right );
		}
		else {
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT,
					(JpaCriteriaQuery<T>) left,
					(JpaCriteriaQuery<T>) right );
		}
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> exceptAll(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT_ALL,
					(Subquery<T>) left,
					(Subquery<T>) right );
		}
		else {
			//noinspection unchecked
			return setOperation( SetOperator.EXCEPT_ALL,
					(JpaCriteriaQuery<T>) left,
					(JpaCriteriaQuery<T>) right );
		}
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> except(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return setOperation( all ? SetOperator.EXCEPT_ALL : SetOperator.EXCEPT, query1, queries );
	}

	@SuppressWarnings("unchecked")
	private <T> JpaCriteriaQuery<T> setOperation(
			SetOperator operator,
			CriteriaQuery<? extends T> criteriaQuery,
			CriteriaQuery<?>... queries) {
		final var resultType = (Class<T>) criteriaQuery.getResultType();
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( queries.length + 1 );
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>();
		final var selectStatement = (SqmSelectStatement<T>) criteriaQuery;
		collectQueryPartsAndCtes( selectStatement, queryParts, cteStatements );
		for ( var query : queries ) {
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
		final var resultType = (Class<T>) subquery.getResultType();
		final var parent = (SqmQuery<T>) subquery.getParent();
		final List<SqmQueryPart<T>> queryParts = new ArrayList<>( queries.length + 1 );
		final Map<String, SqmCteStatement<?>> cteStatements = new LinkedHashMap<>();
		collectQueryPartsAndCtes( (SqmSelectQuery<T>) subquery, queryParts, cteStatements );
		for ( var query : queries ) {
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
		for ( var cteStatement : query.getCteStatements() ) {
			final String name = cteStatement.getCteTable().getCteName();
			final var old = cteStatements.put( name, cteStatement );
			if ( old != null && old != cteStatement ) {
				throw new IllegalArgumentException(
						String.format( "Different CTE with same name [%s] found in different set operands!", name )
				);
			}
		}
	}

	@Nonnull
	@Override
	public <X, T> SqmExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull Class<X> castTargetJavaType) {
		return cast( expression, castTarget( castTargetJavaType ) );
	}

	@Nonnull
	@Override
	public <X, T> SqmExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull JpaCastTarget<X> castTarget) {
		final var sqmCastTarget = (SqmCastTarget<X>) castTarget;
		return getFunctionDescriptor( "cast" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) expression, sqmCastTarget ),
				sqmCastTarget.getType(),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType) {
		return castTarget( castTargetJavaType, null, null, null );
	}

	@Nonnull
	@Override
	public <X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, long length) {
		return castTarget( castTargetJavaType, length, null, null );
	}

	@Nonnull
	@Override
	public <X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, int precision, int scale) {
		return castTarget( castTargetJavaType, null, precision, scale );
	}

	private <X> SqmCastTarget<X> castTarget(
			Class<X> castTargetJavaType,
			@Nullable Long length, @Nullable Integer precision, @Nullable Integer scale) {
		final var type = getTypeConfiguration().standardBasicTypeForJavaType( castTargetJavaType );
		return new SqmCastTarget<>( type, length, precision, scale, this );
	}

	@Nonnull
	@Override
	public SqmPredicate wrap(@Nonnull Expression<Boolean> expression) {
		return expression instanceof SqmPredicate predicate
				? predicate
				: new SqmBooleanExpressionPredicate( (SqmExpression<Boolean>) expression, this );
	}

	@Nonnull
	@Override
	@SafeVarargs
	public final SqmPredicate wrap(@Nonnull Expression<Boolean>... expressions) {
		if ( expressions.length == 1 ) {
			return wrap( expressions[0] );
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( expressions.length );
			for ( var expression : expressions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate wrap(@Nonnull BooleanExpression... expressions) {
		if ( expressions.length == 1 ) {
			return wrap( expressions[0] );
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( expressions.length );
			for ( var expression : expressions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Override
	public SqmPredicate wrap(List<? extends Expression<Boolean>> restrictions) {
		if ( restrictions.size() == 1 ) {
			return wrap( restrictions.get( 0 ) );
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.size() );
			for ( var expression : restrictions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Nonnull
	@Override @SuppressWarnings("unchecked")
	public <T extends HibernateCriteriaBuilder> T unwrap(@Nonnull Class<T> clazz) {
		final T result = (T) extensions.get( clazz );
		if ( result == null ) {
			throw new IllegalArgumentException( "Unable to unwrap to " + clazz.getName() );
		}
		return result;
	}

	@Nonnull
	@Override
	public SqmPath<?> fk(@Nonnull Path<?> path) {
		final var sqmPath = (SqmPath<?>) path;
		final var toOneReference = sqmPath.getReferencedPathSource();
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

	@Nonnull
	@Override
	public <X, T extends X> SqmPath<T> treat(@Nonnull Path<X> path, @Nonnull Class<T> type) {
		return ( (SqmPath<X>) path ).treatAs( type );
	}

	@Nonnull
	@Override
	public <X, T extends X> SqmRoot<T> treat(@Nonnull Root<X> root, @Nonnull Class<T> type) {
		final var treatedRoot = ((SqmRoot<X>) root).treatAs( type );
		@SuppressWarnings("unchecked") // there is a real inconsistency in the supertypes of SqmTreatedRoot
		final var castRoot = (SqmRoot<T>) treatedRoot;
		return castRoot;
	}

	@Nonnull
	@Override
	public <X, Y, T extends Y> SqmFrom<X, T> treat(@Nonnull From<X, Y> from, @Nonnull Class<T> type) {
		return ( (SqmFrom<X, Y>) from ).treatAs( type );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> union(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right) {
		return createUnionSet( SetOperator.UNION, left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> unionAll(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right) {
		return createUnionSet( SetOperator.UNION_ALL, left, right );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> intersect(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right) {
		if ( left instanceof Subquery<?> ) {
			assert right instanceof Subquery<?>;
			//noinspection unchecked
			return setOperation( SetOperator.INTERSECT, (Subquery<T>) left, (Subquery<T>) right );
		}
		//noinspection unchecked
		return setOperation( SetOperator.INTERSECT, (JpaCriteriaQuery<T>) left, (JpaCriteriaQuery<T>) right );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> intersectAll(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right) {
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
		assert operator == SetOperator.UNION
			|| operator == SetOperator.UNION_ALL;
		final var leftSqm = (SqmSelectStatement<? extends T>) left;
		final var rightSqm = (SqmSelectStatement<? extends T>) right;

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

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersect(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right) {
		return createIntersectSet( SetOperator.INTERSECT, left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right) {
		return createIntersectSet( SetOperator.INTERSECT_ALL, left, right );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> JpaCriteriaQuery<T> createIntersectSet(
			SetOperator operator,
			CriteriaQuery<? super T> left,
			CriteriaQuery<? super T> right) {
		assert operator == SetOperator.INTERSECT
			|| operator == SetOperator.INTERSECT_ALL;
		final var leftSqm = (SqmSelectStatement<? extends T>) left;
		final var rightSqm = (SqmSelectStatement<? extends T>) right;

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

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> except(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right) {
		return createExceptSet( SetOperator.EXCEPT, left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right) {
		return createExceptSet( SetOperator.EXCEPT_ALL, left, right );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> JpaCriteriaQuery<T> createExceptSet(
			SetOperator operator,
			CriteriaQuery<T> left,
			CriteriaQuery<?> right) {
		assert operator == SetOperator.EXCEPT
			|| operator == SetOperator.EXCEPT_ALL;
		final var leftSqm = (SqmSelectStatement<? extends T>) left;
		final var rightSqm = (SqmSelectStatement<? extends T>) right;

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

	@Nonnull
	@Override
	public <X, T, V extends T> SqmJoin<X, V> treat(@Nonnull Join<X, T> join, @Nonnull Class<V> type) {
		return (SqmJoin<X,V>) ( (SqmJoin<X, T>) join ).treatAs( type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> SqmBagJoin<X, E> treat(@Nonnull CollectionJoin<X, T> join, @Nonnull Class<E> type) {
		return ( (SqmBagJoin<X, T>) join ).treatAs( type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> SqmSetJoin<X, E> treat(@Nonnull SetJoin<X, T> join, @Nonnull Class<E> type) {
		return ( (SqmSetJoin<X, T>) join ).treatAs( type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> SqmListJoin<X, E> treat(@Nonnull ListJoin<X, T> join, @Nonnull Class<E> type) {
		return ( (SqmListJoin<X, T>) join ).treatAs( type );
	}

	@Nonnull
	@Override
	public <X, K, T, V extends T> SqmMapJoin<X, K, V> treat(@Nonnull MapJoin<X, K, T> join, @Nonnull Class<V> type) {
		return ( (SqmMapJoin<X, K, T>) join ).treatAs( type );
	}

	@Nonnull
	@Override
	public SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public SqmSortSpecification sort(
			@Nonnull JpaExpression<?> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence,
			boolean ignoreCase) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Nonnull
	@Override
	public SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression, sortOrder );
	}

	@Nonnull
	@Override
	public SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression) {
		return new SqmSortSpecification( (SqmExpression<?>) sortExpression );
	}

	@Nonnull
	@Override
	public SqmSortSpecification asc(@Nonnull Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortDirection.ASCENDING );
	}

	@Nonnull
	@Override
	public SqmSortSpecification desc(@Nonnull Expression<?> x) {
		return new SqmSortSpecification( (SqmExpression<?>) x, SortDirection.DESCENDING );
	}

	@Nonnull
	@Override
	public SqmSortSpecification asc(@Nonnull Expression<?> expression, @Nonnull Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) expression, SortDirection.ASCENDING, nullPrecedence );
	}

	@Nonnull
	@Override
	public SqmSortSpecification desc(@Nonnull Expression<?> expression, @Nonnull Nulls nullPrecedence) {
		return new SqmSortSpecification( (SqmExpression<?>) expression, SortDirection.DESCENDING, nullPrecedence );
	}

	@Nonnull
	@Override
	public SqmSortSpecification asc(@Nonnull Expression<?> x, boolean nullsFirst) {
		return new SqmSortSpecification(
				(SqmExpression<?>) x,
				SortDirection.ASCENDING,
				nullsFirst ? Nulls.FIRST : Nulls.LAST
		);
	}

	@Nonnull
	@Override
	public SqmSortSpecification desc(@Nonnull Expression<?> x, boolean nullsFirst) {
		return new SqmSortSpecification(
				(SqmExpression<?>) x,
				SortDirection.DESCENDING,
				nullsFirst ? Nulls.FIRST : Nulls.LAST
		);
	}

	@Nonnull
	@Override
	public JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute sortExpression, @Nonnull SortDirection sortOrder) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, sortOrder, Nulls.NONE );
	}

	@Nonnull
	@Override
	public JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute sortExpression) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) sortExpression, SortDirection.ASCENDING, Nulls.NONE );
	}

	@Nonnull
	@Override
	public JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) x, SortDirection.ASCENDING, Nulls.NONE );
	}

	@Nonnull
	@Override
	public JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x) {
		return new SqmSearchClauseSpecification( (SqmCteTableColumn) x, SortDirection.DESCENDING, Nulls.NONE );
	}

	@Nonnull
	@Override
	public JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return new SqmSearchClauseSpecification(
				(SqmCteTableColumn) x,
				SortDirection.ASCENDING,
				nullsFirst ? Nulls.FIRST : Nulls.LAST
		);
	}

	@Nonnull
	@Override
	public JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return new SqmSearchClauseSpecification(
				(SqmCteTableColumn) x,
				SortDirection.DESCENDING,
				nullsFirst ? Nulls.FIRST : Nulls.LAST
		);
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Tuple> tuple(@Nonnull Selection<?>... selections) {
		return tuple( asList( selections ) );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Tuple> tuple(@Nonnull List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new SqmJpaCompoundSelection<>(
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList(),
				getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( Tuple.class ),
				this
		);
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Object[]> array(@Nonnull Selection<?>... selections) {
		return array( Object[].class,
				Arrays.stream( selections ).map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Object[]> array(@Nonnull List<Selection<?>> selections) {
		return arrayInternal( Object[].class,
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections) {
		return arrayInternal( resultClass,
				Arrays.stream( selections ).map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> selections) {
		return arrayInternal( resultClass,
				selections.stream().map( selection -> (SqmSelectableNode<?>) selection ).toList() );
	}

	public <Y> JpaCompoundSelection<Y> arrayInternal(Class<Y> resultClass, List<? extends SqmSelectableNode<?>> selections) {
		checkMultiselect( selections );
		final var javaType = getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( resultClass );
		return new SqmJpaCompoundSelection<>( selections, javaType, this );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... arguments) {
		return constructInternal( resultClass,
				Arrays.stream( arguments ).map( arg -> (SqmSelectableNode<?>) arg ).toList() );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> arguments) {
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
		for ( var selection : selections ) {
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

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Double> avg(@Nonnull Expression<N> argument) {
		return getFunctionDescriptor( "avg" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <N extends Number> SqmExpression<N> sum(@Nonnull Expression<N> argument) {
		final var typedNode = (SqmTypedNode<N>) argument;
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				typedNode,
				(ReturnableType<N>) typedNode.getExpressible().getSqmType(),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> sumAsLong(@Nonnull Expression<Integer> argument) {
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> sumAsDouble(@Nonnull Expression<Float> argument) {
		return getFunctionDescriptor( "sum" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> max(@Nonnull Expression<N> argument) {
		return getFunctionDescriptor( "max" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> min(@Nonnull Expression<N> argument) {
		return getFunctionDescriptor( "min" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> greatest(@Nonnull Expression<X> argument) {
		return getFunctionDescriptor( "max" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine);
	}

	@Nonnull
	@Override
	public <X extends Comparable<? super X>> SqmExpression<X> least(@Nonnull Expression<X> argument) {
		return getFunctionDescriptor( "min" )
				.generateSqmExpression( (SqmTypedNode<?>) argument, null, queryEngine);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> count(@Nonnull Expression<?> argument) {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				(SqmTypedNode<?>) argument,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> countDistinct(@Nonnull Expression<?> argument) {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				new SqmDistinct<>( (SqmExpression<?>) argument, this ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> count() {
		return getFunctionDescriptor( "count" ).generateSqmExpression(
				new SqmStar( this ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> sign(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "sign" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> ceiling(@Nonnull Expression<N> x) {
		return getFunctionDescriptor( "ceiling" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> floor(@Nonnull Expression<N> x) {
		return getFunctionDescriptor( "floor" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> exp(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "exp" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> ln(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "ln" ).generateSqmExpression(
				(SqmExpression<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return getFunctionDescriptor( "power" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, (SqmExpression<?>) y),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return getFunctionDescriptor( "power" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( y ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T extends Number> SqmExpression<T> round(@Nonnull Expression<T> x, @Nonnull Integer n) {
		return getFunctionDescriptor( "round" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( n ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T extends Number> SqmExpression<T> truncate(@Nonnull Expression<T> x, @Nullable Integer n) {
		return getFunctionDescriptor( "truncate" ).generateSqmExpression(
				asList( (SqmExpression<?>) x, value( n ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> neg(@Nonnull Expression<N> x) {
		return new SqmUnaryOperation<>(
				UnaryArithmeticOperator.UNARY_MINUS,
				(SqmExpression<N>) x,
				getNodeBuilder()
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> abs(@Nonnull Expression<N> x) {
		return getFunctionDescriptor( "abs" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> duration(long magnitude, @Nonnull TemporalUnit unit) {
		return new SqmToDuration<>(
				literal( magnitude ),
				new SqmDurationUnit<>( unit, getLongType(), this ),
				getTypeConfiguration().standardBasicTypeForJavaType( Duration.class ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> durationByUnit(@Nonnull TemporalUnit unit, @Nonnull Expression<Duration> duration) {
		return new SqmByUnit(
				new SqmDurationUnit<>( unit, getLongType(), this ),
				(SqmExpression<Duration>) duration,
				getLongType(),
				this
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<Duration>) x, (SqmExpression<Duration>) y );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nullable Duration y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<Duration>) x, value( y ) );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<Duration>) x, (SqmExpression<Duration>) y );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nullable Duration y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<Duration>) x, value( y ) );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends Number>) number, (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationScaled(@Nullable Number number, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				value( number ), (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public SqmExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nullable Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends Number>) number, value( duration ) );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nonnull Expression<T> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) x, (SqmExpression<T>) y );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nullable T y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) x, value( y ) );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<T>) datetime, (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<T>) datetime, value( duration ) );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> addDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				value( datetime ), (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) datetime, (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<T>) datetime, value( duration ) );
	}

	@Nonnull
	@Override
	public <T extends Temporal> SqmExpression<T> subtractDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.SUBTRACT,
				value( datetime ), (SqmExpression<Duration>) duration );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> sum(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode( BinaryArithmeticOperator.ADD,
				(SqmExpression<? extends N>) x, (SqmExpression<? extends N>) y );
	}

	private <N> SqmExpression<N> createSqmArithmeticNode(
			BinaryArithmeticOperator operator,
			SqmExpression<?> leftHandExpression,
			SqmExpression<?> rightHandExpression) {
		final var arithmeticType =
				getTypeConfiguration()
						.resolveArithmeticType(
								leftHandExpression.getNodeType(),
								rightHandExpression.getNodeType(),
								operator
						);
		@SuppressWarnings("unchecked")
		final var castType = (SqmBindableType<N>) arithmeticType;
		return new SqmBinaryArithmetic<>(
				operator,
				leftHandExpression,
				rightHandExpression,
				castType,
				this
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> sum(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> sum(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.ADD,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends N>) x,
				(SqmExpression<? extends N>) y
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> prod(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MULTIPLY,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<? extends N>) x,
				(SqmExpression<? extends N>) y
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				(SqmExpression<? extends N>) x,
				value( y )
		);
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<N> diff(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.SUBTRACT,
				value( x ),
				(SqmExpression<? extends N>) y
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression<? extends Number>) x,
				(SqmExpression<? extends Number>) y
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				(SqmExpression<? extends Number>) x,
				value( y )
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Number> quot(@Nullable Number x, @Nonnull Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.QUOT,
				value( x ),
				(SqmExpression<? extends Number>) y
		);
	}

	@Override
	public SqmExpression<Number> quotPortable(Expression<? extends Number> x, Expression<? extends Number> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.DIVIDE_PORTABLE,
				(SqmExpression<? extends Number>) x,
				(SqmExpression<? extends Number>) y
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression<Integer>) x,
				(SqmExpression<Integer>) y
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nullable Integer y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				(SqmExpression<Integer>) x,
				value( y )
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> mod(@Nullable Integer x, @Nonnull Expression<Integer> y) {
		return createSqmArithmeticNode(
				BinaryArithmeticOperator.MODULO,
				value( x ),
				(SqmExpression<Integer>) y
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Double> sqrt(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "sqrt" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> toLong(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asLong();
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> toInteger(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asInteger();
	}

	@Nonnull
	@Override
	public SqmExpression<Float> toFloat(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asFloat();
	}

	@Nonnull
	@Override
	public SqmExpression<Double> toDouble(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asDouble();
	}

	@Nonnull
	@Override
	public SqmExpression<BigDecimal> toBigDecimal(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigDecimal();
	}

	@Nonnull
	@Override
	public SqmExpression<BigInteger> toBigInteger(@Nonnull Expression<? extends Number> number) {
		return ( (SqmExpression<?>) number ).asBigInteger();
	}

	@Nonnull
	@Override
	public SqmExpression<String> toString(@Nonnull Expression<Character> character) {
		return ( (SqmExpression<?>) character ).asString();
	}

	public <T> SqmLiteral<T> literal(@Nullable T value, @Nullable SqmExpression<? extends T> typeInferenceSource) {
		return value == null
				? new SqmLiteralNull<>( this )
				: createLiteral( value, resolveInferredType( value, typeInferenceSource ) );
	}

	private <T> SqmLiteral<T> createLiteral(T value, SqmBindableType<T> expressible) {
		final var javaType = expressible.getExpressibleJavaType();
		if ( javaType.isInstance( value ) ) {
			return new SqmLiteral<>( value, expressible, this );
		}
		else {
			// Just like in HQL, we allow coercion of literal values to the inferred type
			final Object coercedValue = javaType.coerce( value );
			// ignore typeInferenceSource and fall back to the value type
			return javaType.isInstance( coercedValue )
					? new SqmLiteral<>( javaType.cast( coercedValue ), expressible, this )
					: literal( value );
		}
	}

	private <T> SqmBindableType<? extends T> resolveInferredType(
			@Nullable T value, @Nullable SqmExpression<? extends T> typeInferenceSource) {
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
		final var typeConfiguration = getTypeConfiguration();
		final var type = ReflectHelper.getClass( value );
		final var result = typeConfiguration.getBasicTypeForJavaType( type );
		if ( result == null && value instanceof Enum<?> enumValue ) {
			return (BasicType<T>)
					resolveEnumType( typeConfiguration, enumValue );
		}
		else {
			return result;
		}
	}

	private static <E extends Enum<E>> BasicType<E> resolveEnumType(TypeConfiguration configuration, Enum<E> enumValue) {
		final var enumJavaType = new EnumJavaType<>( ReflectHelper.getClass( enumValue ) );
		final var jdbcType = enumJavaType.getRecommendedJdbcType( configuration.getCurrentBaseSqlTypeIndicators() );
		return configuration.getBasicTypeRegistry().resolve( enumJavaType, jdbcType );
	}

	@Nonnull
	@Override
	public <T> SqmLiteral<T> literal(@Nonnull T value) {
		if ( value == null ) {
			if ( jpaCompliance.isJpaQueryComplianceEnabled() ) {
				throw new IllegalArgumentException( "literal value cannot be null" );
			}
			return new SqmLiteralNull<>( this );
		}
		else {
			return new SqmLiteral<>( value, resolveExpressible( getParameterBindType( value ) ), this );
		}
	}

	@Nonnull
	@Override
	public <N extends Number & Comparable<N>> SqmNumericExpression<N> numericLiteral(@Nonnull N value) {
		return new SqmNumericExpressionWrapper<>( literal( value ) );
	}

	@Nonnull
	@Override
	public TextExpression stringLiteral(@Nonnull String value) {
		return new SqmTextExpressionWrapper( literal( value ) );
	}

	@Nonnull
	@Override
	public <T extends Temporal & Comparable<? super T>> TemporalExpression<T> temporalLiteral(@Nonnull T value) {
		return new SqmTemporalExpressionWrapper<>( literal( value ) );
	}

	@Nonnull
	@Override
	public BooleanExpression booleanLiteral(boolean value) {
		return new SqmBooleanExpressionWrapper( literal( value ) );
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return (MappingMetamodelImplementor) bindingContext.getMappingMetamodel();
	}

	@Nonnull
	@Override
	public <T> List<? extends SqmExpression<T>> literals(@Nonnull T[] values) {
		if ( values == null || values.length == 0 ) {
			return emptyList();
		}
		else {
			final List<SqmLiteral<T>> literals = new ArrayList<>();
			for ( T value : values ) {
				literals.add( literal( value ) );
			}
			return literals;
		}
	}

	@Nonnull
	@Override
	public <T> List<? extends SqmExpression<T>> literals(@Nonnull List<T> values) {
		if ( values == null || values.isEmpty() ) {
			return emptyList();
		}
		else {
			final List<SqmLiteral<T>> literals = new ArrayList<>();
			for ( T value : values ) {
				literals.add( literal( value ) );
			}
			return literals;
		}
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> nullLiteral(@Nonnull Class<T> resultClass) {
		if ( resultClass.isEnum() ) {
			// Retain the declared Java type while allowing the enum mapping to be inferred from context.
			return new SqmLiteralNull<>( null,
					getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( resultClass ), this );
		}
		else {
			final var basicTypeForJavaType =
					getTypeConfiguration().getBasicTypeForJavaType( resultClass );
			// if there's no basic type, it might be an entity type
			final var sqmExpressible =
					basicTypeForJavaType == null
							? resolveExpressible( getDomainModel().managedType( resultClass ) )
							: basicTypeForJavaType;
			return new SqmLiteralNull<>( sqmExpressible, this );
		}
	}

	class MultiValueParameterType<T> implements SqmBindableType<T> {
		private final JavaType<T> javaType;

		public MultiValueParameterType(Class<T> type) {
			javaType = getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( type );
		}

		@Override
		@Nonnull
		public PersistenceType getPersistenceType() {
			return BASIC;
		}

		@Override
		public JavaType<T> getExpressibleJavaType() {
			return javaType;
		}

		@Override
		@Nonnull
		public Class<T> getJavaType() {
			return javaType.getJavaTypeClass();
		}

		@Override
		public @Nullable SqmDomainType<T> getSqmType() {
			return null;
		}
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaParameter<T> parameter(@Nonnull Class<T> paramClass) {
		return createParameter( paramClass, null );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaParameter<T> parameter(@Nonnull Class<T> paramClass, @Nonnull String name) {
		return createParameter( paramClass, name );
	}

	@Nonnull
	private <T> JpaCriteriaParameter<T> createParameter(@Nonnull Class<T> paramClass, @Nullable String name) {
		final var basicType = getTypeConfiguration().getBasicTypeForJavaType( paramClass );
		final boolean notBasic = basicType == null;
		final var parameterType =
				notBasic && Collection.class.isAssignableFrom( paramClass )
						// a Collection-valued, multi-valued parameter
						? new MultiValueParameterType<>( (Class<T>) Collection.class )
						: basicType;
		return new JpaCriteriaParameter<>( name, parameterType, paramClass, notBasic, this );
	}

	@Nonnull
	@Override
	public <T> ParameterExpression<T> convertedParameter(@Nonnull Class<? extends AttributeConverter<T, ?>> converter) {
		return new JpaCriteriaParameter<>(
				null,
				createConvertedParameterType( converter, serviceRegistry, getTypeConfiguration() ),
				false,
				this
		);
	}

	@Nonnull
	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass) {
		return listParameter( paramClass, null );
	}

	@Nonnull
	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass, @Nullable String name) {
		final var parameterType = new MultiValueParameterType<>( (Class<List<T>>) (Class) List.class );
		return new JpaCriteriaParameter<>( name, parameterType, true, this );
	}

	@Nonnull
	@Override
	public SqmExpression<String> concat(@Nonnull List<Expression<String>> expressions) {
		//noinspection RedundantCast, unchecked
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) expressions,
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> concat(@Nonnull Expression<String> x, @Nonnull Expression<String> y) {
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<String>) y ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> concat(@Nonnull Expression<String> x, @Nonnull String y) {
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, value( y, (SqmExpression<String>) x ) ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> concat(@Nonnull String x, @Nonnull Expression<String> y) {
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( value( x, (SqmExpression<String>) y ), (SqmExpression<String>) y ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> concat(@Nullable String x, @Nullable String y) {
		return getFunctionDescriptor( "concat" ).generateSqmExpression(
				asList( value( x ), value( y, value( x ) ) ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> substring(@Nonnull Expression<String> source, @Nonnull Expression<Integer> from) {
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

	@Nonnull
	@Override
	public SqmFunction<String> substring(@Nonnull Expression<String> source, int from) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				value( from ),
				null
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> substring(@Nonnull Expression<String> source, @Nonnull Expression<Integer> from, @Nonnull Expression<Integer> len) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				(SqmExpression<Integer>) from,
				(SqmExpression<Integer>) len
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> substring(@Nonnull Expression<String> source, int from, int len) {
		return createSubstringNode(
				(SqmExpression<String>) source,
				value( from ),
				value( len )
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> trim(@Nonnull Expression<String> source) {
		return createTrimNode( null, null, (SqmExpression<String>) source );
	}

	private SqmFunction<String> createTrimNode(
			@Nullable TrimSpec trimSpecification,
			@Nullable SqmExpression<Character> trimCharacter,
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

	@Nonnull
	@Override
	public SqmFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), null, (SqmExpression<String>) source );
	}

	@Nonnull
	@Override
	public SqmFunction<String> trim(@Nonnull Expression<Character> trimChar, @Nonnull Expression<String> source) {
		return createTrimNode( null, (SqmExpression<Character>) trimChar, (SqmExpression<String>) source );
	}

	@Nonnull
	@Override
	public SqmFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<Character> trimChar, @Nonnull Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), (SqmExpression<Character>) trimChar, (SqmExpression<String>) source );
	}

	@Nonnull
	@Override
	public SqmFunction<String> trim(char trimChar, @Nonnull Expression<String> source) {
		return createTrimNode( null, literal( trimChar ), (SqmExpression<String>) source );
	}

	@Nonnull
	@Override
	public SqmFunction<String> trim(@Nonnull Trimspec ts, char trimChar, @Nonnull Expression<String> source) {
		return createTrimNode( fromCriteriaTrimSpec( ts ), literal( trimChar ), (SqmExpression<String>) source );
	}

	@Nonnull
	@Override
	public SqmFunction<String> lower(@Nonnull Expression<String> x) {
		return getFunctionDescriptor( "lower" ).generateSqmExpression(
				(SqmExpression<String>) x,
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> upper(@Nonnull Expression<String> x) {
		return getFunctionDescriptor( "upper" ).generateSqmExpression(
				(SqmExpression<String>) x,
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> length(@Nonnull Expression<String> argument) {
		return getFunctionDescriptor( "length" ).generateSqmExpression(
				(SqmExpression<String>) argument,
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> locate(@Nonnull Expression<String> source, @Nonnull Expression<String> pattern) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				(SqmExpression<String>) pattern,
				null
		);
	}

	private SqmFunction<Integer> createLocateFunctionNode(
			SqmExpression<String> source,
			SqmExpression<String> pattern,
			@Nullable SqmExpression<Integer> startPosition) {
		return getFunctionDescriptor("locate").generateSqmExpression(
				startPosition == null
						? asList( pattern, source )
						: asList( pattern, source, startPosition ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> locate(@Nonnull Expression<String> source, @Nonnull String pattern) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				value( pattern ),
				null
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> locate(@Nonnull Expression<String> source, @Nonnull Expression<String> pattern, @Nonnull Expression<Integer> startPosition) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				(SqmExpression<String>) pattern,
				(SqmExpression<Integer>) startPosition
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> locate(@Nonnull Expression<String> source, @Nonnull String pattern, int startPosition) {
		return createLocateFunctionNode(
				(SqmExpression<String>) source,
				value( pattern ),
				value( startPosition )
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Date> currentDate() {
		return getFunctionDescriptor("current_date")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
	@Override
	public SqmFunction<Timestamp> currentTimestamp() {
		return getFunctionDescriptor("current_timestamp")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
	@Override
	public SqmFunction<Time> currentTime() {
		return getFunctionDescriptor("current_time")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
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

	@Nonnull
	@Override
	public SqmExpression<LocalDate> localDate() {
		return getFunctionDescriptor("local_date")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
	@Override
	public SqmExpression<LocalDateTime> localDateTime() {
		return getFunctionDescriptor("local_datetime")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
	@Override
	public SqmExpression<LocalTime> localTime() {
		return getFunctionDescriptor("local_time")
				.generateSqmExpression(
						null,
						queryEngine
				);
	}

	@Nonnull
	@Override
	public SqmPath<?> id(@Nonnull Path<?> path) {
		return ((SqmPath<?>) path).get( EntityIdentifierMapping.ID_ROLE_NAME );
	}

	@Nonnull
	@Override
	public SqmPath<?> version(@Nonnull Path<?> path) {
		return ((SqmPath<?>) path).get( EntityVersionMapping.VERSION_ROLE_NAME );
	}

	@Nonnull
	@Override
	public <T> SqmFunction<T> function(@Nonnull String name, @Nonnull Class<T> type, @Nonnull Expression<?>[] args) {
		final var resultType = getTypeConfiguration().standardBasicTypeForJavaType( type );
		return getFunctionTemplate( name, resultType ).generateSqmExpression(
				expressionList( args ),
				resultType,
				getQueryEngine()
		);
	}

	private <T> SqmFunctionDescriptor getFunctionTemplate(String name, BasicType<T> resultType) {
		final var functionTemplate = getFunctionDescriptor( name );
		if ( functionTemplate == null ) {
			failIfSafeModeEnabled( safeModeEnabled, name, null );
			return new NamedSqmFunctionDescriptor(
					name,
					true,
					null,
					StandardFunctionReturnTypeResolvers.invariant( resultType ),
					null
			);
		}
		else {
			if ( safeModeEnabled && "sql".equals( name ) ) {
				failIfSafeModeEnabled( safeModeEnabled, name, null );
			}
			return functionTemplate;
		}
	}

	private static List<SqmExpression<?>> expressionList(Expression<?>[] jpaExpressions) {
		if ( jpaExpressions == null || jpaExpressions.length == 0 ) {
			return emptyList();
		}
		else {
			final ArrayList<SqmExpression<?>> sqmExpressions = new ArrayList<>();
			for ( var jpaExpression : jpaExpressions ) {
				sqmExpressions.add( (SqmExpression<?>) jpaExpression );
			}
			return sqmExpressions;
		}
	}

	@Nonnull
	@Override
	public <Y> SqmModifiedSubQueryExpression<Y> all(@Nonnull Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.ALL,
				this
		);
	}

	@Nonnull
	@Override
	public <Y> SqmModifiedSubQueryExpression<Y> some(@Nonnull Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.SOME,
				this
		);
	}

	@Nonnull
	@Override
	public <Y> SqmModifiedSubQueryExpression<Y> any(@Nonnull Subquery<Y> subquery) {
		return new SqmModifiedSubQueryExpression<>(
				(SqmSubQuery<Y>) subquery,
				SqmModifiedSubQueryExpression.Modifier.ANY,
				this
		);
	}

	@Nonnull
	@Override
	public <K, L extends List<?>> SqmExpression<Set<K>> indexes(@Nonnull L list) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Creates an expression for the value with the given "type inference" information
	 */
	public <T> SqmExpression<T> value(@Nullable T value, @Nullable SqmExpression<? extends T> typeInferenceSource) {
		if ( value instanceof SqmExpression<?> ) {
			//noinspection unchecked
			return (SqmExpression<T>) value;
		}
		else {
			return inlineValue( value )
					? literal( value, typeInferenceSource )
					: valueParameter( value, typeInferenceSource );
		}
	}

	private <E> SqmExpression<? extends Collection<?>> collectionValue(
			Collection<E> value, SqmExpression<E> typeInferenceSource) {
		return inlineValue( value )
				? collectionLiteral( value.toArray() )
				: collectionValueParameter( value, typeInferenceSource );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> value(@Nullable T value) {
		if ( value instanceof Duration duration ) {
			final SqmExpression<Duration> expression = duration.getNano() == 0
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

	private <T> boolean isInstance(BindableType<? extends T> bindableType, T value) {
		if ( value == null ) {
			return true;
		}
		else if ( bindableType instanceof SqmExpressible<?> expressible ) {
			return expressible.getExpressibleJavaType().isInstance( value );
		}
		else {
			return bindableType.getJavaType().isInstance( value )
				|| resolveExpressible( bindableType ).getExpressibleJavaType().isInstance( value );
		}
	}

	private static <X, T extends X> @Nullable BindableType<? extends X> resolveInferredParameterType(
			@Nullable X value,
			@Nullable SqmExpression<T> typeInferenceSource,
			TypeConfiguration typeConfiguration) {
		if ( typeInferenceSource != null ) {
			if ( typeInferenceSource instanceof BindableType ) {
				//noinspection unchecked
				return (BindableType<T>) typeInferenceSource;
			}
			else {
				final var nodeType = typeInferenceSource.getExpressible();
				if ( nodeType != null ) {
					return nodeType;
				}
			}
		}

		if ( value == null ) {
			return null;
		}
		else {
			@SuppressWarnings("unchecked") // this is completely safe
			final Class<? extends X> valueClass = (Class<? extends X>) value.getClass();
			return typeConfiguration.getBasicTypeForJavaType( valueClass );
		}
	}

	private <T> ValueBindJpaCriteriaParameter<T> valueParameter(@Nullable T value, @Nullable SqmExpression<? extends T> typeInferenceSource) {
		final var bindableType = resolveInferredParameterType( value, typeInferenceSource, getTypeConfiguration() );
		if ( bindableType == null || isInstance( bindableType, value) ) {
			@SuppressWarnings("unchecked") // safe, we just checked
			final var widerType = (BindableType<? super T>) bindableType;
			return new ValueBindJpaCriteriaParameter<>( widerType, value, this );
		}
		else {
			final var javaType = resolveExpressible( bindableType ).getExpressibleJavaType();
			final Object coercedValue = javaType.coerce( value );
			// ignore typeInferenceSource and fall back to the value type
			if ( isInstance( bindableType, coercedValue ) ) {
				@SuppressWarnings("unchecked") // safe, we just checked
				final var widerType = (BindableType<? super T>) bindableType;
				return new ValueBindJpaCriteriaParameter<>( widerType, javaType.cast( coercedValue ), this );
			}
			else {
				return new ValueBindJpaCriteriaParameter<>( getParameterBindType( value ), value, this );
			}
		}
	}

	private <E> ValueBindJpaCriteriaParameter<? extends Collection<E>> collectionValueParameter(Collection<E> value, SqmExpression<E> elementTypeInferenceSource) {
		final var elementType =
				resolveExpressible( bindableType( elementTypeInferenceSource ) )
						.getSqmType();
		if ( elementType == null ) {
			throw new UnsupportedOperationException( "Can't infer collection type based on element expression: " + elementTypeInferenceSource );
		}
		final var collectionType = DdlTypeHelper.resolveListType( elementType, getTypeConfiguration() );
		//noinspection unchecked
		return new ValueBindJpaCriteriaParameter<>( (BasicType<Collection<E>>) collectionType, value, this );
	}

	private static <E> BindableType<E> bindableType(SqmExpression<E> elementTypeInferenceSource) {
		if ( elementTypeInferenceSource != null ) {
			if ( elementTypeInferenceSource instanceof BindableType ) {
				//noinspection unchecked
				return (BindableType<E>) elementTypeInferenceSource;
			}
			else if ( elementTypeInferenceSource.getNodeType() != null ) {
				return elementTypeInferenceSource.getNodeType();
			}
		}
		return null;
	}

	private <T> ValueBindJpaCriteriaParameter<T> valueParameter(T value) {
		return new ValueBindJpaCriteriaParameter<>( getParameterBindType( value ), value, this );
	}

	private <T> @Nullable BindableType<? super T> getParameterBindType(@Nullable T value) {
		return getMappingMetamodel().resolveParameterBindType( value );
	}

	private <T> boolean inlineValue(T value) {
		return criteriaValueHandlingMode == ValueHandlingMode.INLINE;
//			|| is a literal enum mapped to a PostgreSQL named 'enum' type
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(@Nonnull Expression<C> collection) {
		return new SqmCollectionSize( (SqmPath<C>) collection, this );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<Integer> size(@Nonnull C collection) {
		return new SqmLiteral<>( collection.size(), getIntegerType(), this );
	}

	@Nonnull
	@Override
	public <T> SqmCoalesce<T> coalesce() {
		return new SqmCoalesce<>( this );
	}

	@Nonnull
	@Override
	public <Y> SqmCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		@SuppressWarnings("unchecked")
		final var sqmExpressible = (SqmBindableType<Y>) highestPrecedenceType(
				( (SqmExpression<? extends Y>) x ).getExpressible(),
				( (SqmExpression<? extends Y>) y ).getExpressible()
		);
		return new SqmCoalesce<>( sqmExpressible, 2, this ).value(x).value(y);
	}

	@Nonnull
	@Override
	public <Y> SqmCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		return coalesce( x, value( y, (SqmExpression<? extends Y>) x ) );
	}

	@Nonnull
	@Override
	public <Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y) {
		//noinspection unchecked
		return createNullifFunctionNode( (SqmExpression<Y>) x, (SqmExpression<Y>) y );
	}

	@Nonnull
	@Override
	public <Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, @Nullable Y y) {
		return createNullifFunctionNode( (SqmExpression<Y>) x, value( y, (SqmExpression<Y>) x ) );
	}

	private <Y> SqmExpression<Y> createNullifFunctionNode(SqmExpression<Y> first, SqmExpression<Y> second) {
		final var bindableType = highestPrecedenceType( first.getExpressible(), second.getExpressible() );
		@SuppressWarnings("unchecked")
		final var resultType = bindableType == null ? null : (ReturnableType<Y>) bindableType.getSqmType();
		return getFunctionDescriptor( "nullif" )
				.generateSqmExpression( asList( first, second ), resultType, getQueryEngine() );
	}

	private SqmFunctionDescriptor getFunctionDescriptor(String name) {
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( name );
	}

	private SqmSetReturningFunctionDescriptor getSetReturningFunctionDescriptor(String name) {
		return queryEngine.getSqmFunctionRegistry().findSetReturningFunctionDescriptor( name );
	}

	@Nonnull
	@Override
	public <C, R> SqmCaseSimple<C, R> selectCase(@Nonnull Expression<? extends C> expression) {
		//noinspection unchecked
		return new SqmCaseSimple<>( (SqmExpression<C>) expression, this );
	}

	@Nonnull
	@Override
	public <C, R> SqmCaseSimple<C, R> selectCase(@Nonnull Expression<? extends C> expression, @Nonnull Class<R> resultType) {
		//noinspection unchecked
		return new SqmCaseSimple<>( (SqmExpression<C>) expression, getCaseResultType( resultType ), this );
	}

	@Nonnull
	@Override
	public <R> SqmCaseSearched<R> selectCase() {
		return new SqmCaseSearched<>( this );
	}

	@Nonnull
	@Override
	public <R> SqmCaseSearched<R> selectCase(@Nonnull Class<R> resultType) {
		return new SqmCaseSearched<>( getCaseResultType( resultType ), this );
	}

	private <R> SqmBindableType<R> getCaseResultType(Class<R> resultType) {
		return resultType == null ? null : getTypeConfiguration().getBasicTypeForJavaType( resultType );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> SqmExpression<Integer> mapSize(@Nonnull JpaExpression<M> mapExpression) {
		return new SqmCollectionSize( (SqmPath<?>) mapExpression, this );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> SqmExpression<Integer> mapSize(@Nonnull M map) {
		return new SqmLiteral<>( map.size(), getIntegerType(), this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Nonnull
	@Override
	public SqmPredicate and(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y) {
		return new SqmJunctionPredicate(
				Predicate.BooleanOperator.AND,
				wrap( x ),
				wrap( y ),
				this
		);
	}

	@Nonnull
	@Override
	public JpaPredicate and(@Nonnull BooleanExpression... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return conjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
			for ( var expression : restrictions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate and(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		if ( restrictions == null || restrictions.isEmpty() ) {
			return conjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.size() );
			for ( var expression : restrictions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate and(@Nonnull Predicate... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return conjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
			for ( var expression : restrictions ) {
				predicates.add( (SqmPredicate) expression );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.AND, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate or(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y) {
		return new SqmJunctionPredicate(
				Predicate.BooleanOperator.OR,
				wrap( x ),
				wrap( y ),
				this
		);
	}

	@Nonnull
	@Override
	public JpaPredicate or(@Nonnull BooleanExpression... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return disjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
			for ( var expression : restrictions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.OR, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate or(@Nonnull Predicate... restrictions) {
		if ( restrictions == null || restrictions.length == 0 ) {
			return disjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.length );
			for ( var expression : restrictions ) {
				predicates.add( (SqmPredicate) expression );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.OR, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate or(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		if ( restrictions == null || restrictions.isEmpty() ) {
			return disjunction();
		}
		else {
			final List<SqmPredicate> predicates = new ArrayList<>( restrictions.size() );
			for ( var expression : restrictions ) {
				predicates.add( wrap( expression ) );
			}
			return new SqmJunctionPredicate( Predicate.BooleanOperator.OR, predicates, this );
		}
	}

	@Nonnull
	@Override
	public SqmPredicate not(@Nonnull Expression<Boolean> restriction) {
		return wrap( restriction ).not();
	}

	@Nonnull
	@Override
	public SqmPredicate conjunction() {
		return new SqmComparisonPredicate(
				new SqmLiteral<>( 1, getIntegerType(), this ),
				ComparisonOperator.EQUAL,
				new SqmLiteral<>( 1, getIntegerType(), this ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate disjunction() {
		return new SqmComparisonPredicate(
				new SqmLiteral<>( 1, getIntegerType(), this ),
				ComparisonOperator.NOT_EQUAL,
				new SqmLiteral<>( 1, getIntegerType(), this ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate isTrue(@Nonnull Expression<Boolean> x) {
		return wrap( x );
	}

	@Nonnull
	@Override
	public SqmPredicate isFalse(@Nonnull Expression<Boolean> x) {
		return wrap( x ).not();
	}

	@Nonnull
	@Override
	public SqmPredicate isNull(@Nonnull Expression<?> x) {
		return new SqmNullnessPredicate( (SqmExpression<?>) x, this );
	}

	@Nonnull
	@Override
	public SqmPredicate isNotNull(@Nonnull Expression<?> x) {
		return new SqmNullnessPredicate( (SqmExpression<?>) x, this ).not();
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(@Nonnull Expression<? extends Y> value, @Nonnull Expression<? extends Y> lower, @Nonnull Expression<? extends Y> upper) {
		return new SqmBetweenPredicate(
				(SqmExpression<? extends Y>) value,
				(SqmExpression<? extends Y>) lower,
				(SqmExpression<? extends Y>) upper,
				false,
				this
		);
	}

	@Override
	public SqmPredicate between(Expression<?> value, Expression<?> lower, Expression<?> upper, boolean negated) {
		return new SqmBetweenPredicate(
				(SqmExpression<?>) value,
				(SqmExpression<?>) lower,
				(SqmExpression<?>) upper,
				negated,
				this
		);
	}

	@Override
	public SqmPredicate comparison(Expression<?> x, ComparisonOperator operator, Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				operator,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(@Nonnull Expression<? extends Y> value, @Nullable Y lower, @Nullable Y upper) {
		final var valueExpression = (SqmExpression<? extends Y>) value;
		return new SqmBetweenPredicate(
				valueExpression,
				value( lower, valueExpression ),
				value( upper, valueExpression ),
				false,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate between(
			@Nullable Y value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper) {
		final var lowerExpression = (SqmExpression<? extends Y>) lower;
		return new SqmBetweenPredicate(
				value( value, lowerExpression ),
				lowerExpression,
				(SqmExpression<? extends Y>) upper,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate equal(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate equal(@Nonnull Expression<?> x, @Nullable Object y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate notEqual(@Nonnull Expression<?> x, @Nullable Object y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate distinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.DISTINCT_FROM,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate distinctFrom(@Nonnull Expression<?> x, @Nullable Object y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.DISTINCT_FROM,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_DISTINCT_FROM,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nullable Object y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.NOT_DISTINCT_FROM,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate gt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate gt(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ge(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.GREATER_THAN_OR_EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate lt(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				(SqmExpression<?>) y,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate le(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		final var yExpr = value( y, (SqmExpression<?>) x );
		return new SqmComparisonPredicate(
				(SqmExpression<?>) x,
				ComparisonOperator.LESS_THAN_OR_EQUAL,
				yExpr,
				this
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmPredicate isEmpty(@Nonnull Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<C>) collection, false, this );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmPredicate isNotEmpty(@Nonnull Expression<C> collection) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<C>) collection, true, this );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection) {
		return createSqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, false);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> SqmPredicate isMember(@Nullable E elem, @Nonnull Expression<C> collection) {
		return createSqmMemberOfPredicate( value( elem ), (SqmPath<?>) collection, false);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection) {
		return createSqmMemberOfPredicate( (SqmExpression<?>) elem, (SqmPath<?>) collection, true);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> SqmPredicate isNotMember(@Nullable E elem, @Nonnull Expression<C> collection) {
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

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull Expression<String> pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull String pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				(SqmExpression<?>) escapeChar,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull Expression<String> pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				literal( escapeChar ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				(SqmExpression<?>) escapeChar,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate like(@Nonnull Expression<String> searchString, @Nonnull String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				literal( escapeChar ),
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(@Nonnull Expression<String> searchString, @Nonnull Expression<String> pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(@Nonnull Expression<String> searchString, @Nullable String pattern) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(
			@Nonnull Expression<String> searchString,
			@Nonnull Expression<String> pattern,
			@Nonnull Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				(SqmExpression<?>) escapeChar,
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(@Nonnull Expression<String> searchString, @Nonnull Expression<String> pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				(SqmExpression<?>) pattern,
				literal( escapeChar ),
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(@Nonnull Expression<String> searchString, @Nullable String pattern, @Nonnull Expression<Character> escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				(SqmExpression<?>) escapeChar,
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate ilike(@Nonnull Expression<String> searchString, @Nullable String pattern, char escapeChar) {
		return new SqmLikePredicate(
				(SqmExpression<?>) searchString,
				value( pattern, (SqmExpression<?>) searchString ),
				literal( escapeChar ),
				false,
				false,
				this
		);
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return not( like( x, pattern ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return not( like( x, pattern ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar) {
		return not( like( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return not( ilike( x, pattern ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern) {
		return not( ilike( x, pattern ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, @Nonnull Expression<Character> escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public SqmPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, char escapeChar) {
		return not( ilike( x, pattern, escapeChar ) );
	}

	@Nonnull
	@Override
	public JpaPredicate likeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return new SqmBooleanExpressionPredicate(
				getFunctionDescriptor( "regexp_like" )
						.generateSqmExpression(
								asList( (SqmExpression<String>) x,
										literal( pattern ) ),
								null,
								getQueryEngine()
						),
				this
		);
	}

	@Nonnull
	@Override
	public JpaPredicate ilikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return new SqmBooleanExpressionPredicate(
				getFunctionDescriptor( "regexp_like" )
						.generateSqmExpression(
								asList( (SqmExpression<String>) x,
										literal( pattern ),
										literal( "i" ) ),
								null,
								getQueryEngine()
						),
				this
		);
	}

	@Nonnull
	@Override
	public JpaPredicate notLikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return new SqmBooleanExpressionPredicate(
				getFunctionDescriptor( "regexp_like" )
						.generateSqmExpression(
								asList( (SqmExpression<String>) x,
										literal( pattern ) ),
								null,
								getQueryEngine()
						),
				true,
				this
		);
	}

	@Nonnull
	@Override
	public JpaPredicate notIlikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return new SqmBooleanExpressionPredicate(
				getFunctionDescriptor( "regexp_like" )
						.generateSqmExpression(
								asList( (SqmExpression<String>) x,
										literal( pattern ),
										literal( "i" ) ),
								null,
								getQueryEngine()
						),
				true,
				this
		);
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression) {
		return new SqmInListPredicate<>( (SqmExpression<T>) expression, this );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Expression<? extends T>... values) {
		final List<SqmExpression<T>> listExpressions = new ArrayList<>( values.length );
		for ( var value : values ) {
			listExpressions.add( (SqmExpression<T>) value );
		}
		return new SqmInListPredicate<>( (SqmExpression<T>) expression, listExpressions, this );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull T... values) {
		final var sqmExpression = (SqmExpression<T>) expression;
		final List<SqmExpression<T>> listExpressions = new ArrayList<>( values.length );
		for ( T value : values ) {
			listExpressions.add( value( value, sqmExpression ) );
		}
		return new SqmInListPredicate<>( sqmExpression, listExpressions, this );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Collection<T> values) {
		final var sqmExpression = (SqmExpression<T>) expression;
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

	@Nonnull
	@Override
	public SqmPredicate exists(@Nonnull Subquery<?> subQuery) {
		return new SqmExistsPredicate( (SqmExpression<?>) subQuery, this );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapEmpty(@Nonnull JpaExpression<M> mapExpression) {
		return new SqmEmptinessPredicate( (SqmPluralValuedSimplePath<?>) mapExpression, false, this );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> SqmPredicate isMapNotEmpty(@Nonnull JpaExpression<M> mapExpression) {
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
		CORE_LOGGER.trace( "Resolving serialized SqmCriteriaNodeBuilder" );
		return locateSessionFactoryOnDeserialization( uuid, name ).getCriteriaBuilder();
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name) throws InvalidObjectException{
		final SessionFactory uuidResult = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
		if ( uuidResult != null ) {
			CORE_LOGGER.tracef( "Resolved SessionFactory by UUID [%s]", uuid );
			return uuidResult;
		}

		// in case we were deserialized in a different JVM, look for an instance with the same name
		// (provided we were given a name)
		if ( name != null ) {
			final SessionFactory namedResult = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( namedResult != null ) {
				CORE_LOGGER.tracef( "Resolved SessionFactory by name [%s]", name );
				return namedResult;
			}
		}

		throw new InvalidObjectException( "Could not find a SessionFactory [uuid=" + uuid + ",name=" + name + "]" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-standard HQL functions

	@Nonnull
	@Override
	public <T> SqmFunction<T> sql(@Nonnull String pattern, @Nonnull Class<T> type, @Nonnull Expression<?>... arguments) {
		failIfSafeModeEnabled( safeModeEnabled, "sql", null );
		final List<SqmExpression<?>> sqmArguments = new ArrayList<>( expressionList( arguments ) );
		sqmArguments.add( 0, literal( pattern ) );
		return getFunctionDescriptor( "sql" ).generateSqmExpression(
				sqmArguments,
				getTypeConfiguration().standardBasicTypeForJavaType( type ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> format(@Nonnull Expression<? extends TemporalAccessor> datetime, @Nonnull String pattern) {
		final var sqmFormat = new SqmFormat( pattern, getStringType(), this );
		return getFunctionDescriptor( "format" ).generateSqmExpression(
				asList( (SqmExpression<?>) datetime, sqmFormat ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public <N, T extends Temporal> SqmExpression<N> extract(@Nonnull TemporalField<N, T> field, @Nonnull Expression<T> temporal) {
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

	@Nonnull
	@Override
	public SqmFunction<Integer> year(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.YEAR, Integer.class );
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> month(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.MONTH, Integer.class );
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> day(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.DAY, Integer.class );
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> hour(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.HOUR, Integer.class );
	}

	@Nonnull
	@Override
	public SqmFunction<Integer> minute(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.MINUTE, Integer.class );
	}

	@Nonnull
	@Override
	public SqmFunction<Float> second(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return extract( datetime, TemporalUnit.SECOND, Float.class );
	}

	@Nonnull
	@Override
	public <T extends TemporalAccessor> SqmFunction<T> truncate(@Nonnull Expression<T> datetime, @Nonnull TemporalUnit temporalUnit) {
		return getFunctionDescriptor( "trunc" ).generateSqmExpression(
				asList(
						(SqmTypedNode<?>) datetime,
						new SqmExtractUnit<>( temporalUnit, getIntegerType(), this )
				),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start) {
		return overlay( string, replacement, value( start ), null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(@Nonnull Expression<String> string, @Nonnull Expression<String> replacement, int start) {
		return overlay( string, replacement, value( start ), null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, @Nonnull Expression<Integer> start) {
		return overlay( string, value( replacement ), start, null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start) {
		return overlay( string, replacement, start, null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start, int length) {
		return overlay( string, value( replacement ), value( start ), value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			int start,
			int length) {
		return overlay( string, replacement, value( start ), value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			@Nonnull Expression<Integer> start,
			int length) {
		return overlay( string, value( replacement ), start, value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			int length) {
		return overlay( string, replacement, start, value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			int start,
			@Nullable Expression<Integer> length) {
		return overlay( string, value( replacement ), value( start ), length );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			int start,
			@Nullable Expression<Integer> length) {
		return overlay( string, replacement, value( start ), length );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length) {
		return overlay( string, value( replacement ), start, length );
	}

	@Nonnull
	@Override
	public SqmFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length) {
		final var sqmString = (SqmExpression<String>) string;
		final var sqmReplacement = (SqmExpression<String>) replacement;
		final var sqmStart = (SqmExpression<Integer>) start;
		return getFunctionDescriptor( "overlay" ).generateSqmExpression(
				( length == null
						? asList( sqmString, sqmReplacement, sqmStart )
						: asList( sqmString, sqmReplacement, sqmStart, (SqmExpression<Integer>) length ) ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, int length) {
		return pad( null, x, value( length ), null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length) {
		return pad( ts, x, value( length ), null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return pad( null, x, length, null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return pad( ts, x, length, null );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, int length, char padChar) {
		return pad( null, x, value( length ), value( padChar ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, char padChar) {
		return pad( ts, x, value( length ), value( padChar ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar) {
		return pad( null, x, value( length ), padChar );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar) {
		return pad( ts, x, value( length ), padChar );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar) {
		return pad( null, x, length, value( padChar ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar) {
		return pad( ts, x, length, value( padChar ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, @Nullable Expression<Character> padChar) {
		return pad( null, x, length, padChar );
	}

	@Nonnull
	@Override
	public SqmFunction<String> pad(
			@Nullable Trimspec ts,
			@Nonnull Expression<String> x,
			@Nonnull Expression<Integer> length,
			@Nullable Expression<Character> padChar) {
		final var source = (SqmExpression<String>) x;
		final var sqmLength = (SqmExpression<Integer>) length;
		final var padSpec = new SqmTrimSpecification(
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

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nonnull Expression<String> x, @Nonnull Expression<Integer> times) {
		return getFunctionDescriptor( "repeat" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) times ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nonnull Expression<String> x, int times) {
		return repeat( x, value( times ) );
	}

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nullable String x, @Nonnull Expression<Integer> times) {
		return repeat( value( x), times );
	}

	@Nonnull
	@Override
	public SqmFunction<String> left(@Nonnull Expression<String> x, int length) {
		return left( x, value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> left(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return getFunctionDescriptor( "left" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) length ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> right(@Nonnull Expression<String> x, int length) {
		return right( x, value( length ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> right(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return getFunctionDescriptor( "right" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, (SqmExpression<Integer>) length ),
				null,
				getQueryEngine()
		);
	}

	@Nonnull
	@Override
	public SqmFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull String replacement) {
		SqmExpression<String> sqmPattern = value( pattern );
		return replace( x, sqmPattern, value( replacement, sqmPattern ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<String> replacement) {
		return replace( x, value( pattern ), replacement );
	}

	@Nonnull
	@Override
	public SqmFunction<String> replace(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull String replacement) {
		return replace( x, pattern, value( replacement ) );
	}

	@Nonnull
	@Override
	public SqmFunction<String> replace(
			@Nonnull Expression<String> x,
			@Nonnull Expression<String> pattern,
			@Nonnull Expression<String> replacement) {
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

	@Nonnull
	@Override
	public SqmFunction<String> collate(@Nonnull Expression<String> x, @Nonnull String collation) {
		final SqmCollation sqmCollation = new SqmCollation( collation, null, this );
		return getFunctionDescriptor( "collate" ).generateSqmExpression(
				asList( (SqmExpression<String>) x, sqmCollation ),
				null,
				getQueryEngine()
		);
	}


	@Nonnull
	@Override
	public SqmFunction<Double> log10(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "log10" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> log(@Nullable Number b, @Nonnull Expression<? extends Number> x) {
		return log( value( b ), x );
	}

	@Nonnull
	@Override
	public SqmFunction<Double> log(@Nonnull Expression<? extends Number> b, @Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "log" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) b, (SqmTypedNode<?>) x ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> pi() {
		return getFunctionDescriptor( "pi" ).generateSqmExpression(
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> sin(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "sin" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> cos(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "cos" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> tan(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "tan" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> asin(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "asin" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> acos(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "acos" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> atan(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "atan" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> atan2(@Nullable Number y, @Nonnull Expression<? extends Number> x) {
		return atan2( value( y ), x );
	}

	@Nonnull
	@Override
	public SqmFunction<Double> atan2(@Nonnull Expression<? extends Number> y, @Nullable Number x) {
		return atan2( y, value( x ) );
	}

	@Nonnull
	@Override
	public SqmFunction<Double> atan2(@Nonnull Expression<? extends Number> y, @Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "atan2" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) y, (SqmTypedNode<?>) x ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> sinh(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "sinh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> cosh(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "cosh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> tanh(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "tanh" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> degrees(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "degrees" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmFunction<Double> radians(@Nonnull Expression<? extends Number> x) {
		return getFunctionDescriptor( "radians" ).generateSqmExpression(
				(SqmTypedNode<?>) x,
				null,
				queryEngine
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Window functions

	@Nonnull
	@Override
	public SqmWindow createWindow() {
		return new SqmWindow( this );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameUnboundedPreceding() {
		return new SqmWindowFrame( this, FrameKind.UNBOUNDED_PRECEDING );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameBetweenPreceding(int offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_PRECEDING, literal( offset ) );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameBetweenPreceding(@Nonnull Expression<?> offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_PRECEDING, (SqmExpression<?>) offset );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameCurrentRow() {
		return new SqmWindowFrame( this, FrameKind.CURRENT_ROW );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameBetweenFollowing(int offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_FOLLOWING, literal( offset ) );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameBetweenFollowing(@Nonnull Expression<?> offset) {
		return new SqmWindowFrame( this, FrameKind.OFFSET_FOLLOWING, (SqmExpression<?>) offset );
	}

	@Nonnull
	@Override
	public SqmWindowFrame frameUnboundedFollowing() {
		return new SqmWindowFrame( this, FrameKind.UNBOUNDED_FOLLOWING );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> windowFunction(@Nonnull String name, @Nullable Class<T> type, @Nonnull JpaWindow window, @Nonnull Expression<?>... args) {
		SqmExpression<T> function = getFunctionDescriptor( name ).generateSqmExpression(
				expressionList( args ),
				null,
				queryEngine
		);
		return new SqmOver<>( function, (SqmWindow) window );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rowNumber(@Nonnull JpaWindow window) {
		return windowFunction( "row_number", Long.class, window );
	}


	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> firstValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "first_value", ((SqmExpression<T>) argument).getJavaTypeIfKnown(), window, argument );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> lastValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "last_value", ((SqmExpression<T>) argument).getJavaTypeIfKnown(), window, argument );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> nthValue(@Nonnull Expression<T> argument, int n, @Nonnull JpaWindow window) {
		return nthValue( argument, literal( n ), window );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> nthValue(@Nonnull Expression<T> argument, @Nonnull Expression<Integer> n, @Nonnull JpaWindow window) {
		return (SqmExpression<T>) windowFunction( "nth_value", ((SqmExpression<T>) argument).getJavaTypeIfKnown(), window, argument, n );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rank(@Nonnull JpaWindow window) {
		return windowFunction( "rank", Long.class, window );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> denseRank(@Nonnull JpaWindow window) {
		return windowFunction( "dense_rank", Long.class, window );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> percentRank(@Nonnull JpaWindow window) {
		return windowFunction( "percent_rank", Double.class, window );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> cumeDist(@Nonnull JpaWindow window) {
		return windowFunction( "cume_dist", Double.class, window );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Aggregate functions

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args) {
		return functionAggregate( name, type, filter, null, args );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		return functionAggregate( name, type, null, window, args );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
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

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter) {
		return sum( argument, filter, null );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaWindow window) {
		return sum( argument, null, window );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return functionAggregate( "sum", Number.class, filter, window, argument );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter) {
		return avg( argument, filter, null );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaWindow window) {
		return avg( argument, null, window );
	}

	@Nonnull
	@Override
	public <N extends Number> SqmExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return functionAggregate( "avg", Double.class, filter, window, argument );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter) {
		return count( argument, filter, null );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaWindow window) {
		return count( argument, null, window );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return functionAggregate( "count", Long.class, filter, window, argument );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordered-Set Aggregate functions

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionWithinGroup(@Nonnull String name, @Nullable Class<T> type, @Nullable JpaOrder order, @Nonnull Expression<?>... args) {
		return functionWithinGroup( name, type, order, null, null, args );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args) {
		return functionWithinGroup( name, type, order, filter, null, args );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		return functionWithinGroup( name, type, order, null, window, args );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		final var withinGroupClause = new SqmOrderByClause();
		if ( order != null ) {
			withinGroupClause.addSortSpecification( (SqmSortSpecification) order );
		}
		final var sqmFilter = filter != null ? (SqmPredicate) filter : null;
		final SqmExpression<T> function =
				getFunctionDescriptor( name )
						.generateOrderedSetAggregateSqmExpression(
								expressionList( args ),
								sqmFilter,
								withinGroupClause,
								null,
								queryEngine
						);
		return window == null ? function : new SqmOver<>( function, (SqmWindow) window );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull String separator) {
		return listagg( order, null, null, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull Expression<String> separator) {
		return listagg( order, null, null, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return listagg( order, filter, null, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return listagg( order, filter, null, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return listagg( order, null, window, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return listagg( order, null, window, argument, separator );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return listagg( order, filter, window, argument, literal( separator ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return functionWithinGroup( "listagg", String.class, order, filter, window, argument, separator );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> mode(@Nonnull Expression<T> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence) {
		return mode( null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return mode( filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> mode(
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return mode( null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"mode",
				((SqmExpression<T>) sortExpression).getJavaTypeIfKnown(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileCont( argument, null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileCont( argument, filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileCont( argument, null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"percentile_cont",
				((SqmExpression<T>) sortExpression).getJavaTypeIfKnown(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window,
				argument
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileDisc( argument, null, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileDisc( argument, filter, null, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return percentileDisc( argument, null, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return (SqmExpression<T>) functionWithinGroup(
				"percentile_disc",
				((SqmExpression<T>) sortExpression).getJavaTypeIfKnown(),
				sort( (SqmExpression<T>) sortExpression, sortOrder, nullPrecedence ),
				filter,
				window,
				argument
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, null, null, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, filter, null, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, null, window, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return functionWithinGroup( "rank", Long.class, order, filter, window, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> percentRank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments) {
		return percentRank( order, null, null, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments) {
		return percentRank( order, filter, null, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return percentRank( order, null, window, arguments );
	}

	@Nonnull
	@Override
	public SqmExpression<Double> percentRank(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... arguments) {
		return functionWithinGroup( "percent_rank", Double.class, order, filter, window, arguments );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nonnull Expression<? extends T> argument) {
		return arrayAgg( order, null, null, argument );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<? extends T> argument) {
		return arrayAgg( order, filter, null, argument );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<? extends T> argument) {
		return arrayAgg( order, null, window, argument );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAgg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<? extends T> argument) {
		return functionWithinGroup( "array_agg", null, order, filter, window, argument );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayLiteral(@Nullable T... elements) {
		return getFunctionDescriptor( "array" ).generateSqmExpression(
				literals( elements ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Integer> arrayPosition(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<int[]> arrayPositions(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<List<Integer>> arrayPositionsList(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Integer> arrayLength(@Nonnull Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_length" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayConcat(
			@Nonnull Expression<T[]> arrayExpression1,
			@Nonnull Expression<T[]> arrayExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayConcat(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayPrepend(@Nonnull Expression<T> elementExpression, @Nonnull Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayPrepend(@Nullable T element, @Nonnull Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContains(@Nullable T[] array, @Nonnull Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( value( array ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContainsNullable(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayContainsNullable(@Nullable T[] array, @Nonnull Expression<T> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( value( array ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludes(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( subArray, (SqmExpression<?>) arrayExpression ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludes(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( value( array, (SqmExpression<?>) subArrayExpression ), (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludesNullable(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( subArray, (SqmExpression<?>) arrayExpression ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIncludesNullable(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( value( array, (SqmExpression<?>) subArrayExpression ), (SqmExpression<?>) subArrayExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersects(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersectsNullable(
			@Nonnull Expression<T[]> arrayExpression1,
			@Nonnull Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression1, value( array2, (SqmExpression<?>) arrayExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmPredicate arrayIntersectsNullable(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( value( array1, (SqmExpression<?>) arrayExpression2 ), (SqmExpression<?>) arrayExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nullable T element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable Integer index,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nullable T element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ), value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayRemoveIndex(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) lowerIndexExpression, (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nullable Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) lowerIndexExpression, value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable Integer lowerIndex,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( lowerIndex ), (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable Integer lowerIndex,
			@Nullable Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( lowerIndex ), value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> oldElementExpression,
			@Nonnull Expression<T> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) oldElementExpression, (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> oldElementExpression,
			@Nullable T newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) oldElementExpression, value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable T oldElement,
			@Nonnull Expression<T> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( oldElement ), (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nullable T newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( oldElement ), value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayTrim(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayReverse(@Nonnull Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_reverse" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) arrayExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( descending ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) descendingExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( descending ), value( nullsFirst ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arraySort(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList(
						(SqmExpression<?>) arrayExpression,
						(SqmExpression<?>) descendingExpression,
						(SqmExpression<?>) nullsFirstExpression
				),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer elementCount) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayFill(
			@Nonnull Expression<T> elementExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T[]> arrayFill(@Nullable T element, @Nullable Integer elementCount) {
		return getFunctionDescriptor( "array_fill" ).generateSqmExpression(
				asList( value( element ), value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(
			@Nonnull Expression<? extends Object[]> arrayExpression,
			@Nonnull Expression<String> separatorExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) separatorExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(
			@Nonnull Expression<? extends Object[]> arrayExpression,
			@Nullable String separator) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, value( separator ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) arrayExpression, (SqmExpression<?>) separatorExpression, (SqmExpression<?>) defaultExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue) {
		return arrayToString( arrayExpression, separatorExpression, value( defaultValue ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression) {
		return arrayToString( arrayExpression, value( separator ), defaultExpression );
	}

	@Nonnull
	@Override
	public SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nullable String defaultValue) {
		return arrayToString( arrayExpression, value( separator ), value( defaultValue ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types


	@Nonnull
	@Override
	public <E, C extends Collection<E>> SqmExpression<C> collectionLiteral(@Nullable E... elements) {
		return getFunctionDescriptor( "array_list" ).generateSqmExpression(
				literals( elements ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmExpression<Integer> collectionPosition(
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression,
			@Nullable E element) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmExpression<Integer> collectionPosition(
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression,
			@Nonnull Expression<E> elementExpression) {
		return getFunctionDescriptor( "array_position" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<int[]> collectionPositions(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<int[]> collectionPositions(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nullable T element) {
		return getFunctionDescriptor( "array_positions" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<List<Integer>> collectionPositionsList(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nonnull Expression<T> elementExpression) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<List<Integer>> collectionPositionsList(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nullable T element) {
		return getFunctionDescriptor( "array_positions_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Integer> collectionLength(@Nonnull Expression<? extends Collection<?>> collectionExpression) {
		return getFunctionDescriptor( "array_length" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			@Nonnull Expression<C> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			@Nonnull Expression<C> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(
			@Nullable C collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return getFunctionDescriptor( "array_concat" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E element) {
		return getFunctionDescriptor( "array_append" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(
			@Nonnull Expression<? extends E> elementExpression,
			@Nonnull Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(
			@Nullable E element,
			@Nonnull Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_prepend" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContains(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContains(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable E element) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContains(
			@Nonnull Collection<E> collection,
			@Nonnull Expression<E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains" ).generateSqmExpression(
				asList( collectionValue( collection, (SqmExpression<E>) elementExpression ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContainsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContainsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable E element) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionContainsNullable(
			@Nonnull Collection<E> collection,
			@Nonnull Expression<E> elementExpression) {
		return isTrue( getFunctionDescriptor( "array_contains_nullable" ).generateSqmExpression(
				asList( collectionValue( collection, (SqmExpression<E>) elementExpression ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludes(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludes(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable Collection<? extends E> subCollection) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( subCollection, (SqmExpression<?>) collectionExpression ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludes(
			@Nullable Collection<E> collection,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes" ).generateSqmExpression(
				asList( value( collection, (SqmExpression<?>) subCollectionExpression ), (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable Collection<? extends E> subCollection) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( subCollection, (SqmExpression<?>) collectionExpression ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIncludesNullable(
			@Nullable Collection<E> collection,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return isTrue( getFunctionDescriptor( "array_includes_nullable" ).generateSqmExpression(
				asList( value( collection, (SqmExpression<?>) subCollectionExpression ), (SqmExpression<?>) subCollectionExpression ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersects(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersects(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersects(
			@Nullable Collection<E> collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression1, value( collection2, (SqmExpression<?>) collectionExpression1 ) ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmPredicate collectionIntersectsNullable(
			@Nullable Collection<E> collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return isTrue( getFunctionDescriptor( "array_intersects_nullable" ).generateSqmExpression(
				asList( value( collection1, (SqmExpression<?>) collectionExpression2 ), (SqmExpression<?>) collectionExpression2 ),
				null,
				queryEngine
		) );
	}

	@Nonnull
	@Override
	public <E> SqmExpression<E> collectionGet(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Integer index) {
		return getFunctionDescriptor( "array_get" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nullable E element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index,
			@Nonnull Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ), (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index,
			@Nullable E element) {
		return getFunctionDescriptor( "array_set" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ), value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) elementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E element) {
		return getFunctionDescriptor( "array_remove" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( element ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index) {
		return getFunctionDescriptor( "array_remove_index" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) lowerIndexExpression, (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nullable Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) lowerIndexExpression, value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer lowerIndex,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( lowerIndex ), (SqmExpression<?>) upperIndexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer lowerIndex,
			@Nullable Integer upperIndex) {
		return getFunctionDescriptor( "array_slice" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( lowerIndex ), value( upperIndex ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> oldElementExpression,
			@Nonnull Expression<? extends E> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) oldElementExpression, (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> oldElementExpression,
			@Nullable E newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) oldElementExpression, value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E oldElement,
			@Nonnull Expression<? extends E> newElementExpression) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( oldElement ), (SqmExpression<?>) newElementExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E oldElement,
			@Nullable E newElement) {
		return getFunctionDescriptor( "array_replace" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( oldElement ), value( newElement ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionTrim(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) indexExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionTrim(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index) {
		return getFunctionDescriptor( "array_trim" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( index ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionReverse(@Nonnull Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_reverse" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				Collections.singletonList( (SqmExpression<?>) collectionExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( descending ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) descendingExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( descending ), value( nullsFirst ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression) {
		return getFunctionDescriptor( "array_sort" ).generateSqmExpression(
				asList(
						(SqmExpression<?>) collectionExpression,
						(SqmExpression<?>) descendingExpression,
						(SqmExpression<?>) nullsFirstExpression
				),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(
			@Nonnull Expression<T> elementExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( (SqmExpression<?>) elementExpression, value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( value( element ), (SqmExpression<?>) elementCountExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<Collection<T>> collectionFill(@Nullable T element, @Nullable Integer elementCount) {
		return getFunctionDescriptor( "array_fill_list" ).generateSqmExpression(
				asList( value( element ), value( elementCount ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(
			@Nonnull Expression<? extends Collection<?>> collectionExpression,
			@Nonnull Expression<String> separatorExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) separatorExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(
			@Nonnull Expression<? extends Collection<?>> collectionExpression,
			@Nullable String separator) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, value( separator ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression) {
		return getFunctionDescriptor( "array_to_string" ).generateSqmExpression(
				asList( (SqmExpression<?>) collectionExpression, (SqmExpression<?>) separatorExpression, (SqmExpression<?>) defaultExpression ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue) {
		return collectionToString( collectionExpression, separatorExpression, value( defaultValue ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression) {
		return collectionToString( collectionExpression, value( separator ), defaultExpression );
	}

	@Nonnull
	@Override
	public SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nullable String defaultValue) {
		return collectionToString( collectionExpression, value( separator ), value( defaultValue ) );
	}

	@Nonnull
	@Override
	public SqmJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return jsonValue( jsonDocument, value( jsonPath ), null );
	}

	@Nonnull
	@Override
	public <T> SqmJsonValueExpression<T> jsonValue(
			@Nonnull Expression<?> jsonDocument,
			@Nullable String jsonPath,
			@Nullable Class<T> returningType) {
		return jsonValue( jsonDocument, value( jsonPath ), returningType );
	}

	@Nonnull
	@Override
	public SqmJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return jsonValue( jsonDocument, jsonPath, null );
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> SqmJsonValueExpression<T> jsonValue(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nullable Class<T> returningType) {
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

	@Nonnull
	@Override
	public SqmJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return jsonQuery( jsonDocument, value( jsonPath ) );
	}

	@Nonnull
	@Override
	public SqmJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return (SqmJsonQueryExpression) getFunctionDescriptor( "json_query" ).<String>generateSqmExpression(
				asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return jsonExists( jsonDocument, value( jsonPath ) );
	}

	@Nonnull
	@Override
	public SqmJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return (SqmJsonExistsExpression) getFunctionDescriptor( "json_exists" ).<Boolean>generateSqmExpression(
				asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayWithNulls(@Nonnull Expression<?>... values) {
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

	@Nonnull
	@Override
	public SqmExpression<String> jsonArray(@Nonnull Expression<?>... values) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_array" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( values ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, null, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, (SqmPredicate) filter, orderByClause( orderBy ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, (SqmPredicate) filter, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, null, null, orderByClause( orderBy ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, null, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy) {
		return jsonArrayAgg(
				(SqmExpression<?>) value,
				SqmJsonNullBehavior.NULL,
				(SqmPredicate) filter,
				orderByClause( orderBy )
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, (SqmPredicate) filter, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy) {
		return jsonArrayAgg( (SqmExpression<?>) value, SqmJsonNullBehavior.NULL, null, orderByClause( orderBy ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, SqmJsonObjectAggUniqueKeysBehavior.WITH, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return jsonObjectAgg( key, value, null, SqmJsonObjectAggUniqueKeysBehavior.WITH, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, null, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return jsonObjectAgg( key, value, null, null, null );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(
			@Nonnull Expression<?> key,
			@Nonnull Expression<?> value,
			@Nullable Predicate filter) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, SqmJsonObjectAggUniqueKeysBehavior.WITH, filter );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
		return jsonObjectAgg( key, value, null, SqmJsonObjectAggUniqueKeysBehavior.WITH, filter );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
		return jsonObjectAgg( key, value, SqmJsonNullBehavior.NULL, null, filter );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
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

	@Nonnull
	@Override
	public SqmExpression<String> jsonObjectWithNulls(@Nonnull Map<?, ? extends Expression<?>> keyValues) {
		final var arguments = keyValuesAsAlternatingList( keyValues );
		arguments.add( SqmJsonNullBehavior.NULL );
		return getFunctionDescriptor( "json_object" ).generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonObject(@Nonnull Map<?, ? extends Expression<?>> keyValues) {
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

	@Nonnull
	@Override
	public SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return jsonSet( jsonDocument, jsonPath, value( value ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return jsonSet( jsonDocument, value( jsonPath ), value( value ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return jsonSet( jsonDocument, value( jsonPath ), value );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_set" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return jsonRemove( jsonDocument, value( jsonPath ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_remove" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return jsonInsert( jsonDocument, jsonPath, value( value ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return jsonInsert( jsonDocument, value( jsonPath ), value );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return jsonInsert( jsonDocument, value( jsonPath ), value );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonInsert(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nonnull Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_insert" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return jsonReplace( jsonDocument, jsonPath, value( value ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return jsonReplace( jsonDocument, value( jsonPath ), value );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return jsonReplace( jsonDocument, value( jsonPath ), value );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonReplace(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nonnull Expression<?> value) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_replace" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( jsonDocument, jsonPath, value ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonMergepatch(@Nullable String document, @Nonnull Expression<?> patch) {
		return jsonMergepatch( value( document ), patch );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nullable String patch) {
		return jsonMergepatch( document, value( patch ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nonnull Expression<?> patch) {
		//noinspection unchecked
		return getFunctionDescriptor( "json_mergepatch" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) (List<?>) asList( document, patch ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmXmlElementExpression xmlelement(@Nonnull String elementName) {
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( 3 );
		arguments.add( new SqmLiteral<>( elementName, getStringType(), this ) );
		return (SqmXmlElementExpression) getFunctionDescriptor( "xmlelement" ).<String>generateSqmExpression(
				arguments,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlcomment(@Nullable String comment) {
		return getFunctionDescriptor( "xmlcomment" ).generateSqmExpression(
				List.of( value( comment ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <T> SqmExpression<T> named(@Nonnull Expression<T> expression, @Nonnull String name) {
		return new SqmNamedExpression<>( (SqmExpression<T>) expression, name );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlforest(@Nonnull Expression<?>... elements) {
		return xmlforest( asList( elements ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlforest(@Nonnull List<? extends Expression<?>> elements) {
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

	@Nonnull
	@Override
	public SqmExpression<String> xmlconcat(@Nonnull Expression<?>... elements) {
		return xmlconcat( asList( elements ) );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlconcat(@Nonnull List<? extends Expression<?>> elements) {
		return getFunctionDescriptor( "xmlforest" ).generateSqmExpression(
				(List<? extends SqmTypedNode<?>>) elements,
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlpi(@Nonnull String elementName) {
		return getFunctionDescriptor( "xmlpi" ).generateSqmExpression(
				Collections.singletonList( literal( elementName ) ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlpi(@Nonnull String elementName, @Nonnull Expression<String> content) {
		return getFunctionDescriptor( "xmlpi" ).generateSqmExpression(
				asList( literal( elementName ), (SqmTypedNode<?>) content ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlquery(@Nullable String query, @Nonnull Expression<?> xmlDocument) {
		return xmlquery( value( query ), xmlDocument );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlquery(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument) {
		return getFunctionDescriptor( "xmlquery" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) query, (SqmTypedNode<?>) xmlDocument ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<Boolean> xmlexists(@Nullable String query, @Nonnull Expression<?> xmlDocument) {
		return xmlexists( value( query ), xmlDocument );
	}

	@Nonnull
	@Override
	public SqmExpression<Boolean> xmlexists(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument) {
		return getFunctionDescriptor( "xmlexists" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) query, (SqmTypedNode<?>) xmlDocument ),
				null,
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nonnull Expression<?> argument) {
		return xmlagg( order, null, null, argument );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?> argument) {
		return xmlagg( order, filter, null, argument );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?> argument) {
		return xmlagg( order, null, window, argument );
	}

	@Nonnull
	@Override
	public SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?> argument) {
		return functionWithinGroup( "xmlagg", String.class, order, filter, window, argument );
	}

	@Nonnull
	@Override
	public <E> SqmSetReturningFunction<E> setReturningFunction(@Nonnull String name, @Nonnull Expression<?>... args) {
		return getSetReturningFunctionDescriptor( name ).generateSqmExpression(
				expressionList( args ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmSetReturningFunction<E> unnestArray(@Nonnull Expression<E[]> array) {
		return getSetReturningFunctionDescriptor( "unnest" ).generateSqmExpression(
				Collections.singletonList( (SqmTypedNode<?>) array ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E> SqmSetReturningFunction<E> unnestCollection(@Nonnull Expression<? extends Collection<E>> collection) {
		return getSetReturningFunctionDescriptor( "unnest" ).generateSqmExpression(
				Collections.singletonList( (SqmTypedNode<?>) collection ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop, (SqmTypedNode<?>) step ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nullable TemporalAmount step) {
		return generateTimeSeries( value( start ), value( stop ), value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step) {
		return generateTimeSeries( value( start ), stop, value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable TemporalAmount step) {
		return generateTimeSeries( start, value( stop ), value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step) {
		return generateTimeSeries( start, stop, value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( value( start ), value( stop ), step );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( start, value( stop ), step );
	}

	@Nonnull
	@Override
	public <E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return generateTimeSeries( value( start ), stop, step );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop, (SqmTypedNode<?>) step ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nullable E step) {
		return generateSeries( value( start ), value( stop ), value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<E> step) {
		return generateSeries( value( start ), value( stop ), step );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable E step) {
		return generateSeries( start, value( stop ), value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable E step) {
		return generateSeries( value( start ), stop, value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable E step) {
		return generateSeries( start, stop, value( step ) );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<E> step) {
		return generateSeries( start, value( stop ), step );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step) {
		return generateSeries( value( start ), stop, step );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop) {
		return getSetReturningFunctionDescriptor( "generate_series" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) start, (SqmTypedNode<?>) stop ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop) {
		return generateSeries( start, value( stop ) );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop) {
		return generateSeries( value( start ), stop );
	}

	@Nonnull
	@Override
	public <E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop) {
		return generateSeries( value( start ), value( stop ) );
	}

	@Nonnull
	@Override
	public SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument) {
		return jsonTable( jsonDocument, (Expression<String>) null );
	}

	@Nonnull
	@Override
	public SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return jsonTable( jsonDocument, value( jsonPath ) );
	}

	@Nonnull
	@Override
	public SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable Expression<String> jsonPath) {
		return (SqmJsonTableFunction<?>) getSetReturningFunctionDescriptor( "json_table" ).generateSqmExpression(
				jsonPath == null
						? Collections.singletonList( (SqmTypedNode<?>) jsonDocument )
						: asList( (SqmTypedNode<?>) jsonDocument, (SqmTypedNode<?>) jsonPath ),
				queryEngine
		);
	}

	@Nonnull
	@Override
	public SqmXmlTableFunction<?> xmlTable(@Nullable String xpath, @Nonnull Expression<?> xmlDocument) {
		return xmlTable( value( xpath ), xmlDocument );
	}

	@Nonnull
	@Override
	public SqmXmlTableFunction<?> xmlTable(@Nonnull Expression<String> xpath, @Nonnull Expression<?> xmlDocument) {
		return (SqmXmlTableFunction<?>) getSetReturningFunctionDescriptor( "xmltable" ).generateSqmExpression(
				asList( (SqmTypedNode<?>) xpath, (SqmTypedNode<?>) xmlDocument ),
				queryEngine
		);
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> least(Expression<C> x, Expression<C> y) {
		return getFunctionDescriptor( "least" )
				.generateSqmExpression( List.of( (SqmTypedNode<?>) x, (SqmTypedNode<?>) y), null, queryEngine);
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> least(C x, Expression<C> y) {
		return getFunctionDescriptor( "least" )
				.generateSqmExpression( List.of( literal(x), (SqmTypedNode<?>) y), null, queryEngine);
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> greatest(Expression<C> x, Expression<C> y) {
		return getFunctionDescriptor( "greatest" )
				.generateSqmExpression( List.of( (SqmTypedNode<?>) x, (SqmTypedNode<?>) y), null, queryEngine);
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> greatest(C x, Expression<C> y) {
		return getFunctionDescriptor( "greatest" )
				.generateSqmExpression( List.of( literal(x), (SqmTypedNode<?>) y), null, queryEngine);
	}
}
