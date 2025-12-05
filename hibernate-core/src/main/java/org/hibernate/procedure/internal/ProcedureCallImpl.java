/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

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
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.NamedCallableQueryMemento.ParameterMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;
import org.hibernate.type.OutputableType;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;
import org.hibernate.result.internal.OutputsExecutionContext;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.NoMoreOutputsException;
import org.hibernate.type.BasicType;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.collections.CollectionHelper.makeCopy;
import static org.hibernate.jpa.HibernateHints.HINT_CALLABLE_FUNCTION;
import static org.hibernate.jpa.HibernateHints.HINT_CALLABLE_FUNCTION_RETURN_TYPE;
import static org.hibernate.procedure.internal.NamedCallableQueryMementoImpl.ParameterMementoImpl.fromRegistration;
import static org.hibernate.procedure.internal.Util.resolveResultSetMappingClasses;
import static org.hibernate.procedure.internal.Util.resolveResultSetMappingNames;
import static org.hibernate.procedure.internal.Util.resolveResultSetMappings;
import static org.hibernate.query.results.ResultSetMapping.resolveResultSetMapping;

/**
 * Standard implementation of {@link ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractQuery<R>
		implements ProcedureCallImplementor<R>, ResultContext {

	private final String procedureName;

	private FunctionReturnImpl<R> functionReturn;

	private final ProcedureParameterMetadataImplementor parameterMetadata;
	private final ProcedureParamBindings parameterBindings;

	private final ResultSetMapping resultSetMapping;

	private Set<String> synchronizedQuerySpaces;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private ProcedureOutputsImpl outputs;

	private static String mappingId(String procedureName, Class<?>[] resultClasses) {
		assert resultClasses != null && resultClasses.length > 0;
		return procedureName + ":" + join( ",", resultClasses );
	}

	private static String mappingId(String procedureName, String[] resultSetMappingNames) {
		assert resultSetMappingNames != null && resultSetMappingNames.length > 0;
		return procedureName + ":" + join( ",", resultSetMappingNames );
	}

	private void registerParameters(SharedSessionContractImplementor session, NamedCallableQueryMemento memento) {
		memento.getParameterMementos()
				.forEach( parameterMemento -> registerParameter( parameterMemento.resolve( session ) ) );
	}

	private ProcedureCallImpl(
			SharedSessionContractImplementor session,
			String procedureName,
			String resultSetMappingName,
			boolean resultSetMappingDynamic,
			Set<String> synchronizedQuerySpaces) {
		super( session );
		this.procedureName = procedureName;
		this.synchronizedQuerySpaces = synchronizedQuerySpaces;
		final var factory = session.getSessionFactory();
		resultSetMapping = resolveResultSetMapping( resultSetMappingName, resultSetMappingDynamic, factory );
		parameterMetadata = new ProcedureParameterMetadataImpl();
		parameterBindings = new ProcedureParamBindings( parameterMetadata, factory );
	}

	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(
			SharedSessionContractImplementor session,
			String procedureName) {
		this( session, procedureName, procedureName, true, null );
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
		this( session, procedureName,
				mappingId( procedureName, resultClasses ),
				false,
				new HashSet<>() );
		resolveResultSetMappingClasses(
				resultClasses,
				resultSetMapping,
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
		this( session, procedureName,
				mappingId( procedureName, resultSetMappingNames ),
				false,
				new HashSet<>() );
		resolveResultSetMappingNames(
				resultSetMappingNames,
				resultSetMapping,
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
		this( session, memento.getCallableName(),
				memento.getRegistrationName(),
				false,
				makeCopy( memento.getQuerySpaces() ) );
		resolveResultSetMappings(
				memento.getResultSetMappingNames(),
				memento.getResultSetMappingClasses(),
				resultSetMapping,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
		registerParameters( session, memento );
		applyOptions( memento );
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	ProcedureCallImpl(
			SharedSessionContractImplementor session,
			NamedCallableQueryMemento memento,
			Class<?>... resultTypes) {
		this( session, memento.getCallableName(),
				mappingId( memento.getCallableName(), resultTypes ),
				false,
				makeCopy( memento.getQuerySpaces() ) );
		resolveResultSetMappings(
				null,
				resultTypes,
				resultSetMapping,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
		registerParameters( session, memento );
		applyOptions( memento );
	}

	public ProcedureCallImpl(
			SharedSessionContractImplementor session,
			NamedCallableQueryMementoImpl memento,
			String... resultSetMappingNames) {
		this( session, memento.getCallableName(),
				mappingId( memento.getCallableName(), resultSetMappingNames ),
				false,
				makeCopy( memento.getQuerySpaces() ) );
		resolveResultSetMappings(
				resultSetMappingNames,
				null,
				resultSetMapping,
				synchronizedQuerySpaces::add,
				this::getSessionFactory
		);
		registerParameters( session, memento );
		applyOptions( memento );
	}

	private void applyCallableFunctionHint() {
		final List<Class<?>> resultTypes = new ArrayList<>();
		resultSetMapping.visitResultBuilders(
				(index, resultBuilder) -> resultTypes.add( resultBuilder.getJavaType() )
		);
		if ( resultTypes.size() == 1 ) {
			final var basicType =
					getTypeConfiguration()
							.getBasicTypeForJavaType( resultTypes.get(0) );
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

	@Override
	public String getProcedureName() {
		return procedureName;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ProcedureParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	public ParameterStrategy getParameterStrategy() {
		return getParameterMetadata().getParameterStrategy();
	}

	@Override
	public boolean isFunctionCall() {
		return functionReturn != null;
	}

	@Override
	public FunctionReturnImplementor<R> getFunctionReturn() {
		return functionReturn;
	}

	@Override
	public ProcedureCallImplementor<R> markAsFunctionCall(int sqlType) {
		functionReturn = new FunctionReturnImpl<>( this, sqlType );
		return this;
	}

	private void markAsFunctionCallRefRefCursor() {
		functionReturn = new FunctionReturnImpl<>( this, Types.REF_CURSOR );
	}

	@Override
	public ProcedureCallImpl<R> markAsFunctionCall(Class<?> resultType) {
		final var basicType = getTypeConfiguration().getBasicTypeForJavaType( resultType );
		if ( basicType == null ) {
			throw new IllegalArgumentException( "Could not resolve a BasicType for the Java type: " + resultType.getName() );
		}
		markAsFunctionCall( basicType );
		return this;
	}

	@Override
	public ProcedureCall markAsFunctionCall(Type<?> typeReference) {
		if ( !(typeReference instanceof OutputableType<?> outputableType) ) {
			throw new IllegalArgumentException( "Given type is not an OutputableType: " + typeReference );
		}
		if ( resultSetMapping.getNumberOfResultBuilders() == 0 ) {
			final var expressible = resolveExpressible( typeReference );
			// Function returns might not be represented as callable parameters,
			// but we still want to convert the result to the requested java type if possible
			resultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( expressible.getExpressibleJavaType() ) );
		}
		//noinspection unchecked
		functionReturn = new FunctionReturnImpl<>( this, (OutputableType<R>) outputableType );
		return this;
	}

	private void markAsFunctionCall(BasicType<?> basicType) {
		if ( resultSetMapping.getNumberOfResultBuilders() == 0 ) {
			// Function returns might not be represented as callable parameters,
			// but we still want to convert the result to the requested java type if possible
			resultSetMapping.addResultBuilder( new ScalarDomainResultBuilder<>( basicType.getExpressibleJavaType() ) );
		}
		//noinspection unchecked
		functionReturn = new FunctionReturnImpl<>( this, (OutputableType<R>) basicType );
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter registrations

	@Override
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class<?> type, ParameterMode mode) {
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
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			String parameterName,
			Class<?> type,
			ParameterMode mode) {
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
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			int position,
			Type<?> type,
			ParameterMode mode) {
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
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(
			String parameterName,
			Type<?> type,
			ParameterMode mode) {
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
	public <T> ProcedureParameter<T> registerParameter(int position, Class<T> javaType, ParameterMode mode) {
		final var parameterType = getMappingMetamodel().resolveParameterBindType( javaType );
		final var procedureParameter =
				new ProcedureParameterImpl<>( position, mode, getExpressibleJavaType( parameterType ), parameterType );
		registerParameter( procedureParameter );
		return procedureParameter;
	}

	@Override
	public <T> ProcedureParameter<T> registerParameter(
			int position,
			Type<T> typeReference,
			ParameterMode mode) {
		final var expressible = resolveExpressible( typeReference );
		final var procedureParameter =
				new ProcedureParameterImpl<>( position, mode, typeReference.getJavaType(), expressible );
		registerParameter( procedureParameter );
		return procedureParameter;
	}

	private <T> SqmBindableType<T> resolveExpressible(Type<T> typeReference) {
		return getSessionFactory().getRuntimeMetamodels().resolveExpressible( typeReference );
	}

	private void registerParameter(ProcedureParameterImplementor<?> parameter) {
		parameterMetadata.registerParameter( parameter );
	}

	@Override
	public ProcedureParameterImplementor<?> getParameterRegistration(int position) {
		return parameterMetadata.getQueryParameter( position );
	}

	@Override
	public <T> ProcedureParameterImplementor<T> registerParameter(String name, Class<T> javaType, ParameterMode mode) {
		final var parameterType = getMappingMetamodel().resolveParameterBindType( javaType );
		final var parameter =
				new ProcedureParameterImpl<>( name, mode, getExpressibleJavaType( parameterType ), parameterType );
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

	private NodeBuilder getNodeBuilder() {
		return getSessionFactory().getQueryEngine().getCriteriaBuilder();
	}

	@Override
	public <T> ProcedureParameterImplementor<T> registerParameter(
			String name,
			Type<T> typeReference,
			ParameterMode mode) {
		final var expressible = resolveExpressible( typeReference );
		final var parameter =
				new ProcedureParameterImpl<>( name, mode, typeReference.getJavaType(), expressible );
		registerParameter( parameter );
		return parameter;
	}

	@Override
	public ProcedureParameterImplementor<?> getParameterRegistration(String name) {
		return parameterMetadata.getQueryParameter( name );
	}

	@Override
	public List<ProcedureParameter<?>> getRegisteredParameters() {
		return unmodifiableList( parameterMetadata.getRegistrationsAsList() );
	}

	@Override
	public ProcedureOutputs getOutputs() {
		if ( outputs == null ) {
			outputs = buildOutputs();
		}
		return outputs;
	}

	private ProcedureOutputsImpl buildOutputs() {
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

		return new ProcedureOutputsImpl(
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
	public String getQueryString() {
		return null;
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
	public ProcedureCallImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		synchronizedQuerySpaces().add( querySpace );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedEntityName(String entityName) {
		final var entityDescriptor = getMappingMetamodel().getEntityDescriptor( entityName );
		addSynchronizedQuerySpaces( entityDescriptor );
		return this;
	}

	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedEntityClass(@SuppressWarnings("rawtypes") Class entityClass) {
		final var entityDescriptor = getMappingMetamodel().getEntityDescriptor( entityClass );
		addSynchronizedQuerySpaces( entityDescriptor );
		return this;
	}

	@Override
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
				isCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getQueryOptions().getFlushMode(),
				isReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
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

	@Override
	protected void applyGraph(RootGraphImplementor<?> entityGraph, GraphSemantic graphSemantic) {
		throw new IllegalStateException( "EntityGraph hints are not supported for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	public Query<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic) {
		throw new IllegalStateException( "EntityGraph hints are not supported for ProcedureCall/StoredProcedureQuery" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// outputs

	@Override
	public boolean execute() {
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

	@Override
	protected int doExecuteUpdate() {
		// The expectation is that there is just one Output, of type UpdateCountOutput
		try {
			execute();
			return getUpdateCount();
		}
		finally {
			getOutputs().release();
		}
	}

	@Override
	public Object getOutputParameterValue(int position) {
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
	public Object getOutputParameterValue(String parameterName) {
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
	public boolean hasMoreResults() {
		final var outputs = getOutputs();
		return outputs.goToNext()
			&& outputs.getCurrent() instanceof ResultSetOutput;
	}

	@Override
	public int getUpdateCount() {
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

	@Override
	protected List<R> doList() {
		if ( getMaxResults() == 0 ) {
			return emptyList();
		}
		else {
			try {
				if ( getOutputs().getCurrent() instanceof ResultSetOutput<?> resultSetOutput ) {
					return ((ResultSetOutput<R>) resultSetOutput).getResultList();
				}
				else {
					throw new IllegalStateException(
							"Current CallableStatement was not a ResultSet, but getResultList was called" );
				}
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
	}

	@Override
	public long getResultCount() {
		throw new UnsupportedOperationException( "getResultCount() not implemented for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> page) {
		throw new UnsupportedOperationException("getKeyedResultList() not implemented for ProcedureCall/StoredProcedureQuery");
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "scroll() is not implemented for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	protected ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "scroll() is not implemented for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	public List<R> getResultList() {
		return doList();
	}

	@Override
	public R getSingleResult() {
		final var resultList = getResultList();
		if ( resultList == null || resultList.isEmpty() ) {
			throw new NoResultException( "Call to stored procedure '" + getProcedureName()
										+ "' returned no results" );
		}
		else if ( resultList.size() > 1 ) {
			throw new NonUniqueResultException( "Call to stored procedure '" + getProcedureName()
												+ "' returned multiple results" );
		}
		return resultList.get( 0 );
	}

	@Override
	public R getSingleResultOrNull() {
		final var resultList = getResultList();
		if ( resultList == null || resultList.isEmpty() ) {
			return null;
		}
		else if ( resultList.size() > 1 ) {
			throw new NonUniqueResultException( "Call to stored procedure '" + getProcedureName()
												+ "' returned multiple results" );
		}
		return resultList.get( 0 );
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		if ( type.isInstance( parameterMetadata ) ) {
			return type.cast( parameterMetadata );
		}

		if ( type.isInstance( parameterBindings ) ) {
			return type.cast( parameterBindings );
		}

		if ( type.isInstance( getQueryOptions() ) ) {
			return type.cast( getQueryOptions() );
		}

		if ( type.isInstance( getQueryOptions().getAppliedGraph() ) ) {
			return type.cast( getQueryOptions().getAppliedGraph() );
		}

		if ( type.isInstance( getSession() ) ) {
			return type.cast( getSession() );
		}

		if ( type.isInstance( getOutputs() ) ) {
			return type.cast( getOutputs() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + type.getName() + "]" );
	}

	@Override
	public ProcedureCallImplementor<R> setLockMode(LockModeType lockMode) {
		// the JPA spec requires IllegalStateException here, even
		// though it's logically an UnsupportedOperationException
		throw new IllegalStateException( "Illegal attempt to set lock mode for a procedure call" );
	}

	@Override
	public ProcedureCallImplementor<R> setTimeout(Integer timeout) {
		if ( timeout == null ) {
			timeout = -1;
		}
		super.setTimeout( (int) timeout );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		// the JPA spec requires IllegalStateException here, even
		// though it's logically an UnsupportedOperationException
		throw new IllegalStateException( "Illegal attempt to get lock mode for a procedure call" );
	}

	@Override @Deprecated
	public QueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "setLockOptions does not apply to procedure calls" );
	}

	@Override @SuppressWarnings("resource")
	public ProcedureCallImplementor<R> setHint(String hintName, Object value) {
		switch ( hintName ) {
			case HINT_CALLABLE_FUNCTION:
				if ( value instanceof Boolean bool && bool
					|| value instanceof String string && parseBoolean( string ) ) {
					applyCallableFunctionHint();
				}
				else {
					throw new IllegalArgumentException( "Illegal value for hint '" + hintName + "'" );
				}
				break;
			case HINT_CALLABLE_FUNCTION_RETURN_TYPE:
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
				break;
			default:
				super.setHint( hintName, value );
		}
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	// todo (5.3) : all of the parameter stuff here can be done in AbstractQuery
	//		using #getParameterMetadata and #getQueryParameterBindings for abstraction.
	//		this "win" is to define these in one place

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(
			QueryParameter<P> parameter,
			P value,
			Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

//	@Override
//	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type type) {
//		super.setParameter( parameter, value, type );
//		return this;
//	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(String name, P value, Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

//	@Override
//	public ProcedureCallImplementor<R> setParameter(String name, Object value, Type type) {
//		super.setParameter( name, value, type );
//		return this;
//	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(int position, P value, Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(
			Parameter<Calendar> parameter,
			Calendar value,
			TemporalType temporalPrecision) {
		super.setParameter( parameter, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(Parameter<Date> parameter, Date value, TemporalType temporalPrecision) {
		super.setParameter( parameter, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalPrecision) {
		super.setParameter( name, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalPrecision) {
		super.setParameter( name, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalPrecision) {
		super.setParameter( position, value, temporalPrecision );
		return this;
	}

	@Override @Deprecated
	public ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalPrecision) {
		super.setParameter( position, value, temporalPrecision );
		return this;
	}

	@Override
	public Stream<R> getResultStream() {
		return getResultList().stream();
	}

	@Override
	public Stream<R> stream() {
		return getResultStream();
	}

	public ResultSetMapping getResultSetMapping() {
		return resultSetMapping;
	}

	@Override
	public ProcedureCallImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		return (ProcedureCallImplementor<R>) super.setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public ProcedureCallImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		return (ProcedureCallImplementor<R>) super.setCacheStoreMode( cacheStoreMode );
	}
}
