/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.Type;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.util.OptionsHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.Output;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.procedure.NoMoreOutputsException;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.NamedCallableQueryMemento.ParameterMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.procedure.ResultSetOutput;
import org.hibernate.procedure.UpdateCountOutput;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.hibernate.type.OutputableType;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.internal.util.collections.CollectionHelper.makeCopy;
import static org.hibernate.procedure.internal.NamedCallableQueryMementoImpl.ParameterMementoImpl.fromRegistration;
import static org.hibernate.query.results.spi.ResultSetMapping.resolveResultSetMapping;
import static org.hibernate.type.descriptor.converter.internal.ConverterHelper.createConvertedParameterType;

/**
 * Standard implementation of {@link ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractQuery<R>
		implements ProcedureCallImplementor<R> {

	@Override
	@Nonnull
	public ProcedureCallImpl<R> addOption(@Nonnull Option option) {
		checkNotClosed();
		OptionsHelper.applyOption( this, option );
		return this;
	}

	@Override
	@Nonnull
	public Set<Option> getOptions() {
		checkNotClosed();
		return OptionsHelper.getStoredProcedureOptions( getQueryOptions() );
	}

	private final String procedureName;

	private FunctionReturnImpl<?> functionReturn;

	private final ProcedureParameterMetadataImplementor parameterMetadata;
	private final ProcedureParamBindings parameterBindings;

	private final List<ResultSetMapping> resultSetMappings;

	private Set<String> synchronizedQuerySpaces;

	private OutputsImpl outputs;
	private boolean closed;

	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(
			SharedSessionContractImplementor session,
			String procedureName) {
		super( session );
		this.procedureName = procedureName;
		this.synchronizedQuerySpaces = null;
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, session.getFactory() );
		this.resultSetMappings = List.of( resolveResultSetMapping( procedureName, true, session.getFactory() ) );
	}

	/**
	 * The result Class(es) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultClasses The classes making up the result
	 */
	public ProcedureCallImpl(
			SharedSessionContractImplementor session,
			String procedureName,
			Class<?>... resultClasses) {
		super( session );
		this.procedureName = procedureName;
		synchronizedQuerySpaces = new HashSet<>();
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, session.getFactory() );
		resultSetMappings = Util.resolveResultSetMappings(
				procedureName,
				resultClasses,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
	}

	/**
	 * The result-set-mapping(s) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultSetMappingNames The names of the result set mappings making up the result
	 */
	public ProcedureCallImpl(
			final SharedSessionContractImplementor session,
			String procedureName,
			String... resultSetMappingNames) {
		super( session );
		this.procedureName = procedureName;
		synchronizedQuerySpaces = new HashSet<>();
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, session.getFactory() );
		resultSetMappings = Util.resolveResultSetMappings(
				procedureName,
				resultSetMappingNames,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	ProcedureCallImpl(
			SharedSessionContractImplementor session,
			NamedCallableQueryMemento memento) {
		super(session);
		procedureName = memento.getCallableName();
		synchronizedQuerySpaces = makeCopy( memento.getQuerySpaces() );
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, session.getFactory() );
		resultSetMappings = Util.resolveResultSetMappings(
				procedureName,
				memento.getResultSetMappingNames(),
				memento.getResultSetMappingClasses(),
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
		registerParameters( session, memento );
		applyMementoOptions( memento );
	}

	public ProcedureCallImpl(
			SharedSessionContractImplementor session,
			NamedCallableQueryMementoImpl memento,
			String... resultSetMappingNames) {
		super( session );
		this.procedureName = memento.getCallableName();
		this.synchronizedQuerySpaces = makeCopy( memento.getQuerySpaces() );
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, session.getFactory() );
		resultSetMappings = Util.resolveResultSetMappings(
				procedureName,
				resultSetMappingNames,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
		registerParameters( session, memento );
		applyMementoOptions( memento );
	}

	@Override
	@Nonnull
	public String getProcedureName() {
		return procedureName;
	}

	@Override
	@Nullable
	public String getQueryString() {
		return "ProcedureCall::" + procedureName;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Casts

	@Override
	@Nonnull
	public SelectionQueryImplementor<R> asSelectionQuery() {
		throw new IllegalArgumentException( "ProcedureCall cannot be treated as SelectionQuery" );
	}

	@Override
	@Nonnull
	public <X> SelectionQueryImplementor<X> asSelectionQuery(Class<X> type) {
		throw new IllegalArgumentException( "ProcedureCall cannot be treated as SelectionQuery" );
	}

	@Override
	@Nonnull
	public <X> SelectionQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph) {
		throw new IllegalArgumentException( "ProcedureCall cannot be treated as SelectionQuery" );
	}

	@Override
	@Nonnull
	public <X> SelectionQueryImplementor<X> withResultSetMapping(@Nonnull jakarta.persistence.sql.ResultSetMapping<X> mapping) {
		throw new IllegalArgumentException( "ProcedureCall cannot be treated as SelectionQuery" );
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<R> asMutationQuery() {
		throw new IllegalArgumentException( "ProcedureCall cannot be treated as MutationQuery" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Options
	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setComment(@Nullable String comment) {
		checkNotClosed();
		super.setComment( comment );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		checkNotClosed();
		super.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setMaxResults(int maxResult) {
		checkNotClosed();
		super.setMaxResults( maxResult );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setFirstResult(int startPosition) {
		checkNotClosed();
		super.setFirstResult( startPosition );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setLockMode(@Nonnull LockModeType lockMode) {
		throw new IllegalStateException( "Locking not supported for ProcedureCall" );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> addQueryHint(@Nonnull String hint) {
		checkNotClosed();
		super.addQueryHint( hint );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Query<R> setEntityGraph(@Nonnull EntityGraph<? super R> graph, @Nonnull GraphSemantic semantic) {
		throw new UnsupportedOperationException( "Entity graph not supported for ProcedureCall" );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Query<R> enableFetchProfile(@Nonnull String profileName) {
		throw new UnsupportedOperationException( "Fetch profiles not supported for ProcedureCall" );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Query<R> disableFetchProfile(@Nonnull String profileName) {
		throw new UnsupportedOperationException( "Fetch profiles not supported for ProcedureCall" );
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return false;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		throw new UnsupportedOperationException( "Not supported" );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setTimeout(@Nullable Integer timeout) {
		checkNotClosed();
		if ( timeout == null ) {
			timeout = -1;
		}
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setTimeout(int timeout) {
		checkNotClosed();
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setTimeout(@Nullable Timeout timeout) {
		checkNotClosed();
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Deprecated @SuppressWarnings("removal")
	@Nullable
	public LockModeType getLockMode() {
		// the JPA spec requires IllegalStateException here, even
		// though it's logically an UnsupportedOperationException
		throw new IllegalStateException( "Illegal attempt to get lock mode for a procedure call" );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setFlushMode(@Nonnull FlushModeType flushModeType) {
		checkNotClosed();
		super.setFlushMode( flushModeType );
		return this;
	}

	public List<ResultSetMapping> getResultSetMappings() {
		return resultSetMappings;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		checkNotClosed();
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		checkNotClosed();
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setHint(@Nonnull String hintName, @Nullable Object value) {
		checkNotClosed();
		super.setHint( hintName, value );
		return this;
	}

	@Override
	protected void applyCallableFunctionHint(String hintName, Object value) {
		if ( value instanceof Boolean bool && bool
			|| value instanceof String string && parseBoolean( string ) ) {
			applyCallableFunctionHint();
		}
		else {
			throw new IllegalArgumentException( "Illegal value for hint '" + hintName + "'" );
		}
	}

	@Override @SuppressWarnings("resource")
	protected void applyCallableFunctionTypeHint(String hintName, Object value) {
		if ( value instanceof Integer code ) {
			markAsFunctionCall( code );
		}
		else if ( value instanceof Type<?> type ) {
			markAsFunctionCall( type );
		}
		else if ( value instanceof Class<?> type ) {
			markAsFunctionCall( type );
		}
		else if ( value instanceof String string ) {
			markAsFunctionCall( parseInt( string ) );
		}
		else {
			throw new IllegalArgumentException( "Illegal value for hint '" + hintName + "'" );
		}
	}

	private void irrelevantHint(String tag) {
		throw new IllegalArgumentException( tag + " is not relevant for ProcedureCall" );
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

	@Override
	protected void applyEntityGraphHint(GraphSemantic graphSemantic, Object value, String hintName) {
		irrelevantHint( "Entity-graph" );
	}

	@Override
	protected void applyEnabledFetchProfileHint(String hintName, Object value) {
		irrelevantHint( "Fetch-profile" );
	}

	@Override
	protected void applyLockModeHint(String hintName, LockMode value) {
		irrelevantHint( "Locking" );
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
	protected void applyQueryPlanCachingHint(String hintName, Object value) {
		irrelevantHint( "Caching of query plans" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter Handling
	@Override
	@Nonnull
	public ProcedureParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	@Nonnull
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	@Override
	@Nonnull
	protected QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Nonnull
	@Override
	public ParameterStrategy getParameterStrategy() {
		return getParameterMetadata().getParameterStrategy();
	}

	@Override
	@Nullable
	public FunctionReturnImplementor<?> getFunctionReturn() {
		checkNotClosed();
		return functionReturn;
	}

	@Override
	@Nonnull
	public ProcedureParameterImplementor<?> getParameterRegistration(@Nonnull String name) {
		checkNotClosed();
		return parameterMetadata.getQueryParameter( name );
	}

	@Override
	@Nonnull
	public List<ProcedureParameter<?>> getRegisteredParameters() {
		checkNotClosed();
		return unmodifiableList( parameterMetadata.getRegistrationsAsList() );
	}

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		return false;
	}

	private void registerParameters(SharedSessionContractImplementor session, NamedCallableQueryMemento memento) {
		for ( var parameterMemento : memento.getParameterMementos() ) {
			registerParameter( parameterMemento.resolve( session ) );
		}
	}

	private void applyCallableFunctionHint() {
		assert !resultSetMappings.isEmpty();
		if ( resultSetMappings.size() > 1 ) {
			markAsFunctionCallRefRefCursor();
		}
		else {
			final List<Class<?>> resultTypes = new ArrayList<>();
			resultSetMappings.get( 0 ).visitResultBuilders(
					(index, resultBuilder) -> resultTypes.add( resultBuilder.getJavaType() )
			);
			if ( resultTypes.size() == 1 ) {
				final var basicType = getTypeConfiguration()
						.getBasicTypeForJavaType( resultTypes.get( 0 ) );
				if ( basicType != null ) {
					markAsFunctionCall( basicType );
				}
				else {
					markAsFunctionCallRefRefCursor();
				}
			}
			else {
				markAsFunctionCallRefRefCursor();
			}
		}
	}

	@Override
	public boolean isFunctionCall() {
		return functionReturn != null;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> markAsFunctionCall(int sqlType) {
		checkNotClosed();
		functionReturn = new FunctionReturnImpl<>( this, sqlType );
		return this;
	}

	private void markAsFunctionCallRefRefCursor() {
		functionReturn = new FunctionReturnImpl<>( this, Types.REF_CURSOR );
	}

	@Override
	@Nonnull
	public <T> FunctionReturnImplementor<T> registerResultParameter(@Nonnull Class<T> aClass) {
		checkNotClosed();
		//noinspection resource
		markAsFunctionCall( aClass );
		//noinspection unchecked
		return (FunctionReturnImplementor<T>) functionReturn;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> markAsFunctionCall(@Nonnull Class<?> resultType) {
		checkNotClosed();
		final var basicType = getTypeConfiguration().getBasicTypeForJavaType( resultType );
		if ( basicType == null ) {
			throw new IllegalArgumentException( "Could not resolve a BasicType for the Java type: " + resultType.getName() );
		}
		markAsFunctionCall( basicType );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCall markAsFunctionCall(@Nonnull Type<?> typeReference) {
		checkNotClosed();
		if ( !(typeReference instanceof OutputableType<?> outputableType) ) {
			throw new IllegalArgumentException( "Given type is not an OutputableType: " + typeReference );
		}

		assert !resultSetMappings.isEmpty();
		final var firstResultSetMapping = resultSetMappings.get( 0 );
		if ( firstResultSetMapping.getNumberOfResultBuilders() == 0 ) {
			final var expressible = resolveExpressible( typeReference );
			// Function returns might not be represented as callable parameters,
			// but we still want to convert the result to the requested java type if possible
			firstResultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( expressible.getExpressibleJavaType() ) );
		}
		functionReturn = new FunctionReturnImpl<>( this, (OutputableType<?>) outputableType );
		return this;
	}

	private void markAsFunctionCall(BasicType<?> basicType) {
		assert !resultSetMappings.isEmpty();
		final var firstResultSetMapping = resultSetMappings.get( 0 );
		if ( firstResultSetMapping.getNumberOfResultBuilders() == 0 ) {
			// Function returns might not be represented as callable parameters,
			// but we still want to convert the result to the requested java type if possible
			firstResultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( basicType.getExpressibleJavaType() ) );
		}
		functionReturn = new FunctionReturnImpl<>( this, (OutputableType<?>) basicType );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			int position,
			@Nonnull Class<?> type,
			@Nonnull ParameterMode mode) {
		checkNotClosed();
		getSession().checkOpen( true );
		try {
			if ( AttributeConverter.class.isAssignableFrom( type ) ) {
				// Hack to deal with HHH-12661
				registerParameterWithConverter( position, getConvertedType( type ), mode );
			}
			else {
				registerParameter( position, type, mode );
			}
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			String parameterName,
			@Nonnull Class<?> type,
			@Nonnull ParameterMode mode) {
		checkNotClosed();
		getSession().checkOpen( true );
		try {
			if ( AttributeConverter.class.isAssignableFrom( type ) ) {
				// Hack to deal with HHH-12661
				registerParameterWithConverter( parameterName, getConvertedType( type ), mode );
			}
			else {
				registerParameter( parameterName, type, mode );
			}
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
		return this;
	}

	private BasicType<?> getConvertedType(Class<?> type) {
		final var convertedType =
				getNodeBuilder().getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( type.getTypeName() );
		if ( convertedType == null ) {
			throw new IllegalArgumentException( "Unregistered converter type: " + type.getName() );
		}
		return convertedType;
	}

	private <C> void registerParameterWithConverter(int position, BasicType<C> convertedType, ParameterMode mode) {
		registerParameter( new ProcedureParameterImpl<>(
				position,
				mode,
				getExpressibleJavaType( convertedType ),
				convertedType
		) );
	}

	private <C> void registerParameterWithConverter(String name, BasicType<C> convertedType, ParameterMode mode) {
		registerParameter( new ProcedureParameterImpl<>(
				name,
				mode,
				getExpressibleJavaType( convertedType ),
				convertedType
		) );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			int position,
			@Nonnull Type<?> type,
			@Nonnull ParameterMode mode) {
		checkNotClosed();
		getSession().checkOpen( true );

		try {
			registerParameter( position, type, mode );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			String parameterName,
			@Nonnull Type<?> type,
			@Nonnull ParameterMode mode) {
		checkNotClosed();
		getSession().checkOpen( true );
		try {
			registerParameter( parameterName, type, mode );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
		return this;
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerParameter(int position, Class<T> javaType, ParameterMode mode) {
		final var parameterType = getMappingMetamodel().resolveParameterBindType( javaType );
		final var procedureParameter =
				new ProcedureParameterImpl<>( position, mode, getExpressibleJavaType( parameterType ), parameterType );
		registerParameter( procedureParameter );
		return procedureParameter;
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerParameter(
			int position,
			@Nonnull Type<T> typeReference,
			@Nonnull ParameterMode mode) {
		final var expressible = resolveExpressible( typeReference );
		final var procedureParameter =
				new ProcedureParameterImpl<>( position, mode, typeReference.getJavaType(), expressible );
		registerParameter( procedureParameter );
		return procedureParameter;
	}

	private <T> SqmBindableType<T> resolveExpressible(@Nonnull Type<T> typeReference) {
		return getSessionFactory().getRuntimeMetamodels().resolveExpressible( typeReference );
	}

	private void registerParameter(ProcedureParameterImplementor<?> parameter) {
		parameterMetadata.registerParameter( parameter );
	}

	@Override
	@Nonnull
	public ProcedureParameterImplementor<?> getParameterRegistration(int position) {
		checkNotClosed();
		return parameterMetadata.getQueryParameter( position );
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerParameter(@Nonnull String name, @Nonnull Class<T> javaType, @Nonnull ParameterMode mode) {
		checkNotClosed();
		final var parameterType = getMappingMetamodel().resolveParameterBindType( javaType );
		final var parameter =
				new ProcedureParameterImpl<>( name, mode, getExpressibleJavaType( parameterType ), parameterType );
		registerParameter( parameter );
		return parameter;
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerConvertedParameter(int position, @Nonnull Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode) {
		checkNotClosed();
		final var convertedType = createConvertedParameterType(
				converter,
				getSession().getFactory().getServiceRegistry(),
				getNodeBuilder().getTypeConfiguration()
		);
		final var parameter = new ProcedureParameterImpl<>(
				position,
				mode,
				getExpressibleJavaType( convertedType ),
				convertedType
		);
		registerParameter( parameter );
		return parameter;
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerConvertedParameter(@Nonnull String parameterName, @Nonnull Class<? extends AttributeConverter<T, ?>> converter, ParameterMode mode) {
		checkNotClosed();
		final var convertedType = createConvertedParameterType(
				converter,
				getSession().getFactory().getServiceRegistry(),
				getNodeBuilder().getTypeConfiguration()
		);
		final var parameter = new ProcedureParameterImpl<>(
				parameterName,
				mode,
				getExpressibleJavaType( convertedType ),
				convertedType
		);
		registerParameter( parameter );
		return parameter;
	}

	private <T> Class<T> getExpressibleJavaType(Type<T> parameterType) {
		if ( parameterType == null ) {
			return null;
		}
		else {
			final var sqmExpressible = getNodeBuilder().resolveExpressible( parameterType );
			assert sqmExpressible != null;
			return sqmExpressible.getExpressibleJavaType().getJavaTypeClass();
		}
	}

	@Override
	@Nonnull
	public <T> ProcedureParameterImplementor<T> registerParameter(
			String name,
			@Nonnull Type<T> typeReference,
			@Nonnull ParameterMode mode) {
		checkNotClosed();
		final var parameter =
				new ProcedureParameterImpl<>( name, mode, typeReference.getJavaType(),
						resolveExpressible( typeReference ) );
		registerParameter( parameter );
		return parameter;
	}



	/**
	 * Use this form instead of {@link #getSynchronizedQuerySpaces()} when you want to make sure the
	 * underlying Set is instantiated (aka, on add)
	 *
	 * @return The spaces
	 */
	protected Set<String> synchronizedQuerySpaces() {
		if ( synchronizedQuerySpaces == null ) {
			synchronizedQuerySpaces = new HashSet<>();
		}
		return synchronizedQuerySpaces;
	}

	@Override
	public Set<String> getSynchronizedQuerySpaces() {
		return synchronizedQuerySpaces == null ? emptySet() : unmodifiableSet( synchronizedQuerySpaces );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		synchronizedQuerySpaces().add( querySpace );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> addSynchronizedEntityName(String entityName) {
		final var entityDescriptor = getMappingMetamodel().getEntityDescriptor( entityName );
		addSynchronizedQuerySpaces( entityDescriptor );
		return this;
	}

	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) {
		final var entityDescriptor = getMappingMetamodel().getEntityDescriptor( entityClass );
		addSynchronizedQuerySpaces( entityDescriptor );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Memento

	@Override
	@Nonnull
	public NamedCallableQueryMemento toMemento(String name) {
		return new NamedCallableQueryMementoImpl(
				name,
				procedureName,
				getParameterStrategy(),
				toParameterMementos( parameterMetadata ),
				// todo (6.0) : result-set-mapping names
				null,
				// todo (6.0) : result-set-mapping class names
				null,
				getSynchronizedQuerySpaces(),
				queryOptions.isResultCachingEnabled(),
				queryOptions.getResultCacheRegionName(),
				queryOptions.getCacheMode(),
				queryOptions.getQueryFlushMode(),
				queryOptions.isReadOnly(),
				queryOptions.getTimeout(),
				queryOptions.getFetchSize(),
				queryOptions.getComment(),
				getHints()
		);
	}

	private static List<ParameterMemento> toParameterMementos(
			ProcedureParameterMetadataImplementor parameterMetadata) {
		if ( parameterMetadata.getParameterStrategy() == ParameterStrategy.UNKNOWN ) {
			return emptyList();
		}
		else {
			final List<ParameterMemento> mementos = new ArrayList<>();
			parameterMetadata.visitRegistrations(
					queryParameter -> mementos.add(
							fromRegistration( (ProcedureParameterImplementor<?>) queryParameter ) )
			);
			return mementos;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution / Outputs

	@Override
	protected void prepareForExecution() {
	}

	@Override
	@Nonnull
	public ProcedureOutputs getOutputs() {
		checkNotClosed();
		if ( outputs == null ) {
			outputs = buildOutputs();
		}
		return outputs;
	}

	@Override
	public void close() {
		checkNotClosed();
		closed = true;
		if ( outputs != null ) {
			outputs.release();
		}
	}

	private void checkNotClosed() {
		if ( closed ) {
			throw new IllegalStateException( "StoredProcedureQuery is closed" );
		}
	}

	private OutputsImpl buildOutputs() {
		// todo : going to need a very specialized Loader for this.
		// or, might be a good time to look at splitting Loader up into:
		//		1) building statement objects
		//		2) executing statement objects
		//		3) processing result sets

		// for now assume there are no resultClasses nor mappings defined
		// 	TOTAL PROOF-OF-CONCEPT!!!!!!

		// todo : how to identify calls which should be in the form `{? = call procName...}` ??? (note leading param marker)
		// 		more than likely this will need to be a method on the native API.  I can see this as a trigger to
		//		both: (1) add the `? = ` part and also (2) register a REFCURSOR parameter for DBs (Oracle, PGSQL) that
		//		need it.

		final var jdbcServices = getSession().getJdbcServices();
		final var callableStatementSupport =
				jdbcServices.getJdbcEnvironment().getDialect()
						.getCallableStatementSupport();

		final var call = callableStatementSupport.interpretCall( this );

		final var parameterRegistrations = collectParameterRegistrations( call );
		final var refCursorExtractors = collectRefCursorExtractors( call );

		final var jdbcCoordinator = getSession().getJdbcCoordinator();

		final String sqlString = call.getSqlString();
		final var statement =
				jdbcCoordinator.getStatementPreparer()
						.prepareCallableStatement( sqlString );
		try {
			// Register the parameter mode and type
			callableStatementSupport.registerParameters(
					procedureName,
					call,
					statement,
					parameterMetadata,
					getSession()
			);

			final var jdbcParameterBindings = parameterBindings( parameterRegistrations );
			final var executionContext = new OutputsExecutionContext( getSession() );

			// Note that this should actually happen in an executor

			int paramBindingPosition = call.getFunctionReturn() == null ? 1 : 2;
			for ( var parameterBinder : call.getParameterBinders() ) {
				parameterBinder.bindParameterValue(
						statement,
						paramBindingPosition,
						jdbcParameterBindings,
						executionContext
				);
				paramBindingPosition++;
			}
		}
		catch (SQLException e) {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( statement );
			jdbcCoordinator.afterStatementExecution();
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Error registering CallableStatement parameters",
					procedureName
			);
		}

		return new OutputsImpl(
				this,
				parameterRegistrations,
				refCursorExtractors.toArray( new JdbcCallRefCursorExtractor[0] ),
				statement,
				sqlString
		);

	}

	private Map<ProcedureParameter<?>, JdbcCallParameterRegistration> collectParameterRegistrations(JdbcOperationQueryCall call) {
		final Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations = new IdentityHashMap<>();
		final var funReturn = call.getFunctionReturn();
		if ( funReturn != null ) {
			parameterRegistrations.put( functionReturn, funReturn );
		}
		final var registrations = getParameterMetadata().getRegistrationsAsList();
		final var jdbcParameters = call.getParameterRegistrations();
		for ( int i = 0; i < registrations.size(); i++ ) {
			final var jdbcCallParameterRegistration = jdbcParameters.get( i );
			parameterRegistrations.put( registrations.get( i ), jdbcCallParameterRegistration );
		}
		return parameterRegistrations;
	}

	private List<JdbcCallRefCursorExtractor> collectRefCursorExtractors(JdbcOperationQueryCall call) {
		final List<JdbcCallRefCursorExtractor> refCursorExtractors = new ArrayList<>();
		final var funReturn = call.getFunctionReturn();
		if ( funReturn != null ) {
			final var refCursorExtractor = funReturn.getRefCursorExtractor();
			if ( refCursorExtractor != null ) {
				refCursorExtractors.add( refCursorExtractor );
			}
		}
		final var registrations = getParameterMetadata().getRegistrationsAsList();
		final var jdbcParameters = call.getParameterRegistrations();
		for ( int i = 0; i < registrations.size(); i++ ) {
			final var refCursorExtractor = jdbcParameters.get( i ).getRefCursorExtractor();
			if ( refCursorExtractor != null ) {
				refCursorExtractors.add( refCursorExtractor );
			}
		}
		return refCursorExtractors;
	}

	private JdbcParameterBindings parameterBindings(
			Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations) {
		final var jdbcParameterBindings =
				new JdbcParameterBindingsImpl( parameterRegistrations.size() );
		for ( var entry : parameterRegistrations.entrySet() ) {
			final var registration = entry.getValue();
			final var parameterBinder = registration.getParameterBinder();
			if ( parameterBinder != null ) {
				final var parameter = entry.getKey();
				final var binding = getParameterBindings().getBinding( parameter );
				if ( !binding.isBound() ) {
					if ( parameter.isNamed() ) {
						throw new IllegalArgumentException( "The parameter named '" + parameter + "' was not set" );
					}
					else {
						throw new IllegalArgumentException( "The parameter at position " + parameter + " was not set" );
					}
				}
				final var parameterType = (JdbcMapping) registration.getParameterType();
				jdbcParameterBindings.addBinding(
						(JdbcParameter) parameterBinder,
						new JdbcParameterBindingImpl( parameterType,
								parameterType.convertToRelationalValue( binding.getBindValue() ) )
				);
			}
		}
		return jdbcParameterBindings;
	}

	@Override
	public boolean execute() {
		checkNotClosed();
		try {
			return getOutputs().getCurrent() instanceof ResultSetOutput;
		}
		catch (NoMoreOutputsException e) {
			return false;
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	@Override @SuppressWarnings("removal")
	public int executeUpdate() {
		try {
			execute();
			// The expectation is that there is just one Output,
			// of type UpdateCountOutput
			return getUpdateCount();
		}
		finally {
			getOutputs().release();
		}
	}

	@Override
	public Object getOutputParameterValue(int position) {
		checkNotClosed();
		// According to spec, an exception thrown from this method should not mark for rollback.
		try {
			return getOutputs().getOutputParameterValue( position );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public Object getOutputParameterValue(@Nonnull String parameterName) {
		checkNotClosed();
		// According to spec, an exception thrown from this method should not mark for rollback.
		try {
			return getOutputs().getOutputParameterValue( parameterName );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public <T> T getOutputParameterValue(@Nonnull Parameter<T> parameter) {
		checkNotClosed();
		return getOutputs().getOutputParameterValue( (ProcedureParameter<T>) parameter );
	}

	@Override
	public boolean hasMoreResults() {
		checkNotClosed();
		final var outputs = getOutputs();
		return outputs.goToNext()
			&& outputs.getCurrent() instanceof ResultSetOutput;
	}

	@Override
	public int getUpdateCount() {
		checkNotClosed();
		try {
			final var output = getOutputs().getCurrent();
			if ( output == null ) {
				return -1;
			}
			else if ( output instanceof UpdateCountOutput updateCount ) {
				return updateCount.getUpdateCount();
			}
			else {
				return -1;
			}
		}
		catch (NoMoreOutputsException e) {
			return -1;
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	protected <X> List<X> results(Function<Output,List<X>> supplier) {
		if ( getMaxResults() == 0 ) {
			// EARLY EXIT!!!
			return emptyList();
		}

		try {
			return supplier.apply( getOutputs().getCurrent() );
		}
		catch (NoMoreOutputsException e) {
			// TODO: the spec is completely silent on these type of edge-case scenarios.
			// Essentially here we'd have a case where there are no more results
			// (ResultSets nor updateCount) but getResultList() was called.
			return null;
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	@Nonnull
	public List<R> getResultList() {
		checkNotClosed();
		final var results = results( output -> output.asResultSetOutput().getResultList() );
		//noinspection unchecked
		return (List<R>) results;
	}

	@Override
	@Nullable
	public <R1> List<R1> getResultList(@Nonnull Class<R1> type) {
		checkNotClosed();
		return results( output -> output.asResultSetOutput( type ).getResultList() );
	}

	@Override
	@Nullable
	public <R1> List<R1> getResultList(@Nonnull jakarta.persistence.sql.ResultSetMapping<R1> resultSetMapping) {
		checkNotClosed();
		return results( output -> output.asResultSetOutput( resultSetMapping ).getResultList() );
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public ScrollableResults<R> scroll(@Nonnull ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "Cannot scroll a ProcedureCall" );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Stream<R> getResultStream() {
		return stream();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public Stream<R> stream() {
		checkNotClosed();
		return getResultList().stream();
	}

	@Override
	@Nullable
	public <R1> R1 getSingleResult(@Nonnull Class<R1> aClass) {
		checkNotClosed();
		try {
			final var list = getResultList( aClass );
			return list == null ? null : getSingleResult( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	@Override
	@Nullable
	public <R1> R1 getSingleResult(@Nonnull jakarta.persistence.sql.ResultSetMapping<R1> resultSetMapping) {
		checkNotClosed();
		try {
			final var list = getResultList( resultSetMapping );
			return list == null ? null : getSingleResult( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	private <R1> R1 getSingleResult(List<R1> results) {
		if ( results.isEmpty() ) {
			throw new NoResultException( "No result found for query [" + getQueryString() + "]" );
		}
		return uniqueElement( results );
	}

	@Override
	@Nullable
	public <R1> R1 getSingleResultOrNull(@Nonnull Class<R1> aClass) {
		checkNotClosed();
		try {
			final var list = getResultList( aClass );
			return list == null ? null : uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	@Override
	@Nullable
	public <R1> R1 getSingleResultOrNull(@Nonnull jakarta.persistence.sql.ResultSetMapping<R1> resultSetMapping) {
		checkNotClosed();
		try {
			final var list = getResultList( resultSetMapping );
			return list == null ? null : uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	@Override
	protected <X> X unwrapDelegates(Class<X> type) {
		if ( type.isInstance( getOutputs() ) ) {
			return type.cast( getOutputs() );
		}

		return super.unwrapDelegates( type );
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value) {
		checkNotClosed();
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(@Nonnull Parameter<P> parameter, @Nullable P value) {
		checkNotClosed();
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Object value) {
		checkNotClosed();
		super.setParameter( name, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> javaTypeClass) {
		checkNotClosed();
		super.setParameter( name, value, javaTypeClass );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(int position, @Nullable Object value) {
		checkNotClosed();
		super.setParameter( position, value );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setParameters(@Nonnull Object... arguments) {
		checkNotClosed();
		super.setParameters( arguments );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Class<P> javaTypeClass) {
		checkNotClosed();
		super.setParameter( position, value, javaTypeClass );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(
			@Nonnull QueryParameter<P> parameter,
			@Nullable P value,
			@Nonnull Type<P> type) {
		checkNotClosed();
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type) {
		checkNotClosed();
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setParameter(int position, @Nullable P value, @Nonnull Type<P> type) {
		checkNotClosed();
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setProperties(@Nonnull @SuppressWarnings("rawtypes") Map map) {
		checkNotClosed();
		super.setProperties( map );
		return this;
	}

	@Override
	@Nonnull
	public ProcedureCallImplementor<R> setProperties(@Nonnull Object bean) {
		checkNotClosed();
		super.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setConvertedParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		checkNotClosed();
		super.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public <P> ProcedureCallImplementor<R> setConvertedParameter(int position, @Nullable P value, @Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		checkNotClosed();
		super.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(
			@Nonnull Parameter<Calendar> parameter,
			@Nullable Calendar value,
			@Nonnull TemporalType temporalPrecision) {
		checkNotClosed();
		super.setParameter( parameter, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(@Nonnull Parameter<Date> parameter, @Nullable Date value, @Nonnull TemporalType temporalPrecision) {
		checkNotClosed();
		super.setParameter( parameter, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalPrecision) {
		checkNotClosed();
		super.setParameter( name, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalPrecision) {
		checkNotClosed();
		super.setParameter( name, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalPrecision) {
		super.setParameter( position, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public ProcedureCallImplementor<R> setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalPrecision) {
		checkNotClosed();
		super.setParameter( position, value, temporalPrecision );
		return this;
	}

	@Override
	@Nonnull
	public <X> SelectionQueryImplementor<X> asSelectionQuery(EntityGraph<X> entityGraph, GraphSemantic graphSemantic) {
		throw new IllegalSelectQueryException( "Not a HQL query", getQueryString() );
	}

	@Nonnull
	@Override
	public QueryParameterImplementor<?> getParameter(@Nonnull String name) {
		checkNotClosed();
		return super.getParameter( name );
	}

	@Nonnull
	@Override
	public <T> QueryParameterImplementor<T> getParameter(@Nonnull String name, @Nonnull Class<T> type) {
		checkNotClosed();
		return super.getParameter( name, type );
	}

	@Nonnull
	@Override
	public QueryParameterImplementor<?> getParameter(int position) {
		checkNotClosed();
		return super.getParameter( position );
	}

	@Nonnull
	@Override
	public <T> QueryParameterImplementor<T> getParameter(int position, @Nonnull Class<T> type) {
		checkNotClosed();
		return super.getParameter( position, type );
	}

	@Override
	public boolean isBound(@Nonnull Parameter<?> param) {
		checkNotClosed();
		return super.isBound( param );
	}

	@Nullable
	@Override
	public <T> T getParameterValue(@Nonnull Parameter<T> param) {
		checkNotClosed();
		return super.getParameterValue( param );
	}

	@Nullable
	@Override
	public Object getParameterValue(@Nonnull String name) {
		checkNotClosed();
		return super.getParameterValue( name );
	}

	@Nullable
	@Override
	public Object getParameterValue(int position) {
		checkNotClosed();
		return super.getParameterValue( position );
	}

	@Override
	public int getMaxResults() {
		checkNotClosed();
		return super.getMaxResults();
	}

	@Override
	public int getFirstResult() {
		checkNotClosed();
		return super.getFirstResult();
	}

	@Nonnull
	@Override
	public FlushModeType getFlushMode() {
		checkNotClosed();
		return super.getFlushMode();
	}

	@Nonnull
	@Override
	public QueryFlushMode getQueryFlushMode() {
		checkNotClosed();
		return super.getQueryFlushMode();
	}

	@Nullable
	@Override
	public Integer getTimeout() {
		checkNotClosed();
		return super.getTimeout();
	}

	@Override
	@Nonnull
	public Set<Parameter<?>> getParameters() {
		checkNotClosed();
		return super.getParameters();
	}
}
