/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.procedure.NoMoreReturnsException;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.Output;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.ResultSetOutput;
import org.hibernate.procedure.UpdateCountOutput;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterBinderImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterExtractorImpl;
import org.hibernate.sql.exec.internal.JdbcCallParameterRegistrationImpl;
import org.hibernate.sql.exec.internal.JdbcCallRefCursorExtractorImpl;
import org.hibernate.sql.exec.results.process.internal.RowReaderNoResultsExpectedImpl;
import org.hibernate.sql.exec.results.process.internal.RowReaderStandardImpl;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.InitializerSource;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.results.process.spi.RowReader;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.spi.Type;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractQuery<R>
		implements ProcedureCallImplementor<R> {
	private final String procedureName;
	private final Set<String> synchronizedQuerySpaces;

	private final RowReader<R> rowReader;
	private final ParameterManager parameterManager;

	private FunctionReturnImpl functionReturn;

	private ProcedureOutputsImpl outputs;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();


	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(SharedSessionContractImplementor session, String procedureName) {
		super( session );
		this.procedureName = procedureName;

		this.parameterManager = new ParameterManager( this );

		this.synchronizedQuerySpaces = Collections.emptySet();
		this.rowReader = RowReaderNoResultsExpectedImpl.instance();
	}

	/**
	 * The result Class(es) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultClasses The classes making up the result
	 */
	public ProcedureCallImpl(final SharedSessionContractImplementor session, String procedureName, Class... resultClasses) {
		super( session );
		this.procedureName = procedureName;

		this.parameterManager = new ParameterManager( this );

		final Set<String> querySpaces = new HashSet<>();
		final List<ReturnAssembler> returnAssemblers = new ArrayList<>();
		final List<Initializer> initializers = new ArrayList<>();

		Util.resolveResultClasses(
				new Util.ResultClassesResolutionContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getFactory();
					}

					@Override
					public void addQuerySpaces(String... spaces) {
						Collections.addAll( querySpaces, spaces );
					}

					@Override
					public void addQueryReturns(Return... queryReturns) {
						for ( Return queryReturn : queryReturns ) {
							returnAssemblers.add( queryReturn.getReturnAssembler() );
							if ( queryReturn instanceof InitializerSource ) {
								( (InitializerSource) queryReturn ).registerInitializers( initializers::add );
							}
						}
					}
				},
				resultClasses
		);

		this.synchronizedQuerySpaces = Collections.unmodifiableSet( querySpaces );
		this.rowReader = new RowReaderStandardImpl<>(
				returnAssemblers,
				initializers,
				null,
				null
		);
	}

	/**
	 * The result-set-mapping(s) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultSetMappings The names of the result set mappings making up the result
	 */
	public ProcedureCallImpl(final SharedSessionContractImplementor session, String procedureName, String... resultSetMappings) {
		super( session );
		this.procedureName = procedureName;

		this.parameterManager = new ParameterManager( this );

		final Set<String> querySpaces = new HashSet<>();
		final List<ReturnAssembler> returnAssemblers = new ArrayList<>();
		final List<Initializer> initializers = new ArrayList<>();

		Util.resolveResultSetMappings(
				new Util.ResultSetMappingResolutionContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getFactory();
					}

					@Override
					public ResultSetMappingDefinition findResultSetMapping(String name) {
						return session.getFactory().getNamedQueryRepository().getResultSetMappingDefinition( name );
					}

					@Override
					public void addQuerySpaces(String... spaces) {
						Collections.addAll( querySpaces, spaces );
					}

					@Override
					public void addQueryReturns(Return... queryReturns) {
						for ( Return queryReturn : queryReturns ) {
							returnAssemblers.add( queryReturn.getReturnAssembler() );
							if ( queryReturn instanceof InitializerSource ) {
								( (InitializerSource) queryReturn ).registerInitializers( initializers::add );
							}
						}
					}

					// todo : add QuerySpaces to JdbcOperation
					// todo : add JdbcOperation#shouldFlushAffectedQuerySpaces()
				},
				resultSetMappings
		);

		this.synchronizedQuerySpaces = Collections.unmodifiableSet( querySpaces );
		this.rowReader = new RowReaderStandardImpl<>(
				returnAssemblers,
				initializers,
				null,
				null
		);
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	@SuppressWarnings("unchecked")
	ProcedureCallImpl(SharedSessionContractImplementor session, ProcedureCallMementoImpl memento) {
		super( session );
		this.procedureName = memento.getProcedureName();

		this.parameterManager = new ParameterManager( this );
		this.parameterManager.registerParameters( memento );

		this.synchronizedQuerySpaces = Util.copy( memento.getSynchronizedQuerySpaces() );
		this.rowReader = memento.getRowReader();

		for ( Map.Entry<String, Object> entry : memento.getHintsMap().entrySet() ) {
			setHint( entry.getKey(), entry.getValue() );
		}
	}


	public SharedSessionContractImplementor getSession() {
		return (SharedSessionContractImplementor) super.getSession();
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
	public ParameterManager getParameterMetadata() {
		return parameterManager;
	}

	@Override
	protected ParameterManager queryParameterBindings() {
		return parameterManager;
	}

	@Override
	public boolean isFunctionCall() {
		return functionReturn != null;
	}

	@Override
	public ProcedureCall markAsFunctionCall(int sqlType) {
		functionReturn = new FunctionReturnImpl( this, sqlType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistration<T> registerParameter(int position, Class<T> type, ParameterMode mode) {
		return parameterManager.registerParameter( position, mode, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(int position, Class type, ParameterMode mode) {
		parameterManager.registerParameter( position, mode, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ParameterRegistrationImplementor getParameterRegistration(int position) {
		return parameterManager.getQueryParameter( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistration<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
		return parameterManager.registerParameter( name, mode, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(String name, Class type, ParameterMode mode) {
		parameterManager.registerParameter( name, mode, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ParameterRegistrationImplementor getParameterRegistration(String name) {
		return parameterManager.getQueryParameter( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ParameterRegistration> getRegisteredParameters() {
		return parameterManager.collectParameterRegistrations();
	}

	@Override
	public ProcedureOutputs getOutputs() {
		if ( outputs == null ) {
			outputs = buildOutputs();
		}

		return outputs;
	}

	private ProcedureOutputsImpl buildOutputs() {
		parameterManager.validate();

		if ( isFunctionCall() ) {
			if ( !parameterManager.hasAnyParameterRegistrations() ) {
				// should we simply warn and switch this back to a proc-call?
				throw new IllegalStateException(
						"A function call was requested, but no parameter registrations were made; " +
								"at least need to register the function result param"
				);
			}
		}

		// todo : a lot of this should be delegated to something like SqlTreeExecutor
		// 		^^ and there should be a generalized contracts pulled out of SqlTreeExecutor concept
		// 		for individually:
		//			1) Preparing the PreparedStatement for execution (PreparedStatementPreparer) - including:
		//				* getting the PreparedStatement/CallableStatement from the Connection
		//				* registering CallableStatement parameters (JdbcCallParameterRegistrations)
		//				* binding any IN/INOUT parameter values into the PreparedStatement
		//			2) Executing the PreparedStatement and giving access to the outputs - PreparedStatementExecutor
		//				- ? PreparedStatementResult as an abstraction at the Jdbc level?
		//			3) For ResultSet outputs, extracting the "jdbc values" - for integration into
		// 				the org.hibernate.sql.exec.results.process stuff.  This allows, for example, easily
		// 				applying query result caching over a ProcedureCall for its ResultSet outputs (if we
		//				decide that is a worthwhile feature.

		// the current approach to this in the SQM stuff is that SqlTreeExecutor is passed delegates
		// that handle the 3 phases mentioned above (create statement, execute, extract results) - it
		// acts as the "coordinator"
		//
		// but another approach would be to have different "SqlTreeExecutor" impls, i.e.. JdbcCallExecutor, JdbcSelectExecutor, etc
		// this approach has a lot of benefits

		final CallableStatementSupport callableStatementSupport = getSession().getFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getDialect()
				.getCallableStatementSupport();

		final JdbcCallImpl jdbcCall = new JdbcCallImpl( getProcedureName(), parameterManager.getParameterStrategy() );

		// positional parameters are 0-based..
		//		JDBC positions (1-based) come into play later, although we could calculate them here as well
		int parameterPosition = 0;

		final FunctionReturnImpl functionReturnToUse;
		if ( this.functionReturn != null ) {
			// user explicitly defined the function return via native API - good for them :)
			functionReturnToUse = this.functionReturn;
		}
		else {
			final boolean useFunctionCallSyntax = isFunctionCall()
					|| callableStatementSupport.shouldUseFunctionSyntax( parameterManager );

			if ( useFunctionCallSyntax ) {
				// todo : how to determine the JDBC type code here?
				functionReturnToUse = new FunctionReturnImpl( this, Types.VARCHAR );
				parameterPosition++;
			}
			else {
				functionReturnToUse = null;
			}
		}

		if ( functionReturnToUse != null ) {
			jdbcCall.addParameterRegistration( functionReturnToUse.toJdbcCallParameterRegistration( getSession() ) );

			// function returns use a parameter
			parameterPosition++;
		}

		for ( ParameterRegistrationImplementor registration : parameterManager.getParameterRegistrations() ) {
			final JdbcCallParameterRegistrationImpl jdbcRegistration;

			if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
				jdbcRegistration = new JdbcCallParameterRegistrationImpl(
						registration.getName(),
						parameterPosition,
						ParameterMode.REF_CURSOR,
						Types.REF_CURSOR,
						null,
						null,
						null,
						new JdbcCallRefCursorExtractorImpl( registration.getName(), parameterPosition )
				);
			}
			else {
				final Type ormType = determineTypeToUse( registration );
				if ( ormType == null ) {
					throw new ParameterMisuseException( "Could not determine Hibernate Type for parameter : " + registration );
				}

				JdbcParameterBinder parameterBinder = null;
				JdbcCallParameterExtractorImpl parameterExtractor = null;

				if ( registration.getMode() == ParameterMode.IN || registration.getMode() == ParameterMode.INOUT ) {
					parameterBinder = new JdbcCallParameterBinderImpl(
							procedureName,
							registration.getName(),
							registration.getPosition(),
							ormType
					);
				}
				if ( registration.getMode() == ParameterMode.OUT || registration.getMode() == ParameterMode.INOUT ) {
					parameterExtractor = new JdbcCallParameterExtractorImpl(
							procedureName,
							registration.getName(),
							registration.getPosition(),
							ormType
					);
				}

				jdbcRegistration = new JdbcCallParameterRegistrationImpl(
						registration.getName(),
						parameterPosition,
						ParameterMode.REF_CURSOR,
						Types.REF_CURSOR,
						ormType,
						parameterBinder,
						parameterExtractor,
						null
				);
			}

			jdbcCall.addParameterRegistration( jdbcRegistration );

			parameterPosition++;
		}

		return new ProcedureOutputsImpl(
				this,
				jdbcCall,
				resolveParameterStrategy( parameterManager.getParameterStrategy() ),
				queryOptions,
				parameterManager,
				// todo : build RowTransformer based on ResultSet mapping
				null,
				getSession()
		);
	}

	private ParameterStrategy resolveParameterStrategy(ParameterStrategy parameterStrategy) {
		if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
			return ParameterStrategy.POSITIONAL;
		}

		if ( parameterStrategy == ParameterStrategy.NAMED ) {
			final boolean supportsNamedParameters = getSession().getJdbcServices()
					.getJdbcEnvironment()
					.getExtractedDatabaseMetaData()
					.supportsNamedParameters();
			if ( !supportsNamedParameters ) {
				return ParameterStrategy.POSITIONAL;
			}
		}

		return parameterStrategy;
	}

	private Type determineTypeToUse(ParameterRegistrationImplementor parameterRegistration) {
		if ( parameterRegistration.getBind() != null ) {
			if ( parameterRegistration.getBind().getBindType() != null ) {
				return parameterRegistration.getBind().getBindType();
			}
		}

		if ( parameterRegistration.getHibernateType() != null ) {
			return parameterRegistration.getHibernateType();
		}

		return null;
	}

	@Override
	public String getQueryString() {
		return getProcedureName() + "(...)";
	}

	/**
	 * Use this form instead of {@link #getSynchronizedQuerySpaces()} when you want to make sure the
	 * underlying Set is instantiated (aka, on add)
	 *
	 * @return The spaces
	 */
	protected Set<String> synchronizedQuerySpaces() {
		return synchronizedQuerySpaces == null ? Collections.emptySet() : synchronizedQuerySpaces;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getSynchronizedQuerySpaces() {
		if ( synchronizedQuerySpaces == null ) {
			return Collections.emptySet();
		}
		else {
			return Collections.unmodifiableSet( synchronizedQuerySpaces );
		}
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		synchronizedQuerySpaces().add( querySpace );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedEntityName(String entityName) {
		addSynchronizedQuerySpaces( getSession().getFactory().getMetamodel().entityPersister( entityName ) );
		return this;
	}

	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( Arrays.asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedEntityClass(Class entityClass) {
		addSynchronizedQuerySpaces( getSession().getFactory().getMetamodel().entityPersister( entityClass.getName() ) );
		return this;
	}

	@Override
	public ProcedureCallMemento extractMemento(Map<String, Object> hints) {
		return new ProcedureCallMementoImpl(
				procedureName,
				parameterManager.getParameterStrategy(),
				parameterManager.toParameterMementos(),
				rowReader,
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( hints )
		);
	}

	@Override
	public ProcedureCallMemento extractMemento() {
		return new ProcedureCallMementoImpl(
				procedureName,
				parameterManager.getParameterStrategy(),
				parameterManager.toParameterMementos(),
				rowReader,
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( getHints() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA StoredProcedureQuery impl

	private ProcedureOutputs procedureResult;

	@Override
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		getSession().checkOpen( true );

		try {
			registerParameter( position, type, mode );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		getSession().checkOpen( true );
		try {
			registerParameter( parameterName, type, mode );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}

		return this;
	}


	// outputs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean execute() {
		try {
			final Output rtn = outputs().getCurrent();
			return rtn != null && ResultSetOutput.class.isInstance( rtn );
		}
		catch (NoMoreReturnsException e) {
			return false;
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	protected ProcedureOutputs outputs() {
		if ( procedureResult == null ) {
			procedureResult = getOutputs();
		}
		return procedureResult;
	}

	@Override
	protected int doExecuteUpdate() {
		if ( ! getSession().isTransactionInProgress() ) {
			throw new TransactionRequiredException( "javax.persistence.Query.executeUpdate requires active transaction" );
		}

		// the expectation is that there is just one Output, of type UpdateCountOutput
		try {
			execute();
			return getUpdateCount();
		}
		finally {
			outputs().release();
		}
	}

	@Override
	public Object getOutputParameterValue(int position) {
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( position );
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
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( parameterName );
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
		return outputs().goToNext() && ResultSetOutput.class.isInstance( outputs().getCurrent() );
	}

	@Override
	public int getUpdateCount() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( rtn == null ) {
				return -1;
			}
			else if ( UpdateCountOutput.class.isInstance( rtn ) ) {
				return ( (UpdateCountOutput) rtn ).getUpdateCount();
			}
			else {
				return -1;
			}
		}
		catch (NoMoreReturnsException e) {
			return -1;
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( ! ResultSetOutput.class.isInstance( rtn ) ) {
				throw new IllegalStateException( "Current CallableStatement ou was not a ResultSet, but getResultList was called" );
			}

			return ( (ResultSetOutput) rtn ).getResultList();
		}
		catch (NoMoreReturnsException e) {
			// todo : the spec is completely silent on these type of edge-case scenarios.
			// Essentially here we'd have a case where there are no more results (ResultSets nor updateCount) but
			// getResultList was called.
			return null;
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getSession().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	protected Iterator<R> doIterate() {
		throw new UnsupportedOperationException( "Query#iterate is not valid for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "Query#scroll is not valid for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterManager ) ) {
			return (T) parameterManager;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}

		if ( cls.isInstance( getSession() ) ) {
			return (T) getSession();
		}

		if ( ProcedureOutputs.class.isAssignableFrom( cls ) ) {
			return (T) getOutputs();
		}

		throw new PersistenceException( "Unrecognized unwrap type : " + cls.getName() );
	}

	@Override
	@SuppressWarnings("deprecation")
	public ProcedureCallImplementor<R> setHint(String hintName, Object value) {
		if ( FUNCTION_RETURN_TYPE_HINT.equals( hintName ) ) {
			markAsFunctionCall( ConfigurationHelper.getInteger( value ) );
		}
		else if ( IS_FUNCTION_HINT.equals( hintName ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedQueryHint( IS_FUNCTION_HINT, FUNCTION_RETURN_TYPE_HINT );
			// make a guess as to the type - for now VARCHAR
			// todo : how best to determine JDBC Types code?
			markAsFunctionCall( Types.VARCHAR );
		}

		super.setHint( hintName, value );

		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, EntityGraphImpl entityGraph) {
		throw new IllegalStateException( "EntityGraph hints are not supported for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	protected boolean canApplyAliasSpecificLockModes() {
		return false;
	}

	@Override
	protected void verifySettingLockMode() {
		throw new IllegalStateException( "Illegal attempt to set lock mode on a ProcedureCall / StoredProcedureQuery" );
	}

	@Override
	protected void verifySettingAliasSpecificLockModes() {
		throw new IllegalStateException( "Illegal attempt to set lock mode on a ProcedureCall / StoredProcedureQuery" );
	}

	@Override
	public ProcedureCallImplementor<R> setLockMode(LockModeType lockMode) {
		throw new IllegalStateException( "javax.persistence.Query.setLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new IllegalStateException( "javax.persistence.Query.getLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public ProcedureCallImplementor<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		parameterManager.resolve( parameter ).bindValue( value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(Parameter<P> parameter, P value) {
		parameterManager.resolve( parameter ).bindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value) {
		parameterManager.getQueryParameter( name ).bindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value) {
		parameterManager.getQueryParameter( position ).bindValue( value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type type) {
		final ParameterRegistrationImplementor<P> reg = parameterManager.resolve( parameter );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(String name, Object value, Type type) {
		final ParameterRegistrationImplementor reg = parameterManager.getQueryParameter( name );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(int position, Object value, Type type) {
		final ParameterRegistrationImplementor reg = parameterManager.getQueryParameter( position );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		parameterManager.resolve( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value, TemporalType temporalType) {
		parameterManager.getQueryParameter( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value, TemporalType temporalType) {
		parameterManager.getQueryParameter( position ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(QueryParameter parameter, Collection values) {
		throw new ParameterMisuseException( "ProcedureCall/StoredProcedureQuery parameters cannot be multi-valued" );
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Collection values) {
		throw new ParameterMisuseException( "ProcedureCall/StoredProcedureQuery parameters cannot be multi-valued" );
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Collection values, Type type) {
		throw new ParameterMisuseException( "ProcedureCall/StoredProcedureQuery parameters cannot be multi-valued" );
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Object[] values, Type type) {
		throw new ParameterMisuseException( "ProcedureCall/StoredProcedureQuery parameters cannot be multi-valued" );
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Object[] values) {
		throw new ParameterMisuseException( "ProcedureCall/StoredProcedureQuery parameters cannot be multi-valued" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Calendar value, TemporalType temporalType) {
		parameterManager.resolve( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Date value, TemporalType temporalType) {
		parameterManager.resolve( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		parameterManager.getQueryParameter( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		parameterManager.getQueryParameter( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		parameterManager.getQueryParameter( position ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		parameterManager.getQueryParameter( position ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		if ( !parameterManager.hasAnyParameterRegistrations() ) {
			return Collections.emptySet();
		}

		return parameterManager.collectParameterRegistrations().stream().collect( Collectors.toSet() );
	}
}
