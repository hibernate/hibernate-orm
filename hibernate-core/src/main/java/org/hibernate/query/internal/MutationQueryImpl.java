/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.Statement;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.util.OptionsHelper;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.IllegalSelectQueryException;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.internal.CriteriaMutationMementoImpl;
import org.hibernate.query.named.internal.HqlMutationMementoImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.internal.AggregatedNonSelectQueryPlanImpl;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.MultiTableDeleteQueryPlan;
import org.hibernate.query.sqm.internal.MultiTableInsertQueryPlan;
import org.hibernate.query.sqm.internal.MultiTableUpdateQueryPlan;
import org.hibernate.query.sqm.internal.SimpleDeleteQueryPlan;
import org.hibernate.query.sqm.internal.SimpleNonSelectQueryPlan;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.InterpretationsKeySource;
import org.hibernate.query.sqm.spi.SqmStatementAccess;
import org.hibernate.query.sqm.tree.AbstractSqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.query.sqm.internal.SqmInterpretationsKey.generateNonSelectKey;
import static org.hibernate.query.sqm.tree.SqmCopyContext.simpleContext;

///Implementation of `MutationQuery` based on a [SqmDmlStatement] SQM AST.
///
/// @author Steve Ebersole
public class MutationQueryImpl<T>
		extends AbstractSqmQuery<T>
		implements SqmStatementAccess<T>, MutationQueryImplementor<T>, DomainQueryExecutionContext, InterpretationsKeySource {
	private final String hql;
	private final Object queryStringCacheKey;
	private final SqmDmlStatement<T> sqm;
	private final Class<T> targetType;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings parameterBindings;

	public MutationQueryImpl(
			String hql,
			HqlInterpretation<T> hqlInterpretation,
			Class<T> targetType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;
		this.queryStringCacheKey = hql;

		this.sqm = SqmUtil.asDmlStatement( hqlInterpretation.getSqmStatement(), hql );
		this.targetType = determineTargetType( targetType, sqm.getTarget().getNodeJavaType().getJavaTypeClass() );

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		validateQuery( sqm, hql );
	}

	@Override
	@Nonnull
	public <R> SelectionQueryImplementor<R> withResultSetMapping(@Nonnull ResultSetMapping<R> mapping) {
		throw new IllegalSelectQueryException( "MutationQuery cannot be treated as SelectionQuery", hql );
	}

	private static <R> void validateQuery(SqmDmlStatement<R> sqm, String hql) {
		if ( sqm instanceof AbstractSqmDmlStatement<R> abstractSqm ) {
			abstractSqm.validate( hql );
		}
	}

	@SuppressWarnings("unchecked")
	private static <X> Class<X> determineTargetType(Class<?> providedType, Class<?> sqmTargetType) {
		if ( providedType != null && !Object.class.equals( providedType ) ) {
			return (Class<X>) providedType;
		}
		else {
			return (Class<X>) sqmTargetType;
		}
	}

	/// Creates a [org.hibernate.query.MutationQuery] from a named HQL memento.
	/// Form used from [HqlMutationMementoImpl#toMutationQuery].
	public MutationQueryImpl(
			HqlMutationMementoImpl<?> memento,
			HqlInterpretation<T> interpretation,
			Class<T> targetType,
			SharedSessionContractImplementor session) {
		this( memento.getHqlString(),
				interpretation,
				targetType,
				session );
		applyMementoOptions( memento );
	}

	/// Creates a [org.hibernate.query.MutationQuery] from a named HQL memento.
	/// Form used from [CriteriaMutationMementoImpl#toMutationQuery].
	public MutationQueryImpl(
			CriteriaMutationMementoImpl<?> memento,
			SqmDmlStatement<T> criteria,
			SharedSessionContractImplementor session) {
		this( criteria, session );
		applyMementoOptions( memento );
	}

	public MutationQueryImpl(
			SqmDmlStatement<T> criteria,
			SharedSessionContractImplementor session) {
		this( criteria, session.isCriteriaCopyTreeEnabled(), session );
	}

	public MutationQueryImpl(
			SqmDmlStatement<T> criteria,
			boolean copyAst,
			SharedSessionContractImplementor session) {
		super( session );
		hql = CRITERIA_HQL_STRING;
		sqm = SqmUtil.asDmlStatement( copyAst ? criteria.copy( simpleContext() ) : criteria, hql );
		queryStringCacheKey = sqm;
		targetType = criteria.getTarget().getNodeJavaType().getJavaTypeClass();

		domainParameterXref = DomainParameterXref.from( sqm );
		parameterMetadata = domainParameterXref.hasParameters()
				? new ParameterMetadataImpl( domainParameterXref.getQueryParameters() )
				: ParameterMetadataImpl.EMPTY;
		parameterBindings = parameterMetadata.createBindings( session.getFactory() );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		bindValueBindCriteriaParameters( domainParameterXref, parameterBindings );

		validateQuery( sqm, hql );
	}

	public MutationQueryImpl(
			NamedQueryMemento<?> memento,
			SqmDmlStatement<T> criteria,
			boolean copyAst,
			SharedSessionContractImplementor session) {
		this( criteria, copyAst, session );
		applyMementoOptions( memento );
	}

	@Override
	@Nullable
	public String getQueryString() {
		return hql;
	}

	@Override
	public Object getQueryStringCacheKey() {
		return queryStringCacheKey;
	}

	@Override
	public SqmDmlStatement<T> getSqmStatement() {
		return sqm;
	}

	@Nullable
	public Class<T> getTargetType() {
		return targetType;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> asMutationQuery() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	public int execute() {
		return executeMutation( this::doExecute );
	}

	private int doExecute() {
		try {
			return resolveNonSelectQueryPlan().executeUpdate( this );
		}
		finally {
			domainParameterXref.clearExpansions();
		}
	}

	@Override @SuppressWarnings("removal")
	public int executeUpdate() {
		return execute();
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		// resolve (or make) the QueryPlan.

		final var cacheKey = generateNonSelectKey( this );
		final var interpretationCache = getInterpretationCache();
		if ( cacheKey != null ) {
			final var queryPlan = interpretationCache.getNonSelectQueryPlan( cacheKey );
			if ( queryPlan != null ) {
				return queryPlan;
			}
		}


		final var queryPlan = buildNonSelectQueryPlan();
		if ( cacheKey != null ) {
			interpretationCache.cacheNonSelectQueryPlan( cacheKey, queryPlan );
		}
		return queryPlan;
	}

	private NonSelectQueryPlan buildNonSelectQueryPlan() {
		// To get here, the SQM statement has already been
		// validated to be a non-select variety
		final var sqmStatement = getSqmStatement();
		if ( sqmStatement instanceof SqmDeleteStatement<?> ) {
			return buildDeleteQueryPlan();
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			return buildUpdateQueryPlan();
		}
		else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
			return buildInsertQueryPlan();
		}
		else {
			throw new UnsupportedOperationException( "Query#executeUpdate for Statements of type [" + sqmStatement + "] not supported" );
		}
	}

	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final var concreteSqmStatements =
				QuerySplitter.split( (SqmDeleteStatement<?>) getSqmStatement() );
		return concreteSqmStatements.length > 1
				? buildAggregatedDeleteQueryPlan( concreteSqmStatements )
				: buildConcreteDeleteQueryPlan( concreteSqmStatements[0] );
	}

	private NonSelectQueryPlan buildConcreteDeleteQueryPlan(SqmDeleteStatement<?> deleteStatement) {
		final var entityDomainType = deleteStatement.getTarget().getModel();
		final String entityName = entityDomainType.getHibernateEntityName();
		final var persister = getMappingMetamodel().getEntityDescriptor( entityName );
		final var multiTableStrategy = persister.getSqmMultiTableMutationStrategy();
		return multiTableStrategy != null
				// NOTE: MultiTableDeleteQueryPlan and SqmMultiTableMutationStrategy already handle soft-deletes internally
				? new MultiTableDeleteQueryPlan( deleteStatement, domainParameterXref, multiTableStrategy )
				: new SimpleDeleteQueryPlan( persister, deleteStatement, domainParameterXref );
	}

	private NonSelectQueryPlan buildAggregatedDeleteQueryPlan(SqmDeleteStatement<?>[] deleteStatements) {
		final var aggregatedQueryPlans = new NonSelectQueryPlan[deleteStatements.length];
		for ( int i = 0, x = deleteStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteDeleteQueryPlan( deleteStatements[i] );
		}
		return new AggregatedNonSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		final var sqmUpdate = (SqmUpdateStatement<?>) getSqmStatement();
		final String entityName = sqmUpdate.getTarget().getModel().getHibernateEntityName();
		final var persister = getMappingMetamodel().getEntityDescriptor( entityName );
		final var multiTableStrategy = persister.getSqmMultiTableMutationStrategy();
		return multiTableStrategy == null
				? new SimpleNonSelectQueryPlan( sqmUpdate, domainParameterXref )
				: new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		final var sqmInsert = (SqmInsertStatement<?>) getSqmStatement();
		final String entityName = sqmInsert.getTarget().getModel().getHibernateEntityName();
		final var persister = getMappingMetamodel().getEntityDescriptor( entityName );
		if ( useMultiTableInsert( persister, sqmInsert ) ) {
			return new MultiTableInsertQueryPlan(
					sqmInsert,
					domainParameterXref,
					persister.getSqmMultiTableInsertStrategy()
			);
		}
		else if ( sqmInsert instanceof SqmInsertValuesStatement<?> insertValues
				&& insertValues.getValuesList().size() != 1
				&& !getSessionFactory().getJdbcServices().getDialect().supportsValuesListForInsert() ) {
			return buildAggregateInsertQueryPlan( insertValues );
		}
		else {
			return new SimpleNonSelectQueryPlan( sqmInsert, domainParameterXref );
		}
	}

	private NonSelectQueryPlan buildAggregateInsertQueryPlan(SqmInsertValuesStatement<?> insertValues) {
		// Split insert-values queries if the dialect doesn't support values lists
		final var valuesList = insertValues.getValuesList();
		final var planParts = new NonSelectQueryPlan[valuesList.size()];
		for ( int i = 0; i < valuesList.size(); i++ ) {
			final var subInsert = insertValues.copyWithoutValues( simpleContext() );
			subInsert.values( valuesList.get( i ) );
			planParts[i] = new SimpleNonSelectQueryPlan( subInsert, domainParameterXref );
		}
		return new AggregatedNonSelectQueryPlanImpl( planParts );
	}

	private boolean useMultiTableInsert(EntityPersister persister, SqmInsertStatement<?> sqmInsert) {
		boolean useMultiTableInsert = persister.hasMultipleTables();
		if ( !useMultiTableInsert && !isSimpleValuesInsert( sqmInsert, persister ) ) {
			final var identifierGenerator = persister.getGenerator();
			if ( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapableGenerator
				&& identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
				final var optimizer = optimizableGenerator.getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1
					|| !bulkInsertionCapableGenerator.supportsBulkInsertionIdentifierGeneration() ) {
					useMultiTableInsert = !hasIdentifierAssigned( sqmInsert, persister );
				}
			}
		}
		return useMultiTableInsert;
	}

	protected boolean hasIdentifierAssigned(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		final var identifierMapping = entityDescriptor.getIdentifierMapping();
		final String partName =
				identifierMapping instanceof SingleAttributeIdentifierMapping
						? identifierMapping.getAttributeName()
						: EntityIdentifierMapping.ID_ROLE_NAME;
		for ( var insertionTargetPath : sqmInsert.getInsertionTargetPaths() ) {
			if ( insertionTargetPath.getLhs() instanceof SqmRoot<?> ) {
				if ( insertionTargetPath.getReferencedPathSource().getPathName().equals( partName ) ) {
					return true;
				}
			}
		}

		return false;
	}

	protected boolean isSimpleValuesInsert(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		// Simple means that we can translate the statement to a single plain insert
		return sqmInsert instanceof SqmInsertValuesStatement
			// An insert is only simple if no SqmMultiTableMutation strategy is available,
			// as the presence of it means the entity has multiple tables involved,
			// in which case we currently need to use the MultiTableInsertQueryPlan
			&& entityDescriptor.getSqmMultiTableMutationStrategy() == null;
	}

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	public List<T> getResultList() {
		final var message = "Attempting to get a result list from a mutation query";
		throw new IllegalStateException( message, new IllegalSelectQueryException( message, hql ) );
	}

	@Override @SuppressWarnings("deprecation")
	@Nonnull
	public ScrollableResults<T> scroll(@Nonnull ScrollMode scrollMode) {
		final var message = "Attempting to scroll list from a mutation query";
		throw new IllegalStateException( message, new IllegalSelectQueryException( message, hql ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options

	@Override
	protected void verifySelectionOption(String name) {
		throw selectionOption( name );
	}

	private IllegalStateException selectionOption(String name) {
		final var msg = name + " is not supported for mutation queries";
		// JPA calls for ISE, but use that to wrap our more meaningful IllegalSelectQueryException
		return new IllegalStateException( msg, new IllegalSelectQueryException( msg, hql ) );
	}

	@Override
	@Deprecated @SuppressWarnings("deprecation")
	@Nullable
	public LockModeType getLockMode() {
		throw selectionOption( "Locking" );
	}

	@Override
	@Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public QueryImplementor<T> setLockMode(@Nonnull LockModeType lockMode) {
		if ( lockMode == LockModeType.NONE ) {
			// this is safe, according to the spec
			return this;
		}
		else {
			throw selectionOption( "Locking" );
		}
	}

	@Override
	@Nonnull
	public QueryImplementor<T> setHibernateLockMode(@Nonnull LockMode lockMode) {
		if ( lockMode == LockMode.NONE ) {
			// allow this just like spec
			return this;
		}
		else {
			throw selectionOption( "Locking" );
		}
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setComment(@Nullable String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> addQueryHint(@Nonnull String hint) {
		super.addQueryHint( hint );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		super.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> addOption(@Nonnull Statement.Option option) {
		OptionsHelper.applyOption( this, option );
		return this;
	}

	@Override
	@Nonnull
	public Set<Statement.Option> getOptions() {
		return OptionsHelper.getStatementOptions( getQueryOptions() );
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setTimeout(@Nullable Integer timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setTimeout(@Nullable Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setFlushMode(@Nonnull FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setHint(@Nonnull String hintName, @Nullable Object value) {
		super.setHint( hintName, value );
		return this;
	}

	private void irrelevantHint(String tag) {
		final var msg = tag + " is not relevant for MutationQuery";
		throw new IllegalArgumentException( msg, new IllegalSelectQueryException( msg, hql ) );
	}

	@Override
	protected void applyReadOnlyHint(String hintName, Object value) {
		irrelevantHint( "Read-only" );
	}

	@Override
	protected void applyFetchSizeHint(String hintName, Object value) {
		irrelevantHint( "Fetch-size" );
	}

	@Override
	protected void applyResultCachingHint(String hintName, Object value) {
		irrelevantHint( "Result-caching" );
	}

	@Override
	protected void applyResultCacheModeHint(String hintName, Object value) {
		irrelevantHint( "Result-caching" );
	}

	@Override
	protected void applyResultCachingRetrieveModeHint(String hintName, Object value) {
		irrelevantHint( "Result-caching" );
	}

	@Override
	protected void applyResultCachingStoreModeHint(String hintName, Object value) {
		irrelevantHint( "Result-caching" );
	}

	@Override
	protected void applyResultCachingRegionHint(String hintName, Object value) {
		irrelevantHint( "Result-caching" );
	}

	protected void applyEntityGraphHint(GraphSemantic graphSemantic, Object value, String hintName) {
		irrelevantHint( "Entity-graph" );
	}

	@Override
	protected void applyEnabledFetchProfileHint(String hintName, Object value) {
		irrelevantHint( "Fetch-profile" );
	}

	@Override
	protected void applyLockModeHint(String hintName, LockMode value) {
		if ( value != LockMode.NONE ) {
			irrelevantHint( "Locking" );
		}
		// else, per spec, just ignore this
	}

	@Override
	protected void applyLockTimeoutHint(String hintName, Object timeout) {
		irrelevantHint( "Locking" );
	}

	@Override
	protected void applyFollowOnStrategyHint(Object value) {
		irrelevantHint( "Locking" );
	}

	@Override
	@Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Query<T> setEntityGraph(@Nonnull EntityGraph<? super T> graph, @Nonnull GraphSemantic semantic) {
		throw new UnsupportedOperationException( "Entity graphs are not supported for mutation queries" );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Query<T> enableFetchProfile(@Nonnull String profileName) {
		throw new UnsupportedOperationException( "Fetch profiles are not supported for mutation queries" );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public Query<T> disableFetchProfile(@Nonnull String profileName) {
		throw new UnsupportedOperationException( "Fetch profiles are not supported for mutation queries" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling
	@Override
	@Nonnull
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	@Nonnull
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	@Nonnull
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameter(int position, @Nullable Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameters(@Nonnull Object... arguments) {
		super.setParameters( arguments );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(int position, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(int position, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameter(@Nonnull Parameter<P> parameter, @Nullable P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setProperties(@Nonnull @SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setProperties(@Nonnull Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameterList(int position, @Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(int position, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<T> setParameterList(int position, @Nonnull Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull Collection<? extends P> values, @Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> MutationQueryImplementor<T> setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull Parameter<Calendar> param, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public MutationQueryImplementor<T> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NamedMutationMemento<?> toMutationMemento(String name) {
		if ( CRITERIA_HQL_STRING.equals( queryStringCacheKey ) ) {
			return new CriteriaMutationMementoImpl<>(
					name,
					targetType,
					sqm,
					queryOptions.getFlushMode(),
					queryOptions.getTimeout(),
					getComment(),
					Map.of()
			);
		}
		else {
			return new HqlMutationMementoImpl<>(
					name,
					hql,
					targetType,
					Map.of(),
					queryOptions.getFlushMode(),
					queryOptions.getTimeout(),
					getComment(),
					Map.of()

			);
		}
	}
}
