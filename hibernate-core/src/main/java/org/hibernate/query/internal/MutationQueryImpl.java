/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Query;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.internal.CriteriaMutationMementoImpl;
import org.hibernate.query.named.internal.HqlMutationMementoImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
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

		validateQuery( targetType, sqm, hql );
	}

	private static <R> void validateQuery(Class<R> expectedResultType, SqmDmlStatement<R> sqm, String hql) {
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

		validateQuery( targetType, sqm, hql );
	}

	@Override
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

	public Class<T> getTargetType() {
		return targetType;
	}

	@Override
	public MutationQueryImplementor<T> asMutationQuery() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution

	@Override
	public int execute() {
		return executeUpdate();
	}

	@Override
	protected int doExecuteUpdate() {
		try {
			return resolveNonSelectQueryPlan().executeUpdate( this );
		}
		finally {
			domainParameterXref.clearExpansions();
		}
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
		// to get here the SQM statement has already been validated to be
		// a non-select variety...
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
			if ( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator
				&& identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
				final var optimizer = optimizableGenerator.getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
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

	@Override
	protected List<T> doList() {
		final var msg = "Attempting to get a result list from a mutation query";
		throw new IllegalStateException( msg, new IllegalSelectQueryException( msg, hql ) );
	}

	@Override
	protected ScrollableResultsImplementor<T> doScroll(ScrollMode scrollMode) {
		final var msg = "Attempting to scroll list from a mutation query";
		throw new IllegalStateException( msg, new IllegalSelectQueryException( msg, hql ) );
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
	public LockModeType getLockMode() {
		throw selectionOption( "Locking" );
	}

	@Override
	public QueryImplementor<T> setLockMode(LockModeType lockMode) {
		if ( lockMode == LockModeType.NONE ) {
			// this is safe according to the spec
			return this;
		}
		else {
			throw selectionOption( "Locking" );
		}
	}

	@Override
	public QueryImplementor<T> setHibernateLockMode(LockMode lockMode) {
		if ( lockMode == LockMode.NONE ) {
			// allow this just like spec
			return this;
		}
		else {
			throw selectionOption( "Locking" );
		}
	}

	@Override
	public Query<T> setLockTimeout(Timeout lockTimeout) {
		return super.setLockTimeout( lockTimeout );
	}

	@Override
	public QueryImplementor<T> setLockScope(PessimisticLockScope lockScope) {
		return super.setLockScope( lockScope );
	}

	@Override
	public QueryImplementor<T> setFollowOnLockingStrategy(Locking.FollowOn strategy) {
		return super.setFollowOnLockingStrategy( strategy );
	}

	@Override
	public MutationQueryImplementor<T> setComment(String comment) {
		return (MutationQueryImplementor<T>) super.setComment( comment );
	}

	@Override
	public MutationQueryImplementor<T> addQueryHint(String hint) {
		return (MutationQueryImplementor<T>) super.addQueryHint( hint );
	}

	@Override
	public MutationQueryImplementor<T> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		return (MutationQueryImplementor<T>) super.setQueryFlushMode( queryFlushMode );
	}

	@Override
	public MutationQueryImplementor<T> setTimeout(int timeout) {
		return (MutationQueryImplementor<T>) super.setTimeout( timeout );
	}

	@Override
	public MutationQueryImplementor<T> setTimeout(Integer timeout) {
		return (MutationQueryImplementor<T>) super.setTimeout( timeout );
	}

	@Override
	public MutationQueryImplementor<T> setTimeout(Timeout timeout) {
		return (MutationQueryImplementor<T>) super.setTimeout( timeout );
	}

	@Override
	public MutationQueryImplementor<T> setFlushMode(FlushModeType flushMode) {
		return (MutationQueryImplementor<T>) super.setFlushMode( flushMode );
	}

	@Override
	public MutationQueryImplementor<T> setHint(String hintName, Object value) {
		return (MutationQueryImplementor<T>) super.setHint( hintName, value );
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
		if ( value == LockMode.NONE ) {
			// per spec, just ignore this
		}
		else {
			irrelevantHint( "Locking" );
		}
	}

	@Override
	protected void applyLockTimeoutHint(String hintName, Object timeout) {
		irrelevantHint( "Locking" );
	}

	@Override
	protected void applyFollowOnStrategyHint(Object value) {
		irrelevantHint( "Locking" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter handling

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	@Override
	public MutationQueryImplementor<T> setParameter(String name, Object value) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(String name, P value, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(String name, P value, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value, type );
	}

	@Override
	public MutationQueryImplementor<T> setParameter(int position, Object value) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(int position, P value, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(int position, P value, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value, type );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P value) {
		return (MutationQueryImplementor<T>) super.setParameter( parameter, value );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameter( parameter, value, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameter( parameter, value, type );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameter(Parameter<P> parameter, P value) {
		return (MutationQueryImplementor<T>) super.setParameter( parameter, value );
	}

	@Override
	public <P> MutationQueryImplementor<T> setConvertedParameter(String name, P value, Class<? extends AttributeConverter<P, ?>> converter) {
		return (MutationQueryImplementor<T>) super.setConvertedParameter( name, value, converter );
	}

	@Override
	public <P> MutationQueryImplementor<T> setConvertedParameter(int position, P value, Class<? extends AttributeConverter<P, ?>> converter) {
		return (MutationQueryImplementor<T>) super.setConvertedParameter( position, value, converter );
	}

	@Override
	public MutationQueryImplementor<T> setProperties(@SuppressWarnings("rawtypes") Map map) {
		return (MutationQueryImplementor<T>) super.setProperties( map );
	}

	@Override
	public MutationQueryImplementor<T> setProperties(Object bean) {
		return (MutationQueryImplementor<T>) super.setProperties( bean );
	}

	@Override
	public MutationQueryImplementor<T> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(String name, Collection<? extends P> values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values, type );
	}

	@Override
	public MutationQueryImplementor<T> setParameterList(String name, Object[] values) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(String name, P[] values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(String name, P[] values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( name, values, type );
	}

	@Override
	public MutationQueryImplementor<T> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(int position, Collection<? extends P> values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values, type );
	}

	@Override
	public MutationQueryImplementor<T> setParameterList(int position, Object[] values) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(int position, P[] values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(int position, P[] values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( position, values, type );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values, type );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> MutationQueryImplementor<T> setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		return (MutationQueryImplementor<T>) super.setParameterList( parameter, values, type );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(String name, Instant value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(int position, Instant value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(Parameter param, Calendar value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( param, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(Parameter param, Date value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( param, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(String name, Calendar value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(String name, Date value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( name, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(int position, Calendar value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value, temporalType );
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public MutationQueryImplementor<T> setParameter(int position, Date value, TemporalType temporalType) {
		return (MutationQueryImplementor<T>) super.setParameter( position, value, temporalType );
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
