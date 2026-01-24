/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.UnknownProfileException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.spi.NativeQueryArrayTransformer;
import org.hibernate.jpa.spi.NativeQueryConstructorTransformer;
import org.hibernate.jpa.spi.NativeQueryListTransformer;
import org.hibernate.jpa.spi.NativeQueryMapTransformer;
import org.hibernate.jpa.spi.NativeQueryTupleTransformer;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Page;
import org.hibernate.query.PathException;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.internal.NativeMutationMementoImpl;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;
import org.hibernate.query.results.internal.Builders;
import org.hibernate.query.results.internal.ResultSetMappingImpl;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderBasicStandard;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityCalculated;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderEntityStandard;
import org.hibernate.query.results.internal.dynamic.DynamicResultBuilderInstantiation;
import org.hibernate.query.results.internal.implicit.ImplicitModelPartResultBuilderEntity;
import org.hibernate.query.results.internal.implicit.ImplicitResultClassBuilder;
import org.hibernate.query.results.internal.jpa.JpaMappingHelper;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.NonSelectInterpretationsKey;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sql.spi.SelectInterpretationsKey;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.spi.SingleResultConsumer;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.Character.isWhitespace;
import static java.util.Collections.addAll;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.isClass;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.makeCopy;
import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;
import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.hibernate.query.results.internal.Builders.resultClassBuilder;
import static org.hibernate.query.results.spi.ResultSetMapping.resolveResultSetMapping;
import static org.hibernate.query.sqm.internal.SqmUtil.isResultTypeAlwaysAllowed;
import static org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard.isStandardRenderer;

/**
 * @author Steve Ebersole
 */
