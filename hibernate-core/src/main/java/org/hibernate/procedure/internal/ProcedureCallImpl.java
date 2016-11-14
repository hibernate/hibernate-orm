/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterBindImplementor;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.old.AbstractProducedQuery;
import org.hibernate.query.procedure.internal.ProcedureParameterImpl;
import org.hibernate.query.procedure.internal.ProcedureParameterMetadata;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.Outputs;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.type.spi.Type;
import org.hibernate.sql.sqm.exec.internal.QueryOptionsImpl;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.ProcedureParameterNamedBinder;

import org.jboss.logging.Logger;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractQuery<R>
		implements ProcedureCallImplementor<R>, ResultContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ProcedureCallImpl.class.getName()
	);

	private static final NativeSQLQueryReturn[] NO_RETURNS = new NativeSQLQueryReturn[0];

	private final String procedureName;
	private boolean functionCall;

	private final ParameterManager parameterManager;
	private Set<String> synchronizedQuerySpaces;

	private ProcedureOutputsImpl outputs;
	private final NativeSQLQueryReturn[] queryReturns;

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

		this.queryReturns = NO_RETURNS;
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

		final List<NativeSQLQueryReturn> collectedQueryReturns = new ArrayList<>();
		final Set<String> collectedQuerySpaces = new HashSet<>();

		Util.resolveResultClasses(
				new Util.ResultClassesResolutionContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getFactory();
					}

					@Override
					public void addQueryReturns(NativeSQLQueryReturn... queryReturns) {
						Collections.addAll( collectedQueryReturns, queryReturns );
					}

					@Override
					public void addQuerySpaces(String... spaces) {
						Collections.addAll( collectedQuerySpaces, spaces );
					}
				},
				resultClasses
		);

		this.queryReturns = collectedQueryReturns.toArray( new NativeSQLQueryReturn[ collectedQueryReturns.size() ] );
		this.synchronizedQuerySpaces = collectedQuerySpaces;
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

		final List<NativeSQLQueryReturn> collectedQueryReturns = new ArrayList<>();
		final Set<String> collectedQuerySpaces = new HashSet<>();

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
					public void addQueryReturns(NativeSQLQueryReturn... queryReturns) {
						Collections.addAll( collectedQueryReturns, queryReturns );
					}

					@Override
					public void addQuerySpaces(String... spaces) {
						Collections.addAll( collectedQuerySpaces, spaces );
					}
				},
				resultSetMappings
		);

		this.queryReturns = collectedQueryReturns.toArray( new NativeSQLQueryReturn[ collectedQueryReturns.size() ] );
		this.synchronizedQuerySpaces = collectedQuerySpaces;
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
		parameterManager.registerParameters( memento );

		this.queryReturns = memento.getQueryReturns();
		this.synchronizedQuerySpaces = Util.copy( memento.getSynchronizedQuerySpaces() );

		for ( Map.Entry<String, Object> entry : memento.getHintsMap().entrySet() ) {
			setHint( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public ProcedureParameterMetadata getParameterMetadata() {
		return (ProcedureParameterMetadata) super.getParameterMetadata();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return getProducer();
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
		return functionCall;
	}

	@Override
	public ProcedureCall markAsFunctionCall() {
		functionCall = true;
		return this;
	}

	@Override
	public String getSql() {
		return getProcedureName();
	}

	@Override
	public NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns;
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

		final CallableStatementSupport callableStatementSupport = getProducer().getJdbcServices()
				.getJdbcEnvironment()
				.getDialect()
				.getCallableStatementSupport();

		final boolean useFunctionCallSyntax = isFunctionCall()
				|| callableStatementSupport.shouldUseFunctionSyntax( parameterManager );

		final JdbcCall jdbcCall = new JdbcCall( this, useFunctionCallSyntax );

		// determine whether to use named or positional JDBC parameter access;
		// this is just the initial determination - as we visit each
		// parameter registration we will adjust this if needed.
		//
		// acts as an "opt out" toggle.  Meaning we assume true and apply
		// a series of checks that can only set this to false.
		boolean shouldUseJdbcNamedParameters = canUseJdbcNamedParameters();

		final List<JdbcParameterPreparer> jdbcParameterPreparers = new ArrayList<>();
		final List<JdbcParameterBinder> jdbcParameterBinders = new ArrayList<>();

		final Map<ParameterRegistration,JdbcParameterExtractor> jdbcParameterExtractorMap = new HashMap<>();
		final List<JdbcRefCursorExtractor> jdbcRefCursorExtractors = new ArrayList<>();

		// JDBC positional parameters are 0-based..
		int jdbcParameterPosition = 0;

		for ( ParameterRegistrationImplementor registration : parameterManager.getParameterRegistrations() ) {
			final Type hibernateType = registration.getHibernateType();

			if ( hibernateType == null ) {
				throw new ParameterMisuseException( "Could not determine Hibernate Type for parameter : " + registration );
			}

			final int currentColumnSpan = hibernateType.getColumnSpan();

			if ( currentColumnSpan > 1 ) {
				shouldUseJdbcNamedParameters = false;
			}

			jdbcCall.registerParameters( hibernateType );
			jdbcParameterPreparers.add( new JdbcParameterPreparer( registration, hibernateType, jdbcParameterPosition ) );

			if ( isInputParameter( registration ) ) {
				jdbcParameterBinders.add( new JdbcParameterBinder( registration, hibernateType, jdbcParameterPosition ) );
			}

			if ( isOutputParameter( registration ) ) {
				jdbcParameterExtractorMap.put(
						registration,
						new JdbcParameterExtractor( registration, hibernateType, jdbcParameterPosition )
				);
			}

			if ( isRefCursorParameter( registration ) ) {
				jdbcRefCursorExtractors.add( new JdbcRefCursorExtractor( registration, hibernateType, jdbcParameterPosition ) );
			}

			jdbcParameterPosition += currentColumnSpan;
		}

		final String jdbcCallString = jdbcCall.toCallString();

		try {
			LOG.debugf( "Preparing procedure/function call : %s", jdbcCallString );
			final CallableStatement callableStatement = (CallableStatement) getSession()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( jdbcCallString, true );

			for ( JdbcParameterPreparer jdbcParameterPreparer : jdbcParameterPreparers ) {
				jdbcParameterPreparer.prepare( callableStatement, shouldUseJdbcNamedParameters, getProducer() );
			}

			for ( JdbcParameterBinder jdbcParameterBinder : jdbcParameterBinders ) {
				jdbcParameterBinder.bindInputValue( callableStatement, shouldUseJdbcNamedParameters, getProducer() );
			}

			return new ProcedureOutputsImpl( this, callableStatement, shouldUseJdbcNamedParameters, jdbcParameterExtractorMap, jdbcRefCursorExtractors );
		}
		catch (SQLException e) {
			throw getSession().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error preparing CallableStatement",
					getProcedureName()
			);
		}
	}

	private boolean isInputParameter(ParameterRegistrationImplementor registration) {
		return registration.getMode() == ParameterMode.IN || registration.getMode() == ParameterMode.INOUT;
	}

	private boolean isOutputParameter(ParameterRegistrationImplementor registration) {
		return registration.getMode() == ParameterMode.OUT || registration.getMode() == ParameterMode.INOUT;
	}

	private boolean isRefCursorParameter(ParameterRegistrationImplementor registration) {
		return registration.getMode() == ParameterMode.REF_CURSOR;
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean canUseJdbcNamedParameters() {
		if ( parameterManager.getParameterStrategy() != ParameterStrategy.NAMED ) {
			return false;
		}

		if ( !session().getFactory().getSessionFactoryOptions().isUseOfJdbcNamedParametersEnabled() ) {
			return false;
		}

		final ExtractedDatabaseMetaData databaseMetaData = session().getJdbcCoordinator()
				.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry().getService( JdbcEnvironment.class )
				.getExtractedDatabaseMetaData();
		if ( !databaseMetaData.supportsNamedParameters() ) {
			return false;
		}

		return true;
	}

	private boolean shouldUseJdbcNamedParameters() {
		// atm we execute this for each registration.
		// todo : this should really get passed into each registration - like when obtaining the Binder/Extractor

		if ( parameterManager.getParameterStrategy() != ParameterStrategy.NAMED ) {
			return false;
		}

		if ( !session().getFactory().getSessionFactoryOptions().isUseOfJdbcNamedParametersEnabled() ) {
			return false;
		}

		final ExtractedDatabaseMetaData databaseMetaData = session().getJdbcCoordinator()
				.getJdbcSessionOwner()
				.getJdbcSessionContext()
				.getServiceRegistry().getService( JdbcEnvironment.class )
				.getExtractedDatabaseMetaData();
		if ( !databaseMetaData.supportsNamedParameters() ) {
			return false;
		}

		// todo : I think this could be kept track of as we register the parameters...
		for ( ParameterRegistrationImplementor parameterRegistration : parameterManager.collectParameterRegistrationImplementors() ) {
			final Type typeToUse = determineTypeToUse( parameterRegistration );
			if ( typeToUse == null ) {
				return false;
			}

			if ( typeToUse.getColumnSpan() > 1 ) {
				return false;
			}

			if ( !ProcedureParameterNamedBinder.class.isInstance( typeToUse )
					|| !( (ProcedureParameterNamedBinder) typeToUse ).canDoSetting() ) {
				return false;
			}
		}

		return true;
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

	private ProcedureCallImpl procedureCall() {
		return parameterManager.getProcedureCall();
	}

	private SharedSessionContractImplementor session() {
		return procedureCall().getSession();
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
		if ( synchronizedQuerySpaces == null ) {
			synchronizedQuerySpaces = new HashSet<>();
		}
		return synchronizedQuerySpaces;
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
	public QueryParameters getQueryParameters() {
		// todo : remove this
		//		- kept here for now as reminder to make sure I integrate auto-discover and callable

		final QueryParameters qp = super.getQueryParameters();
		// both of these are for documentation purposes, they are actually handled directly...
		qp.setAutoDiscoverScalarTypes( true );
		qp.setCallable( true );
		return qp;
	}

	@Override
	public ProcedureCallMemento extractMemento(Map<String, Object> hints) {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				parameterManager.getParameterStrategy(),
				parameterManager.toParameterMementos(),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( hints )
		);
	}

	@Override
	public ProcedureCallMemento extractMemento() {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				parameterManager.getParameterStrategy(),
				parameterManager.toParameterMementos(),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( getHints() )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA StoredProcedureQuery impl

	private ProcedureOutputs procedureResult;

	@Override
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );

		try {
			registerParameter( position, type, mode );
		}
		catch (HibernateException he) {
			throw getProducer().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );
		try {
			registerParameter( parameterName, type, mode );
		}
		catch (HibernateException he) {
			throw getProducer().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
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
			throw getProducer().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
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
		if ( ! getProducer().isTransactionInProgress() ) {
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
			throw getProducer().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
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
			throw getProducer().getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
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

		if ( cls.isInstance( queryReturns ) ) {
			return (T) queryReturns;
		}

		if ( cls.isInstance( getProducer() ) ) {
			return (T) getProducer();
		}

		if ( Outputs.class.isAssignableFrom( cls ) ) {
			return (T) getOutputs();
		}

		throw new PersistenceException( "Unrecognized unwrap type : " + cls.getName() );
	}

	@Override
	public ProcedureCallImplementor<R> setHint(String hintName, Object value) {
		if ( IS_FUNCTION_HINT.equals( hintName ) ) {
			// functionCall is a 1-way toggle
			if ( !functionCall && ConfigurationHelper.getBoolean( value ) ) {
				this.functionCall = true;
			}
		}
		else {
			super.setHint( hintName, value );
		}

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
		return parameterManager.collectAllParametersJpa();
	}

	private static class JdbcParameterPreparer {
		private final ParameterRegistrationImplementor registration;
		private final Type hibernateType;
		private final int startingJdbcParameterPosition;

		public JdbcParameterPreparer(
				ParameterRegistrationImplementor registration,
				Type hibernateType, int startingJdbcParameterPosition) {
			this.registration = registration;
			this.hibernateType = hibernateType;
			this.startingJdbcParameterPosition = startingJdbcParameterPosition;
		}

		public void prepare(
				CallableStatement callableStatement,
				boolean shouldUseJdbcNamedParameters,
				SharedSessionContractImplementor session) throws SQLException {
			switch ( registration.getMode() ) {
				case REF_CURSOR: {
					registerRefCursorParameter( callableStatement, shouldUseJdbcNamedParameters, session );
					break;
				}
				case IN: {
					// nothing to prepare
					break;
				}
				default: {
					// OUT and INOUT
					registerOutputParameter( callableStatement, shouldUseJdbcNamedParameters, session );
					break;
				}
			}
		}

		private void registerRefCursorParameter(
				CallableStatement callableStatement,
				boolean shouldUseJdbcNamedParameters,
				SharedSessionContractImplementor session) {
			if ( shouldUseJdbcNamedParameters && registration.getName() != null ) {
				session.getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( callableStatement, registration.getName() );
			}
			else {
				session.getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.registerRefCursorParameter( callableStatement, startingJdbcParameterPosition );
			}

		}

		private void registerOutputParameter(
				CallableStatement callableStatement,
				boolean shouldUseJdbcNamedParameters,
				SharedSessionContractImplementor session) {
			if ( hibernateType.getColumnSpan() > 1 ) {
				assert !shouldUseJdbcNamedParameters;

				// there is more than one column involved; see if the Hibernate Type can handle
				// multi-param extraction...
				final boolean canHandleMultiParamExtraction = ProcedureParameterExtractionAware.class.isInstance( hibernateType )
						&& ( (ProcedureParameterExtractionAware) hibernateType ).canDoExtraction();
				if ( ! canHandleMultiParamExtraction ) {
					// it cannot...
					throw new UnsupportedOperationException(
							"Type [" + hibernateType + "] does support multi-parameter value extraction"
					);
				}

				// todo : this needs CompositeType etal to be finished

				for ( int i = 0; i < hibernateType.getColumnSpan(); i++ ) {
					callableStatement.registerOutParameter( startingJdbcParameterPosition + i, typeCodes[i] );
				}
			}
			else {
				// again, needs Type to get finished (access to JDBC type code
				if ( shouldUseJdbcNamedParameters && registration.getName() != null ) {
					callableStatement.registerOutParameter( registration.getName(), typeCode );
				}
				else {
					callableStatement.registerOutParameter( startingJdbcParameterPosition, typeCode );
				}
			}
		}
	}

	private static boolean isNamed(ParameterRegistrationImplementor registration) {
		return registration.getName() != null;
	}

	private static class JdbcParameterBinder {
		private final ParameterRegistrationImplementor registration;
		private final Type hibernateType;
		private final int startingJdbcParameterPosition;

		public JdbcParameterBinder(
				ParameterRegistrationImplementor registration,
				Type hibernateType,
				int startingJdbcParameterPosition) {
			this.registration = registration;
			this.hibernateType = hibernateType;
			this.startingJdbcParameterPosition = startingJdbcParameterPosition;
		}

		public void bindInputValue(
				CallableStatement callableStatement,
				boolean shouldUseJdbcNamedParameters,
				SharedSessionContractImplementor session) throws SQLException {
			final ParameterBindImplementor binding = registration.getBind();

			if ( binding == null || binding.getValue() == null ) {
				// the user did not bind a value to the parameter being processed, or they bound null.
				// This is the condition defined by `passNulls` and that value controls what happens
				// here.  If `passNulls` is {@code true} we will bind the NULL value into the statement;
				// if `passNulls` is {@code false} we will not.
				//
				// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
				// parameter defines a default value.  Deferring to that information would be the best option
				if ( registration.isPassNullsEnabled() ) {
					LOG.debugf(
							"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to true; binding NULL",
							registration.getProcedureCall().getProcedureName(),
							this
					);

					if ( shouldUseJdbcNamedParameters && registration.getName() != null ) {
						( (ProcedureParameterNamedBinder) hibernateType ).nullSafeSet(
								callableStatement,
								null,
								registration.getName(),
								session
						);
					}
					else {
						hibernateType.nullSafeSet( callableStatement, null, startingJdbcParameterPosition, session );
					}
				}
				else {
					LOG.debugf(
							"Stored procedure [%s] IN/INOUT parameter [%s] not bound and `passNulls` was set to false; assuming procedure defines default value",
							registration.getProcedureCall().getProcedureName(),
							this
					);
				}
			}
			else {
				if ( shouldUseJdbcNamedParameters && registration.getName() != null ) {
					( (ProcedureParameterNamedBinder) hibernateType ).nullSafeSet(
							callableStatement,
							binding.getValue(),
							registration.getName(),
							session
					);
				}
				else {
					hibernateType.nullSafeSet( callableStatement, binding.getValue(),
											   startingJdbcParameterPosition, session );
				}
			}
		}
	}

}