public class NativeQueryImpl<R>
		extends AbstractQuery<R>
		implements NativeQueryImplementor<R>, DomainQueryExecutionContext, ResultSetMappingResolutionContext {

	private final String sqlString;
	private final String originalSqlString;
	private final ParameterMetadataImplementor parameterMetadata;
	private final List<ParameterOccurrence> parameterOccurrences;
	private final QueryParameterBindings parameterBindings;

	private final Class<R> resultType;
	private final ResultSetMapping resultSetMapping;
	private final boolean resultMappingSuppliedToCtor;
	private final HashMap<String, EntityMappingType> entityMappingTypeByTableAlias = new HashMap<>();

	private Boolean startsWithSelect;
	private Set<String> querySpaces;
	private Callback callback;

	/**
	 * The simplest, though trickiest, constructor form.
	 * We will ultimately need to make a determination (guess), if possible,
	 * whether the query is a selection or mutation.
	 *
	 * @see org.hibernate.SharedSessionContract#createNativeQuery(String)
	 * @see #isSelectQuery
	 */
	public NativeQueryImpl(String incomingSqlString, SharedSessionContractImplementor session) {
		super( session );

		originalSqlString = incomingSqlString;
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( incomingSqlString, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		// We don't know (yet).  Create an "implicit" ResultSetMapping...
		//noinspection unchecked
		resultType = (Class<R>) Object.class;
		var resultSetMappingProducer = session.getFactory().getJdbcValuesMappingProducerProvider();
		resultSetMapping = resultSetMappingProducer.buildResultSetMapping( sqlString, true, session.getFactory() );
		resultMappingSuppliedToCtor = false;
	}

	/**
	 * Dynamic mutation native query.  Here, we know the query is a mutation up front.
	 *
	 * @param flag The parameter value is not used.  It is just used to differentiate
	 * between other constructors.
	 *
	 * @see org.hibernate.SharedSessionContract#createNativeStatement(String)
	 */
	public NativeQueryImpl(String sql, boolean flag, SharedSessionContractImplementor session) {
		super( session );

		originalSqlString = sql;
		startsWithSelect = false;
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( sql, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		resultType = null;
		resultSetMapping = MutationResultSetMapping.INSTANCE;
		resultMappingSuppliedToCtor = false;
	}

	/**
	 * Used from selection native query memento.  We know this is a selection.
	 *
	 * @param selectionMemento The named query or stored reference memento.
	 * @param specifiedResultType The type specified on the API call at
	 * 		{@linkplain org.hibernate.SharedSessionContract#createNamedQuery(String, Class)}
	 * 		or {@linkplain org.hibernate.SharedSessionContract#createNamedQuery(String, String, Class)}.
	 * 		May be {@code null}.
	 * @param specifiedResultSetMappingName The result-set mapping name specified on the API call at
	 * 		{@linkplain org.hibernate.SharedSessionContract#createNamedQuery(String, String)}
	 * 		or {@linkplain org.hibernate.SharedSessionContract#createNamedQuery(String, String, Class)}.
	 * 		May be {@code null}.
	 *
	 * @apiNote We expect one of or both of {@code specifiedResultType} and
	 * {@code specifiedResultSetMappingName} to be non-null.
	 *
	 * @see org.hibernate.SharedSessionContract#createNamedQuery(String)
	 * @see org.hibernate.SharedSessionContract#createNamedQuery(String, Class)
	 * @see org.hibernate.SharedSessionContract#createNamedQuery(String, String)
	 * @see org.hibernate.SharedSessionContract#createNamedQuery(String, String, Class)
	 *
	 * @see #NativeQueryImpl(NativeSelectionMementoImpl, Supplier, ResultSetMappingPopulator, Class, SharedSessionContractImplementor)
	 */
	public NativeQueryImpl(
			NativeSelectionMementoImpl<?> selectionMemento,
			@Nullable Class<R> specifiedResultType,
			@Nullable String specifiedResultSetMappingName,
			SharedSessionContractImplementor session) {
		this(
				selectionMemento,
				() -> {
					if ( specifiedResultType == null && specifiedResultSetMappingName == null ) {
						// We have neither - SharedSessionContract#createNamedQuery(String) form was called.
						// Use what's on the selection memento
						return buildResultSetMapping( getResultSetMappingName( selectionMemento ), false, session );
					}
					else if ( specifiedResultSetMappingName != null ) {
						// One of the forms passing an explicit result-set mapping name was used -
						//		- SharedSessionContract#createNamedQuery(String, String)
						//		- SharedSessionContract#createNamedQuery(String, String, Class)
						// Use specified explicit name
						return buildResultSetMapping( specifiedResultSetMappingName, false, session );
					}
					else {
						final String mappingIdentifier = specifiedResultType.getName();
						return buildResultSetMapping( mappingIdentifier, false, session );
					}
				},
				(resultSetMapping, querySpaceConsumer, context) -> {
					if ( specifiedResultSetMappingName != null ) {
						populateResultSetMappingFromSpecifiedName( specifiedResultSetMappingName, resultSetMapping, querySpaceConsumer, context, session );
						return true;
					}
					else {
						return populateResultSetMappingFromQueryMemento( selectionMemento, resultSetMapping, querySpaceConsumer, context, session );
					}
				},
				specifiedResultType,
				session
		);
	}

	@FunctionalInterface
	private interface ResultSetMappingPopulator {
		boolean resolveResultSetMapping(
				ResultSetMapping resultSetMapping,
				Consumer<String> querySpaceConsumer,
				ResultSetMappingResolutionContext context);
	}

	private NativeQueryImpl(
			NativeSelectionMementoImpl<?> selectionMemento,
			Supplier<ResultSetMapping> resultSetMappingCreator,
			ResultSetMappingPopulator resultSetMappingHandler,
			@Nullable Class<R> resultClass,
			SharedSessionContractImplementor session) {
		super( session );
		originalSqlString = selectionMemento.getOriginalSqlString();
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( originalSqlString, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		resultSetMapping = resultSetMappingCreator.get();
		resultMappingSuppliedToCtor =
				resultSetMappingHandler.resolveResultSetMapping( resultSetMapping, querySpaces::add, this );

		resultType = resultClass;
		handleExplicitResultSetMapping();

		checkResultType( resultClass, resultSetMapping );
		applyMementoOptions( selectionMemento );
	}

	private static void populateResultSetMappingFromSpecifiedName(String resultSetMappingName, ResultSetMapping resultSetMapping, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context, SharedSessionContractImplementor session) {
		final var mappingMemento = session.getFactory()
				.getQueryEngine()
				.getNamedObjectRepository()
				.getResultSetMappingMemento( resultSetMappingName );
		assert mappingMemento != null;
		mappingMemento.resolve( resultSetMapping, querySpaceConsumer, context );
	}

	private static boolean populateResultSetMappingFromQueryMemento(
			NativeSelectionMementoImpl<?> selectionMemento,
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context,
			SharedSessionContractImplementor session) {
		if ( selectionMemento.getResultMappingName() != null ) {
			final var resultSetMappingMemento = session.getFactory()
					.getQueryEngine()
					.getNamedObjectRepository()
					.getResultSetMappingMemento( selectionMemento.getResultMappingName() );
			if ( resultSetMappingMemento != null ) {
				resultSetMappingMemento.resolve( resultSetMapping, querySpaceConsumer, context );
				return true;
			}
		}

		if ( selectionMemento.getResultType() != null ) {
			resultSetMapping.addResultBuilder( resultClassBuilder( selectionMemento.getResultType(), context ) );
			return true;
		}
		else {
			return false;
		}
	}

	private static String getResultSetMappingName(NativeSelectionMementoImpl<?> memento) {
		if ( memento.getResultMappingName() != null ) {
			return memento.getResultMappingName();
		}
		else if ( memento.getResultType() != null ) {
			return memento.getResultType().getName();
		}
		else {
			return memento.getSqlString();
		}
	}

	private static ResultSetMapping buildResultSetMapping(
			String registeredName,
			boolean isDynamic,
			SharedSessionContractImplementor session) {
		return resolveResultSetMapping( registeredName, isDynamic, session.getFactory() );
	}

	/**
	 * Used to construct a native mutation query from a memento.
	 *
	 * @see org.hibernate.SharedSessionContract#createNamedStatement(String)
	 */
	public NativeQueryImpl(
			NativeMutationMementoImpl<R> mutationMemento,
			SharedSessionContractImplementor session) {
		super( session );

		originalSqlString = mutationMemento.getOriginalSqlString();
		startsWithSelect = false;
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( originalSqlString, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		resultType = null;
		resultSetMapping = MutationResultSetMapping.INSTANCE;
		resultMappingSuppliedToCtor = false;
	}


	/**
	 * @see jakarta.persistence.EntityHandler#createNativeQuery(String, jakarta.persistence.sql.ResultSetMapping)
	 */
	public NativeQueryImpl(
			String sql,
			jakarta.persistence.sql.ResultSetMapping<R> resultSetMapping,
			SharedSessionContractImplementor session) {
		super( session );
		originalSqlString = sql;
		startsWithSelect = true;
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( sql, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		this.resultSetMapping = JpaMappingHelper.toHibernateMapping( resultSetMapping, session.getSessionFactory() );
		resultMappingSuppliedToCtor = true;
		resultType = resultSetMapping.type();
	}

	public NativeQueryImpl(
			String sql,
			NamedResultSetMappingMemento resultSetMappingMemento,
			Class<R> resultClass,
			SharedSessionContractImplementor session) {
		super( session );
		originalSqlString = sql;
		startsWithSelect = true;
		querySpaces = new HashSet<>();

		final var parameterInterpretation = resolveParameterInterpretation( sql, session );
		sqlString = parameterInterpretation.getAdjustedSqlString();
		parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		parameterOccurrences = parameterInterpretation.getOrderedParameterOccurrences();
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		resultType = resultClass;
		if ( resultSetMappingMemento != null ) {
			resultSetMapping = session.getFactory().getJdbcValuesMappingProducerProvider()
					.buildResultSetMapping( null, true, session.getFactory() );
			resultSetMappingMemento.resolve( resultSetMapping, this::addSynchronizedQuerySpace, this );
			resultMappingSuppliedToCtor = true;
			handleExplicitResultSetMapping();
		}
		else {
			resultSetMapping = resolveResultSetMapping( sql, true, session.getFactory() );
			resultMappingSuppliedToCtor = false;
			handleImplicitResultSetMapping( session );
		}
	}

	@Override
	public Boolean isSelectQuery() {
		if ( resultMappingSuppliedToCtor
			|| resultSetMapping.getNumberOfResultBuilders() > 0
			|| isReadOnly()
			// as a last resort, see if the SQL starts with "select"
			|| startsWithSelect() ) {
			return true;
		}
		else {
			return null;
		}
	}

	@Override
	public NamedNativeQueryMemento<R> toMemento(String name) {
		if ( isSelectQuery() == Boolean.TRUE ) {
			return toSelectionMemento( name );
		}
		else if ( isSelectQuery() == Boolean.FALSE ) {
			return toMutationMemento( name );
		}
		else {
			throw new IllegalStateException( "Unsure how to create named query memento for native query - " + this );
		}
	}

	@Override
	public NativeMutationMementoImpl<R> toMutationMemento(String name) {
		errorIfSelectForSure();
		final QueryOptions options = getQueryOptions();
		return new NativeMutationMementoImpl<>(
				name,
				originalSqlString,
				null,
				options.getFlushMode(),
				options.getTimeout(),
				getComment(),
				getHints(),
				querySpaces
		);
	}

	@Override
	public NativeSelectionMementoImpl<R> toSelectionMemento(String name) {
		errorIfNotSelectForSure();
		final QueryOptions options = getQueryOptions();
		return new NativeSelectionMementoImpl<>(
				name,
				originalSqlString,
				resultType,
				// todo (jpa4) : we need a "resolvable reference" to result-set-mappings
				//		think about the case of an applied j.p.sql.ResultSetMapping
				null,
				querySpaces,
				options.getFlushMode(),
				options.getTimeout(),
				options.getComment(),
				options.isReadOnly(),
				options.getFetchSize(),
				options.getFirstRow(),
				options.getMaxRows(),
				options.isResultCachingEnabled(),
				options.getCacheMode(),
				options.getResultCacheRegionName(),
				options.getLockOptions().getLockMode(),
				options.getLockOptions().getLockScope(),
				options.getLockOptions().getTimeout(),
				options.getLockOptions().getFollowOnStrategy(),
				getHints()
		);
	}

	private Class<R> extractResultClass(ResultSetMapping resultSetMapping) {
		final List<ResultBuilder> resultBuilders = resultSetMapping.getResultBuilders();
		if ( resultBuilders.size() == 1 ) {
			final ResultBuilder resultBuilder = resultBuilders.get( 0 );
			if ( resultBuilder instanceof ImplicitResultClassBuilder
				|| resultBuilder instanceof ImplicitModelPartResultBuilderEntity
				|| resultBuilder instanceof DynamicResultBuilderEntityCalculated ) {
				//noinspection unchecked
				return (Class<R>) resultBuilder.getJavaType();
			}
		}
		return null;
	}

	@Override
	public NativeQueryImplementor<R> asSelectionQuery() {
		errorIfNotSelectForSure();
		return this;
	}

	@Override
	public <X> NativeQueryImplementor<X> asSelectionQuery(Class<X> type) {
		errorIfNotSelectForSure();
		//noinspection unchecked
		return (NativeQueryImplementor<X>) this;
	}

	@Override
	public <X> NativeQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}

	@Override
	public <X> NativeQueryImplementor<X> ofType(Class<X> type) {
		return asSelectionQuery( type );
	}

	@Override
	public NativeQueryImplementor<R> asMutationQuery() {
		errorIfNotSelectForSure();
		return this;
	}

	@Override
	public NativeQueryImplementor<R> asStatement() {
		return asMutationQuery();
	}

	private ParameterInterpretation resolveParameterInterpretation(
			String sqlString, SharedSessionContractImplementor session) {
		return getInterpretationCache( session )
				.resolveNativeQueryParameters( sqlString,
						s -> parameterInterpretation( sqlString ) );
	}

	private ParameterInterpretationImpl parameterInterpretation(String sqlString) {
		return ParameterInterpretationImpl.interpretParameters( sqlString, getSessionFactory() );
	}

	protected void applyMementoOptions(NamedNativeQueryMemento<?> memento) {
		super.applyMementoOptions( memento );

		if ( memento.getMaxResults() != null ) {
			queryOptions.getLimit().setMaxRows( memento.getMaxResults() );
		}
		if ( memento.getFirstResult() != null ) {
			queryOptions.getLimit().setFirstRow( memento.getFirstResult() );
		}

		final var mementoQuerySpaces = memento.getQuerySpaces();
		if ( mementoQuerySpaces != null ) {
			querySpaces = makeCopy( mementoQuerySpaces );
		}
	}

	private IllegalArgumentException buildIncompatibleException(Class<?> resultClass, Class<?> actualResultClass) {
		final String resultClassName = resultClass.getName();
		final String actualResultClassName = actualResultClass.getName();
		if ( resultClassName.equals( actualResultClassName ) ) {
			return new IllegalArgumentException(
					"Type specified for TypedQuery [" + resultClassName +
							"] is incompatible with the query return type of the same name." +
							" Both classes have the same name but are different as they have been loaded respectively by Classloaders " +
							resultClass.getClassLoader().toString() + ", " + actualResultClass.getClassLoader().toString() +
							". This suggests a classloader bug in the Runtime executing Hibernate ORM, or in the integration code."
			);
		}
		else {
			return new IllegalArgumentException(
					"Type specified for TypedQuery [" + resultClassName +
							"] is incompatible with query return type [" + actualResultClass + "]"
			);
		}
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public boolean hasCallbackActions() {
		return callback != null && callback.hasAfterLoadActions();
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected void resetCallback() {
		callback = null;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return getQueryParameterBindings();
	}

	@Override
	public Class<R> getResultType() {
		errorIfNotSelectForSure();
		return resultType;
	}

	private boolean isNotSelectForSure() {
		return isSelectQuery() == Boolean.FALSE;
	}

	private void errorIfNotSelectForSure() {
		if ( isNotSelectForSure() ) {
			// we unequivocally know it IS NOT a select query
			throw new IllegalSelectQueryException( "Not a select query", sqlString );
		}
	}

	private boolean isSelectForSure() {
		return isSelectQuery() == Boolean.FALSE;
	}

	private void errorIfSelectForSure() {
		if ( isSelectQuery() == Boolean.TRUE ) {
			// we unequivocally know it IS a select query
			throw new IllegalSelectQueryException( "Not a mutation query", sqlString );
		}
	}

	@Override
	public @Nullable Class<R> getTargetType() {
		return resultType;
	}

	@Override
	public NativeQueryImplementor<R> setTimeout(Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		// JPA says this is illegal
		throw new IllegalStateException( "LockModeType not supported for native query" );
	}

	@Override
	public NativeQueryImplementor<R> setLockMode(LockModeType lockModeType) {
		// JPA says this is illegal
		throw new IllegalStateException( "LockModeType not supported for native query" );
	}

	@Override
	public NativeQueryImplementor<R> setHibernateLockMode(LockMode lockMode) {
		queryOptions.getLockOptions().setLockMode( lockMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setLockTimeout(Timeout lockTimeout) {
		return (NativeQueryImplementor<R>) super.setLockTimeout( lockTimeout );
	}

	@Override
	public NativeQueryImplementor<R> setLockScope(PessimisticLockScope lockScope) {
		queryOptions.getLockOptions().setLockScope( lockScope );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFollowOnLockingStrategy(Locking.FollowOn strategy) {
		return (NativeQueryImplementor<R>) super.setFollowOnLockingStrategy( strategy );
	}

	@Override
	public NativeQueryImplementor<R> disableFetchProfile(String profileName) {
		queryOptions.disableFetchProfile( profileName );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> enableFetchProfile(String profileName) {
		if ( getSessionFactory().containsFetchProfileDefinition( profileName ) ) {
			getQueryOptions().enableFetchProfile( profileName );
			return this;
		}
		else {
			throw new UnknownProfileException( profileName );
		}
	}

	@Override
	public NativeQueryImplementor<R> setPage(Page page) {
		queryOptions.getLimit().setFirstRow( page.getFirstResult() );
		queryOptions.getLimit().setMaxRows( page.getMaxResults() );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFollowOnStrategy(Locking.FollowOn followOnStrategy) {
		queryOptions.getLockOptions().setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return queryOptions.getFetchSize();
	}

	@Override
	public String getCacheRegion() {
		return queryOptions.getResultCacheRegionName();
	}

	@Override
	public EntityGraph<? super R> getEntityGraph() {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}

	@Override
	public <X> NativeQueryImplementor<X> withEntityGraph(EntityGraph<X> entityGraph) {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}

	@Override
	public PessimisticLockScope getLockScope() {
		return queryOptions.getLockOptions().getLockScope();
	}

	@Override
	protected void applyReadOnlyHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Read-only is not relevant for non-select NativeQuery" );
		}
		super.applyReadOnlyHint( hintName, value );
	}

	@Override
	protected void applyFetchSizeHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Fetch-size is not relevant for non-select NativeQuery" );
		}
		super.applyFetchSizeHint( hintName, value );
	}

	@Override
	protected void applyResultCachingHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Result-caching is not relevant for non-select NativeQuery" );
		}
		super.applyResultCachingHint( hintName, value );
	}

	@Override
	protected void applyResultCacheModeHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Result-caching is not relevant for non-select NativeQuery" );
		}
		super.applyResultCacheModeHint( hintName, value );
	}

	@Override
	protected void applyResultCachingRetrieveModeHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Result-caching is not relevant for non-select NativeQuery" );
		}
		super.applyResultCachingRetrieveModeHint( hintName, value );
	}

	@Override
	protected void applyResultCachingStoreModeHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Result-caching is not relevant for non-select NativeQuery" );
		}
		super.applyResultCachingStoreModeHint( hintName, value );
	}

	@Override
	protected void applyResultCachingRegionHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Result-caching is not relevant for non-select NativeQuery" );
		}
		super.applyResultCachingRegionHint( hintName, value );
	}

	@Override
	protected void applyEntityGraphHint(GraphSemantic graphSemantic, Object value, String hintName) {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}

	@Override
	protected void applyEnabledFetchProfileHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Fetch-profile is not relevant for non-select NativeQuery" );
		}
		super.applyEnabledFetchProfileHint( hintName, value );
	}

	@Override
	protected void applyLockTimeoutHint(String hintName, Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Locking is not relevant for non-select NativeQuery" );
		}
		super.applyLockTimeoutHint( hintName, value );
		super.applyLockTimeoutHint( hintName, value );
	}

	@Override
	protected void applyFollowOnStrategyHint(Object value) {
		if ( isNotSelectForSure() ) {
			throw new IllegalArgumentException( "Locking is not relevant for non-select NativeQuery" );
		}
		super.applyFollowOnStrategyHint( value );
	}

	@Override
	public <T> NativeQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		errorIfNotSelectForSure();
		queryOptions.setTupleTransformer( transformer );
		//noinspection unchecked
		return (NativeQueryImplementor<T>) this;
	}

	@Override
	public NativeQueryImplementor<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		errorIfNotSelectForSure();
		queryOptions.setResultListTransformer( transformer );
		return this;
	}

	private boolean startsWithSelect() {
		if ( startsWithSelect == null ) {
			startsWithSelect = sqlString.toLowerCase( Locale.ROOT ).startsWith( "select " );
		}
		return startsWithSelect;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	public boolean isQueryPlanCacheable() {
		return true;
	}

	@Override
	protected boolean requiresTxn(LockMode lockMode) {
		return isNotSelectForSure()
				|| lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		return true;
	}

	@Override
	protected void prepareForExecution() {
		final var spaces = getSynchronizedQuerySpaces();
		if ( spaces == null || spaces.isEmpty() ) {
			// We need to flush. The query itself is not required to execute in a
			// transaction; if there is no transaction, the flush would throw a
			// TransactionRequiredException which would potentially break existing
			// apps, so we only do the flush if a transaction is in progress.
			if ( shouldFlush() ) {
				getSession().flush();
			}
			// Reset the callback before every execution
			resetCallback();
		}
		// Otherwise, the application specified query spaces via the Hibernate
		// SynchronizeableQuery and so the query will already perform a partial
		// flush according to the defined query spaces - no need for a full flush.
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected boolean shouldFlush() {
		if ( getSession().isTransactionInProgress() ) {
			var queryFlushMode = getQueryOptions().getFlushMode();
			return switch ( queryFlushMode == null ? session.getHibernateFlushMode() : queryFlushMode ) {
				// The JPA spec requires that we auto-flush before native queries
				case AUTO -> getSessionFactory().getSessionFactoryOptions().isJpaBootstrap();
				case ALWAYS -> true;
				default -> false;
			};
		}
		else {
			return false;
		}
	}

	@Override
	protected List<R> doList() {
		return resolveSelectQueryPlan().performList( this );
	}

	@Override
	public long getResultCount() {
		final var context = new DelegatingDomainQueryExecutionContext(this) {
			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
			}
		};
		return createCountQueryPlan().executeQuery( context, SingleResultConsumer.instance() );
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> page) {
		throw new UnsupportedOperationException("native queries do not support key-based pagination");
	}

	protected SelectQueryPlan<R> resolveSelectQueryPlan() {
		final var mapping = resultSetMapping();
		checkResultType( resultType, mapping );
		final int parameterStartPosition = parameterStartPosition();
		if ( isCacheableQuery() ) {
			return getInterpretationCache().resolveSelectQueryPlan(
					selectInterpretationsKey( mapping, parameterStartPosition ),
					key -> createQueryPlan( key.getResultSetMapping(), key.getStartPosition() )
			);
		}
		else {
			return createQueryPlan( mapping, parameterStartPosition );
		}
	}

	private int parameterStartPosition() {
		final var jdbcServices = getSessionFactory().getJdbcServices();
		if ( !isStandardRenderer( jdbcServices.getParameterMarkerStrategy() )
				&& hasLimit( getQueryOptions().getLimit() ) ) {
			final var limitHandler = jdbcServices.getDialect().getLimitHandler();
			if ( limitHandler.processSqlMutatesState() ) {
				limitHandler.processSql( sqlString, -1, null, getQueryOptions() );
			}
			// A non-standard parameter marker strategy is in use, and the limit handler wants to bind parameters
			// before the main parameters. This requires recording the start position in the cache key
			// because the generated SQL depends on this information
			return limitHandler.getParameterPositionStart( getQueryOptions().getLimit() );
		}
		else {
			return 1;
		}
	}

	private ResultSetMapping resultSetMapping() {
		if ( resultType != null
				&& resultSetMapping.isDynamic()
				&& resultSetMapping.getNumberOfResultBuilders() == 0 ) {
			final var sessionFactory = getSessionFactory();
			final var mapping = resolveResultSetMapping( originalSqlString, true, sessionFactory );
			final var metamodel = getMappingMetamodel();
			if ( metamodel.isEntityClass( resultType ) ) {
				mapping.addResultBuilder(
						Builders.entityCalculated( unqualify( resultType.getName() ), resultType.getName(),
								LockMode.READ, sessionFactory ) );
			}
			else if ( !isResultTypeAlwaysAllowed( resultType )
					&& (!isClass( resultType ) || hasJavaTypeDescriptor( resultType )) ) {
				mapping.addResultBuilder( Builders.resultClassBuilder( resultType, metamodel ) );
			}
			return mapping;
		}
		else {
			return resultSetMapping;
		}
	}

	private NativeSelectQueryPlan<R> createQueryPlan(ResultSetMapping resultSetMapping, int parameterStartPosition) {
		final NativeSelectQueryDefinition<R> queryDefinition = new NativeSelectQueryDefinition<>() {
			final String sqlString = expandParameterLists( parameterStartPosition );

			@Override
			public String getSqlString() {
				return sqlString;
			}

			@Override
			public boolean isCallable() {
				return false;
			}

			@Override
			public List<ParameterOccurrence> getQueryParameterOccurrences() {
				return parameterOccurrences;
			}

			@Override
			public ResultSetMapping getResultSetMapping() {
				return resultSetMapping;
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return querySpaces;
			}
		};
		return getNativeQueryInterpreter().createQueryPlan( queryDefinition, getSessionFactory() );
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected NativeSelectQueryPlan<Long> createCountQueryPlan() {
		final NativeSelectQueryDefinition<Long> queryDefinition = new NativeSelectQueryDefinition<>() {
			final BasicType<Long> longType = getTypeConfiguration().getBasicTypeForJavaType(Long.class);
			final String sqlString = expandParameterLists( 1 );

			@Override
			public String getSqlString() {
				return "select count(*) from (" + sqlString + ") a_";
			}

			@Override
			public boolean isCallable() {
				return false;
			}

			@Override
			public List<ParameterOccurrence> getQueryParameterOccurrences() {
				return parameterOccurrences;
			}

			@Override
			public ResultSetMapping getResultSetMapping() {
				final ResultSetMappingImpl mapping = new ResultSetMappingImpl( "", true );
				mapping.addResultBuilder( new DynamicResultBuilderBasicStandard( 1, longType ) );
				return mapping;
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return querySpaces;
			}
		};
		return getNativeQueryInterpreter().createQueryPlan( queryDefinition, getSessionFactory() );
	}

	private NativeQueryInterpreter getNativeQueryInterpreter() {
		return getSessionFactory().getQueryEngine().getNativeQueryInterpreter();
	}

	protected String expandParameterLists(int parameterStartPosition) {
		if ( parameterOccurrences == null || parameterOccurrences.isEmpty() ) {
			return sqlString;
		}
		// HHH-1123
		// Some DBs limit number of IN expressions.  For now, warn...
		final var factory = getSessionFactory();
		final var dialect = factory.getJdbcServices().getDialect();
		final boolean paddingEnabled = factory.getSessionFactoryOptions().inClauseParameterPaddingEnabled();
		final int inExprLimit = dialect.getInExpressionCountLimit();
		final var parameterMarkerStrategy = factory.getJdbcServices().getParameterMarkerStrategy();
		final boolean needsMarker = !isStandardRenderer( parameterMarkerStrategy );

		var sql =
				needsMarker
						? new StringBuilder( sqlString.length() + parameterOccurrences.size() * 10 )
								.append( sqlString )
						: null;

		// Handle parameter lists
		int offset = 0;
		int parameterPosition = parameterStartPosition;
		for ( var occurrence : parameterOccurrences ) {
			final var queryParameter = occurrence.parameter();
			final var binding = parameterBindings.getBinding( queryParameter );
			if ( binding.isMultiValued() ) {
				final int bindValueCount = binding.getBindValues().size();
				logTooManyExpressions( inExprLimit, bindValueCount, dialect, queryParameter );
				final int sourcePosition = occurrence.sourcePosition();
				if ( sourcePosition >= 0 ) {
					// check if placeholder is already immediately enclosed in parentheses
					// (ignoring whitespace)
					final boolean isEnclosedInParens = isEnclosedInParens( sourcePosition );
					// short-circuit for performance when only 1 value and the
					// placeholder is already enclosed in parentheses...
					if ( bindValueCount != 1 || !isEnclosedInParens ) {
						if ( sql == null ) {
							sql = new StringBuilder( sqlString.length() + 20 )
									.append( sqlString );
						}
						final int bindValueMaxCount =
								determineBindValueMaxCount( paddingEnabled, inExprLimit, bindValueCount );
						final String expansionListAsString = expandList(
								bindValueMaxCount,
								isEnclosedInParens,
								parameterPosition,
								parameterMarkerStrategy,
								needsMarker
						);
						final int start = sourcePosition + offset;
						final int end = start + 1;
						sql.replace( start, end, expansionListAsString );
						offset += expansionListAsString.length() - 1;
						parameterPosition += bindValueMaxCount;
					}
					else if ( needsMarker ) {
						final int start = sourcePosition + offset;
						final int end = start + 1;
						final String parameterMarker = parameterMarkerStrategy.createMarker( parameterPosition, null );
						sql.replace( start, end, parameterMarker );
						offset += parameterMarker.length() - 1;
						parameterPosition++;
					}
				}
			}
			else if ( needsMarker ) {
				final int sourcePosition = occurrence.sourcePosition();
				final int start = sourcePosition + offset;
				final int end = start + 1;
				final String parameterMarker = parameterMarkerStrategy.createMarker( parameterPosition, null );
				sql.replace( start, end, parameterMarker );
				offset += parameterMarker.length() - 1;
				parameterPosition++;
			}
		}
		return sql == null ? sqlString : sql.toString();
	}

	private static void logTooManyExpressions(
			int inExprLimit, int bindValueCount,
			Dialect dialect, QueryParameterImplementor<?> queryParameter) {
		if ( inExprLimit > 0 && bindValueCount > inExprLimit ) {
			CORE_LOGGER.tooManyInExpressions(
					dialect.getClass().getName(),
					inExprLimit,
					queryParameter.getName() == null
							? queryParameter.getPosition().toString()
							: queryParameter.getName(),
					bindValueCount
			);
		}
	}

	private static String expandList(int bindValueMaxCount, boolean isEnclosedInParens, int parameterPosition, ParameterMarkerStrategy parameterMarkerStrategy, boolean needsMarker) {
		// HHH-8901
		if ( bindValueMaxCount == 0 ) {
			return isEnclosedInParens ? "null" : "(null)";
		}
		else if ( needsMarker ) {
			final StringBuilder sb = new StringBuilder( bindValueMaxCount * 4 );
			if ( !isEnclosedInParens ) {
				sb.append( '(' );
			}
			for ( int i = 0; i < bindValueMaxCount; i++ ) {
				sb.append( parameterMarkerStrategy.createMarker( parameterPosition + i, null ) );
				sb.append( ',' );
			}
			sb.setLength( sb.length() - 1 );
			if ( !isEnclosedInParens ) {
				sb.append( ')' );
			}
			return sb.toString();
		}
		else {
			// Shift 1 bit instead of multiplication by 2
			final char[] chars;
			if ( isEnclosedInParens ) {
				chars = new char[(bindValueMaxCount << 1) - 1];
				chars[0] = '?';
				for ( int i = 1; i < bindValueMaxCount; i++ ) {
					final int index = i << 1;
					chars[index - 1] = ',';
					chars[index] = '?';
				}
			}
			else {
				chars = new char[(bindValueMaxCount << 1) + 1];
				chars[0] = '(';
				chars[1] = '?';
				for ( int i = 1; i < bindValueMaxCount; i++ ) {
					final int index = i << 1;
					chars[index] = ',';
					chars[index + 1] = '?';
				}
				chars[chars.length - 1] = ')';
			}
			return new String( chars );
		}
	}

	private boolean isEnclosedInParens(int sourcePosition) {
		boolean isEnclosedInParens = true;
		for ( int i = sourcePosition - 1; i >= 0; i-- ) {
			final char ch = sqlString.charAt( i );
			if ( !isWhitespace( ch ) ) {
				isEnclosedInParens = ch == '(';
				break;
			}
		}
		if ( isEnclosedInParens ) {
			for ( int i = sourcePosition + 1; i < sqlString.length(); i++ ) {
				final char ch = sqlString.charAt( i );
				if ( !isWhitespace( ch ) ) {
					isEnclosedInParens = ch == ')';
					break;
				}
			}
		}
		return isEnclosedInParens;
	}

	public static int determineBindValueMaxCount(boolean paddingEnabled, int inExprLimit, int bindValueCount) {
		int bindValueMaxCount = bindValueCount;

		final boolean inClauseParameterPaddingEnabled = paddingEnabled && bindValueCount > 2;

		if ( inClauseParameterPaddingEnabled ) {
			int bindValuePaddingCount = MathHelper.ceilingPowerOfTwo( bindValueCount );

			if ( inExprLimit > 0 && bindValuePaddingCount > inExprLimit ) {
				bindValuePaddingCount = inExprLimit;
			}

			if ( bindValueCount < bindValuePaddingCount ) {
				bindValueMaxCount = bindValuePaddingCount;
			}
		}
		return bindValueMaxCount;
	}

	private SelectInterpretationsKey selectInterpretationsKey(ResultSetMapping resultSetMapping, int parameterStartPosition) {
		return new SelectInterpretationsKey(
				getQueryString(),
				resultSetMapping,
				getSynchronizedQuerySpaces(),
				parameterStartPosition
		);
	}

	private boolean isCacheableQuery() {
		// todo (6.0): unless we move the limit rendering from DeferredResultSetAccess to NativeSelectQueryPlanImpl
		//  we don't need to consider the limit here at all because that is applied on demand.
		//  It certainly is better for performance to include the limit early, but then we might trash the cache
//		if ( hasLimit( query.getQueryOptions().getLimit() ) ) {
//			return false;
//		}

		// For now, don't cache plans that have parameter lists
		return !parameterBindings.hasAnyMultiValuedBindings();
	}

	private boolean hasLimit(Limit limit) {
		return limit != null && !limit.isEmpty();
	}

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	public int execute() {
		return executeUpdate();
	}

	protected int doExecuteUpdate() {
		return resolveNonSelectQueryPlan().executeUpdate( this );
	}

	private BasicTypeRegistry getBasicTypeRegistry() {
		return getTypeConfiguration().getBasicTypeRegistry();
	}

	protected QueryInterpretationCache getInterpretationCache() {
		return getInterpretationCache( getSession() );
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		NonSelectQueryPlan queryPlan = null;

		final var cacheKey = generateNonSelectInterpretationsKey();
		if ( cacheKey != null ) {
			queryPlan = getInterpretationCache().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			final String sqlString = expandParameterLists( 1 );
			queryPlan = new NativeNonSelectQueryPlanImpl( sqlString, querySpaces, parameterOccurrences );
			if ( cacheKey != null ) {
				getInterpretationCache().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	protected NonSelectInterpretationsKey generateNonSelectInterpretationsKey() {
		// todo (6.0) - should this account for query spaces in determining "cacheable"?
		return isCacheableQuery()
				? new NonSelectInterpretationsKey( getQueryString(), getSynchronizedQuerySpaces() )
				: null;
	}

	@Override
	public void addResultTypeClass(Class<?> resultClass) {
		assert resultSetMapping.getNumberOfResultBuilders() == 0;
		registerBuilder( Builders.resultClassBuilder( resultClass, getSessionFactory().getMappingMetamodel() ) );
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias) {
		return registerBuilder( Builders.scalar( columnAlias ) );
	}

	public NativeQueryImplementor<R> addScalar(int position, Class<?> type) {
		return registerBuilder( Builders.scalar( position, getBasicTypeRegistry().getRegisteredType( type ) ) );
	}

	protected NativeQueryImplementor<R> registerBuilder(ResultBuilder builder) {
		resultSetMapping.addResultBuilder( builder );
		return this;
	}

	@Override
	public NativeQuery<R> addScalar(String columnAlias, @SuppressWarnings("rawtypes") BasicTypeReference type) {
		return registerBuilder( Builders.scalar( columnAlias,
				getBasicTypeRegistry().resolve( (BasicTypeReference<?>) type ) ) );
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias, @SuppressWarnings("rawtypes") BasicDomainType type) {
		return registerBuilder( Builders.scalar( columnAlias, (BasicType<?>) type ) );
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias, @SuppressWarnings("rawtypes") Class javaType) {
		@SuppressWarnings("unchecked")
		final BasicType<?> basicType = getBasicTypeRegistry().getRegisteredType( javaType );
		return basicType != null
				? registerBuilder( Builders.scalar( columnAlias, basicType ) )
				: registerBuilder( Builders.scalar( columnAlias, javaType, getSessionFactory() ) );
	}

	@Override
	public <C> NativeQueryImplementor<R> addScalar(
			String columnAlias,
			Class<C> jdbcJavaType,
			AttributeConverter<?, C> converter) {
		return registerBuilder( Builders.converted( columnAlias, jdbcJavaType, converter, getSessionFactory() ) );
	}

	@Override
	public <O, J> NativeQueryImplementor<R> addScalar(
			String columnAlias,
			Class<O> domainJavaType,
			Class<J> jdbcJavaType,
			AttributeConverter<O, J> converter) {
		return registerBuilder( Builders.converted( columnAlias, domainJavaType, jdbcJavaType, converter, getSessionFactory() ) );
	}

	@Override
	public <C> NativeQueryImplementor<R> addScalar(
			String columnAlias,
			Class<C> relationalJavaType,
			Class<? extends AttributeConverter<?, C>> converter) {
		return registerBuilder( Builders.converted( columnAlias, relationalJavaType, converter, getSessionFactory() ) );
	}

	@Override
	public <O, J> NativeQueryImplementor<R> addScalar(
			String columnAlias,
			Class<O> domainJavaType,
			Class<J> jdbcJavaType,
			Class<? extends AttributeConverter<O, J>> converterJavaType) {
		return registerBuilder( Builders.converted( columnAlias, domainJavaType, jdbcJavaType, converterJavaType, getSessionFactory() ) );
	}

	@Override
	public <J> InstantiationResultNode<J> addInstantiation(Class<J> targetJavaType) {
		final DynamicResultBuilderInstantiation<J> builder =
				Builders.instantiation( targetJavaType, getSessionFactory() );
		registerBuilder( builder );
		return builder;
	}

	@Override
	public NativeQueryImplementor<R> addAttributeResult(
			String columnAlias,
			@SuppressWarnings("rawtypes") Class entityJavaType,
			String attributePath) {
		return addAttributeResult( columnAlias, entityJavaType.getName(), attributePath );
	}

	@Override
	public NativeQueryImplementor<R> addAttributeResult(
			String columnAlias,
			String entityName,
			String attributePath) {
		registerBuilder( Builders.attributeResult( columnAlias, entityName, attributePath, getSessionFactory() ) );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addAttributeResult(
			String columnAlias,
			@SuppressWarnings("rawtypes") SingularAttribute attribute) {
		registerBuilder( Builders.attributeResult( columnAlias, attribute, getSessionFactory() ) );
		return this;
	}

	@Override
	public DynamicResultBuilderEntityStandard addRoot(String tableAlias, String entityName) {
		final var resultBuilder = Builders.entity( tableAlias, entityName, getSessionFactory() );
		resultSetMapping.addResultBuilder( resultBuilder );
		entityMappingTypeByTableAlias.put( tableAlias, resultBuilder.getEntityMapping() );
		return resultBuilder;
	}

	@Override
	public DynamicResultBuilderEntityStandard addRoot(String tableAlias, @SuppressWarnings("rawtypes") Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String entityName) {
		return addEntity( unqualify( entityName ), entityName );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, String entityName) {
		final var builder = Builders.entityCalculated( tableAlias, entityName, getSessionFactory() );
		entityMappingTypeByTableAlias.put( tableAlias, builder.getEntityMapping() );
		registerBuilder( builder );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, String entityName, LockMode lockMode) {
		final var builder = Builders.entityCalculated( tableAlias, entityName, lockMode, getSessionFactory() );
		entityMappingTypeByTableAlias.put( tableAlias, builder.getEntityMapping() );
		registerBuilder( builder );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addEntity(@SuppressWarnings("rawtypes") Class entityType) {
		return addEntity( entityType.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(Class<R> entityType, LockMode lockMode) {
		return addEntity( unqualify( entityType.getName() ), entityType.getName(), lockMode);
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityClass) {
		return addEntity( tableAlias, entityClass.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, @SuppressWarnings("rawtypes") Class entityClass, LockMode lockMode) {
		return addEntity( tableAlias, entityClass.getName(), lockMode );
	}

	@Override
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		final var subPart = entityMappingTypeByTableAlias.get( ownerTableAlias ).findSubPart( joinPropertyName );
		addEntityMappingType( tableAlias, subPart );
		final var fetchBuilder = Builders.fetch( tableAlias, ownerTableAlias, (Fetchable) subPart );
		resultSetMapping.addLegacyFetchBuilder( fetchBuilder );
		return fetchBuilder;
	}

	private void addEntityMappingType(String tableAlias, ModelPart part) {
		if ( part instanceof PluralAttributeMapping pluralAttributeMapping ) {
			final var partMappingType = pluralAttributeMapping.getElementDescriptor().getPartMappingType();
			if ( partMappingType instanceof EntityMappingType entityMappingType ) {
				entityMappingTypeByTableAlias.put( tableAlias, entityMappingType );
			}
		}
		else if ( part instanceof EntityAssociationMapping entityAssociationMapping ) {
			entityMappingTypeByTableAlias.put( tableAlias, entityAssociationMapping.asEntityMappingType() );
		}
		else if ( part instanceof EmbeddedAttributeMapping ) {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String path) {
		createFetchJoin( tableAlias, path );
		return this;
	}

	private FetchReturn createFetchJoin(String tableAlias, String path) {
		final int loc = path.indexOf( '.' );
		if ( loc < 0 ) {
			throw new PathException( "Not a property path '" + path + "'" );
		}
		final String ownerTableAlias = path.substring( 0, loc );
		final String joinedPropertyName = path.substring( loc + 1 );
		return addFetch( tableAlias, ownerTableAlias, joinedPropertyName );
	}

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String path, LockMode lockMode) {
		createFetchJoin( tableAlias, path ).setLockMode( lockMode );
		return this;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return querySpaces;
	}


	@Override
	public NativeQueryImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		addQuerySpaces( querySpace );
		return this;
	}

	protected void addQuerySpaces(String... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new HashSet<>();
			}
			addAll( querySpaces, spaces );
		}
	}

	protected void addQuerySpaces(Serializable... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new HashSet<>();
			}
			addAll( querySpaces, (String[]) spaces );
		}
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) {
		addQuerySpaces( getMappingMetamodel().getEntityDescriptor( entityName ).getQuerySpaces() );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) {
		addQuerySpaces( getMappingMetamodel().getEntityDescriptor( entityClass ).getQuerySpaces() );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		getQueryOptions().setFlushMode( FlushModeTypeHelper.getFlushMode(queryFlushMode) );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		errorIfNotSelectForSure();
		queryOptions.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		errorIfNotSelectForSure();
		queryOptions.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		errorIfNotSelectForSure();
		queryOptions.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setTimeout(Integer timeout) {
		if ( timeout == null ) {
			timeout = -1;
		}
		super.setTimeout( (int) timeout );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheable(boolean cacheable) {
		errorIfNotSelectForSure();
		queryOptions.setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheRegion(String cacheRegion) {
		errorIfNotSelectForSure();
		queryOptions.setResultCacheRegionName( cacheRegion );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		queryOptions.setQueryPlanCachingEnabled( queryPlanCacheable );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFetchSize(int fetchSize) {
		errorIfNotSelectForSure();
		queryOptions.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setReadOnly(boolean readOnly) {
		errorIfNotSelectForSure();
		queryOptions.setReadOnly( readOnly );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addQueryHint(String hint) {
		super.addQueryHint( hint );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setEntityGraph(EntityGraph<? super R> entityGraph) {
		throw new UnsupportedOperationException( "EntityGraphs are not supported for native queries" );
	}

	@Override
	public NativeQueryImplementor<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic) {
		throw new UnsupportedOperationException( "EntityGraphs are not supported for native queries" );
	}

	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		putIfNotNull( hints, HINT_NATIVE_LOCK_MODE, queryOptions.getLockOptions().getLockMode() );
	}

	protected void applySynchronizeSpacesHint(Object value) {
		applySynchronizeSpace( value );
	}

	protected void applySynchronizeSpace(Object value) {
		if ( value instanceof String string ) {
			addSynchronizedQuerySpace( string );
		}
		else if ( value instanceof Class<?> clazz ) {
			addSynchronizedEntityClass( clazz );
		}
		else if ( value instanceof Object[] array ) {
			for ( Object element : array ) {
				applySynchronizeSpace( element );
			}
		}
		else if ( value instanceof Iterable<?> iterable ) {
			for ( Object element : iterable ) {
				applySynchronizeSpace( element );
			}
		}
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(String name, P value, Class<P> javaTypeClass) {
		super.setParameter( name, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(String name, P value, Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(int position, P value, Class<P> javaTypeClass) {
		super.setParameter( position, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(int position, P value, Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaTypeClass) {
		super.setParameter( parameter, value, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated
	public NativeQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		return (NativeQueryImplementor<R>) super.setConvertedParameter( name, value, converterClass );
	}

	@Override
	public <P> NativeQueryImplementor<R> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converterClass) {
		return (NativeQueryImplementor<R>) super.setConvertedParameter( position, value, converterClass );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( name, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( name, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(String name, P[] values, Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( position, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( position, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(int position, P[] values, Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}



	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaTypeClass) {
		super.setParameterList( parameter, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaTypeClass) {
		super.setParameterList( parameter, values, javaTypeClass );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setMaxResults(int maxResults) {
		queryOptions.getLimit().setMaxRows( maxResults );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFirstResult(int startPosition) {
		queryOptions.getLimit().setFirstRow( startPosition );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	@Override
	public NativeQueryImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	/// Called for cases where a ResultSetMapping was not explicitly provided by the application.
	/// If there was an explicit resultType (which is not an entity class), apply a "tuple transformation"
	/// to handle construction of the specified resultType.
	private void handleImplicitResultSetMapping(SharedSessionContractImplementor session) {
		if ( resultType != null && !session.getFactory().getMappingMetamodel().isEntityClass( resultType )  ) {
			setTupleTransformerForResultType( resultType );
		}
	}

	/// Called for cases where a ResultSetMapping (in some form) was explicitly provided by the application.
	/// If there was also an explicit resultType (which is not an entity class), we apply
	/// a "tuple transformation" for the resultType.
	private void handleExplicitResultSetMapping() {
		if ( resultType != null ) {
			if ( isResultTypeAlwaysAllowed( resultType ) ) {
				setTupleTransformerForResultType( resultType );
			}
			else {
				checkResultType( resultType, resultSetMapping );
			}
		}
	}

	private void checkResultType(Class<R> resultType, ResultSetMapping resultSetMapping) {
		// resultType can be null if any of the deprecated methods were used to create the query
		if ( resultType != null && !isResultTypeAlwaysAllowed( resultType )) {
			switch ( resultSetMapping.getNumberOfResultBuilders() ) {
				case 0:
					if ( !resultSetMapping.isDynamic() ) {
						throw new IllegalArgumentException( "Named query exists, but did not specify a resultClass" );
					}
					break;
				case 1:
					final var actualResultJavaType = resultSetMapping.getResultBuilders().get( 0 ).getJavaType();
					if ( actualResultJavaType != null && !resultType.isAssignableFrom( actualResultJavaType ) ) {
						throw buildIncompatibleException( resultType, actualResultJavaType );
					}
					break;
				default:
					// The return type has to be a class with an appropriate constructor,
					// i.e. one whose parameter types match the types of the result builders.
					// If no such constructor is found, throw an IAE
					if ( !validConstructorFoundForResultType( resultType, resultSetMapping ) ) {
						throw new IllegalArgumentException(
								"The return type for a multivalued result set mapping should be Object[], Map, List, or Tuple"
								+ " or it must have an appropriate constructor"
						);
					}
			}
		}
	}

	private boolean validConstructorFoundForResultType(Class<R> resultType, ResultSetMapping resultSetMapping) {
		// TODO: Only one constructor with the right number of parameters is allowed
		//       (see NativeQueryConstructorTransformer) so we should validate that
		outer: for ( var constructor : resultType.getConstructors() ) {
			if ( constructor.getParameterCount() == resultSetMapping.getNumberOfResultBuilders() ) {
				final var resultBuilders = resultSetMapping.getResultBuilders();
				final var paramTypes = constructor.getParameterTypes();
				for ( int i = 0; i < resultBuilders.size(); i++ ) {
					if ( !constructorParameterMatches( resultBuilders.get( i ), paramTypes[i] ) ) {
						continue outer;
					}
				}
				return true;
			}
		}
		return false;
	}

	private static boolean constructorParameterMatches(ResultBuilder resultBuilder, Class<?> paramType) {
		return resultBuilder.getJavaType() == canonicalize( paramType );
	}

	protected <T> void setTupleTransformerForResultType(Class<T> resultClass) {
		final var tupleTransformer = determineTupleTransformerForResultType( resultClass );
		if ( tupleTransformer != null ) {
			setTupleTransformer( tupleTransformer );
		}
	}

	/**
	 * If the result type of the query is {@link Tuple}, {@link Map}, {@link List},
	 * or any record or class type with an appropriate constructor which is NOT a
	 * registered basic type, then we attempt to repackage the result tuple as an
	 * instance of the result type using an appropriate {@link TupleTransformer}.
	 *
	 * @param resultClass The requested result type of the query
	 * @return A {@link TupleTransformer} responsible for repackaging the result type
	 */
	protected @Nullable TupleTransformer<?> determineTupleTransformerForResultType(Class<?> resultClass) {
		if ( Tuple.class.equals( resultClass ) ) {
			return NativeQueryTupleTransformer.INSTANCE;
		}
		else if ( Map.class.equals( resultClass ) ) {
			return NativeQueryMapTransformer.INSTANCE;
		}
		else if ( List.class.equals( resultClass ) ) {
			return NativeQueryListTransformer.INSTANCE;
		}
		else if ( Object[].class.equals( resultClass ) ) {
			return NativeQueryArrayTransformer.INSTANCE;
		}
		else if ( resultClass != Object.class ) {
			// TODO: this is extremely fragile and probably a bug
			if ( isClass( resultClass ) && !hasJavaTypeDescriptor( resultClass ) ) {
				// not a basic type, so something we can attempt
				// to instantiate to repackage the results
				return new NativeQueryConstructorTransformer<>( resultClass );
			}
		}
		return null;
	}

	private <T> boolean hasJavaTypeDescriptor(Class<T> resultClass) {
		final JavaType<?> descriptor = getTypeConfiguration().getJavaTypeRegistry().findDescriptor( resultClass );
		return descriptor != null && descriptor.getClass() != UnknownBasicJavaType.class;
	}

	private static QueryInterpretationCache getInterpretationCache(SharedSessionContractImplementor session) {
		return session.getFactory().getQueryEngine().getInterpretationCache();
	}
}
