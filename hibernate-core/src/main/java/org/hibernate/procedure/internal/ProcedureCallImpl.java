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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.procedure.internal.ProcedureParamBindings;
import org.hibernate.query.procedure.internal.ProcedureParameterImpl;
import org.hibernate.query.procedure.internal.ProcedureParameterMetadata;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractProducedQuery<R>
		implements ProcedureCallImplementor<R>, ResultContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ProcedureCallImpl.class.getName()
	);

	private static final NativeSQLQueryReturn[] NO_RETURNS = new NativeSQLQueryReturn[0];

	private final String procedureName;
	private final NativeSQLQueryReturn[] queryReturns;

	private final boolean globalParameterPassNullsSetting;

	private final ProcedureParameterMetadata parameterMetadata;
	private final ProcedureParamBindings paramBindings;

	private Set<String> synchronizedQuerySpaces;

	private ProcedureOutputsImpl outputs;

	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(SharedSessionContractImplementor session, String procedureName) {
		super( session, null );
		this.procedureName = procedureName;
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

		this.queryReturns = NO_RETURNS;

		this.parameterMetadata = new ProcedureParameterMetadata( this );
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, this );
	}

	/**
	 * The result Class(es) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultClasses The classes making up the result
	 */
	public ProcedureCallImpl(final SharedSessionContractImplementor session, String procedureName, Class... resultClasses) {
		super( session, null );
		this.procedureName = procedureName;
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

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

		this.parameterMetadata = new ProcedureParameterMetadata( this );
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, this );
	}

	/**
	 * The result-set-mapping(s) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultSetMappings The names of the result set mappings making up the result
	 */
	public ProcedureCallImpl(final SharedSessionContractImplementor session, String procedureName, String... resultSetMappings) {
		super( session, null );
		this.procedureName = procedureName;
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

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

		this.parameterMetadata = new ProcedureParameterMetadata( this );
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, this );
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	@SuppressWarnings("unchecked")
	ProcedureCallImpl(SharedSessionContractImplementor session, ProcedureCallMementoImpl memento) {
		super( session, null );
		this.procedureName = memento.getProcedureName();
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

		this.queryReturns = memento.getQueryReturns();
		this.synchronizedQuerySpaces = Util.copy( memento.getSynchronizedQuerySpaces() );

		this.parameterMetadata = new ProcedureParameterMetadata( this );
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, this );

		for ( ProcedureCallMementoImpl.ParameterMemento storedRegistration : memento.getParameterDeclarations() ) {
			final ProcedureParameterImplementor<?> registration;

			if ( StringHelper.isNotEmpty( storedRegistration.getName() ) ) {
				registration = new ProcedureParameterImpl(
						this,
						storedRegistration.getName(),
						storedRegistration.getMode(),
						storedRegistration.getType(),
						storedRegistration.getHibernateType(),
						storedRegistration.isPassNullsEnabled()
				);
			}
			else {
				registration = new ProcedureParameterImpl(
						this,
						storedRegistration.getPosition(),
						storedRegistration.getMode(),
						storedRegistration.getType(),
						storedRegistration.getHibernateType(),
						storedRegistration.isPassNullsEnabled()
				);
			}

			getParameterMetadata().registerParameter( registration );
		}

		for ( Map.Entry<String, Object> entry : memento.getHintsMap().entrySet() ) {
			setHint( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public ProcedureParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return paramBindings;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return getProducer();
	}

	public ParameterStrategy getParameterStrategy() {
		return getParameterMetadata().getParameterStrategy();
	}

	@Override
	public String getProcedureName() {
		return procedureName;
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
		final ProcedureParameterImpl procedureParameter = new ProcedureParameterImpl(
				this,
				position,
				mode,
				type,
				getSession().getFactory().getTypeResolver().heuristicType( type.getName() ),
				globalParameterPassNullsSetting
		);

		registerParameter( procedureParameter );
		return procedureParameter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(int position, Class type, ParameterMode mode) {
		registerParameter( position, type, mode );
		return this;
	}

	private void registerParameter(ProcedureParameterImplementor parameter) {
		getParameterMetadata().registerParameter( parameter );
	}

	@Override
	public ParameterRegistrationImplementor getParameterRegistration(int position) {
		return getParameterMetadata().getQueryParameter( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistration<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
		final ProcedureParameterImpl parameter = new ProcedureParameterImpl(
				this,
				name,
				mode,
				type,
				getSession().getFactory().getTypeResolver().heuristicType( type.getName() ),
				globalParameterPassNullsSetting
		);

		registerParameter( parameter );

		return parameter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(String name, Class type, ParameterMode mode) {
		registerParameter( name, type, mode );
		return this;
	}

	@Override
	public ParameterRegistrationImplementor getParameterRegistration(String name) {
		return getParameterMetadata().getQueryParameter( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getRegisteredParameters() {
		return new ArrayList( getParameterMetadata().collectAllParameters() );
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

		// for now assume there are no resultClasses nor mappings defined..
		// 	TOTAL PROOF-OF-CONCEPT!!!!!!

		// todo : how to identify calls which should be in the form `{? = call procName...}` ??? (note leading param marker)
		// 		more than likely this will need to be a method on the native API.  I can see this as a trigger to
		//		both: (1) add the `? = ` part and also (2) register a REFCURSOR parameter for DBs (Oracle, PGSQL) that
		//		need it.

		final String call = getProducer().getJdbcServices().getJdbcEnvironment().getDialect().getCallableStatementSupport().renderCallableStatement(
				procedureName,
				getParameterMetadata(),
				paramBindings,
				getProducer()
		);

		LOG.debugf( "Preparing procedure call : %s", call );
		final CallableStatement statement = (CallableStatement) getSession()
				.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( call, true );


		// prepare parameters

		getParameterMetadata().visitRegistrations(
				new Consumer<QueryParameter>() {
					int i = 1;

					@Override
					public void accept(QueryParameter queryParameter) {
						try {
							final ParameterRegistrationImplementor registration = (ParameterRegistrationImplementor) queryParameter;
							registration.prepare( statement, i );
							if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
								i++;
							}
							else {
								i += registration.getSqlTypes().length;
							}
						}
						catch (SQLException e) {
							throw getSession().getJdbcServices().getSqlExceptionHelper().convert(
									e,
									"Error preparing registered callable parameter",
									getProcedureName()
							);
						}
					}
				}
		);

		return new ProcedureOutputsImpl( this, statement );
	}

	@Override
	public String getQueryString() {
		return null;
	}

	@Override
	public String[] getReturnAliases() {
		throw new UnsupportedOperationException( "Procedure/function calls do not support returning aliases" );
	}

	@Override
	public Type[] getReturnTypes() {
		throw new UnsupportedOperationException( "Procedure/function calls do not support returning 'return types'" );
	}

	@Override
	public ProcedureCallImplementor<R> setEntity(int position, Object val) {
		return null;
	}

	@Override
	public ProcedureCallImplementor<R> setEntity(String name, Object val) {
		return null;
	}

	/**
	 * Use this form instead of {@link #getSynchronizedQuerySpaces()} when you want to make sure the
	 * underlying Set is instantiated (aka, on add)
	 *
	 * @return The spaces
	 */
	@SuppressWarnings("WeakerAccess")
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

	@SuppressWarnings("WeakerAccess")
	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( Arrays.asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	public ProcedureCallImplementor<R> addSynchronizedEntityClass(Class entityClass) {
		addSynchronizedQuerySpaces( getSession().getFactory().getMetamodel().entityPersister( entityClass.getName() ) );
		return this;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public QueryParameters getQueryParameters() {
		final QueryParameters qp = super.getQueryParameters();
		// both of these are for documentation purposes, they are actually handled directly...
		qp.setAutoDiscoverScalarTypes( true );
		qp.setCallable( true );
		return qp;
	}

	/**
	 * Collects any parameter registrations which indicate a REF_CURSOR parameter type/mode.
	 *
	 * @return The collected REF_CURSOR type parameters.
	 */
	public ParameterRegistrationImplementor[] collectRefCursorParameters() {
		final List<ParameterRegistrationImplementor> refCursorParams = new ArrayList<>();

		getParameterMetadata().visitRegistrations(
				queryParameter -> {
					final ParameterRegistrationImplementor registration = (ParameterRegistrationImplementor) queryParameter;
					if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
						refCursorParams.add( registration );
					}
				}
		);
		return refCursorParams.toArray( new ParameterRegistrationImplementor[refCursorParams.size()] );
	}

	@Override
	public ProcedureCallMemento extractMemento(Map<String, Object> hints) {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				getParameterMetadata().getParameterStrategy(),
				toParameterMementos( getParameterMetadata() ),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( hints )
		);
	}

	@Override
	public ProcedureCallMemento extractMemento() {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				getParameterMetadata().getParameterStrategy(),
				toParameterMementos( getParameterMetadata() ),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( getHints() )
		);
	}

	private static List<ProcedureCallMementoImpl.ParameterMemento> toParameterMementos(ProcedureParameterMetadata parameterMetadata) {
		if ( parameterMetadata.getParameterStrategy() == ParameterStrategy.UNKNOWN ) {
			// none...
			return Collections.emptyList();
		}

		final List<ProcedureCallMementoImpl.ParameterMemento> copy = new ArrayList<>();

		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final ParameterRegistrationImplementor registration = (ParameterRegistrationImplementor) queryParameter;
					copy.add( ProcedureCallMementoImpl.ParameterMemento.fromRegistration( registration ) );
				}
		);

		return copy;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA StoredProcedureQuery impl

	private ProcedureOutputs procedureResult;

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );

		try {
			registerParameter( position, type, mode );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		getProducer().checkOpen( true );
		try {
			registerParameter( parameterName, type, mode );
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
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
			throw getExceptionConverter().convert( he );
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
	public int executeUpdate() {
		getProducer().checkTransactionNeededForUpdateOperation(
				"javax.persistence.Query.executeUpdate requires active transaction" );

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
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}
	}


	@Override
	@SuppressWarnings("unchecked")
	public List<R> getResultList() {
		if ( getMaxResults() == 0 ) {
			return Collections.EMPTY_LIST;
		}
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
			throw getExceptionConverter().convert( he );
		}
		catch (RuntimeException e) {
			getProducer().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public R getSingleResult() {
		final List<R> resultList = getResultList();
		if ( resultList == null || resultList.isEmpty() ) {
			throw new NoResultException(
					String.format(
							"Call to stored procedure [%s] returned no results",
							getProcedureName()
					)
			);
		}
		else if ( resultList.size() > 1 ) {
			throw new NonUniqueResultException(
					String.format(
							"Call to stored procedure [%s] returned multiple results",
							getProcedureName()
					)
			);
		}

		return resultList.get( 0 );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}
		else if ( cls.isInstance( outputs ) ) {
			return (T) outputs();
		}

		return super.unwrap( cls );
	}

	@Override
	public ProcedureCallImplementor<R> setLockMode(LockModeType lockMode) {
		throw new IllegalStateException( "javax.persistence.Query.setLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new IllegalStateException( "javax.persistence.Query.getHibernateFlushMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public ProcedureCallImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	// todo (5.3) : all of the parameter stuff here can be done in AbstractProducedQuery
	//		using #getParameterMetadata and #getQueryParameterBindings for abstraction.
	//		this "win" is to define these in one place

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		paramBindings.getBinding( getParameterMetadata().resolve( parameter ) ).setBindValue( value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(Parameter<P> parameter, P value) {
		paramBindings.getBinding( getParameterMetadata().resolve( parameter ) ).setBindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value) {
		paramBindings.getBinding( getParameterMetadata().getQueryParameter( name ) ).setBindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value) {
		paramBindings.getBinding( getParameterMetadata().getQueryParameter( position ) ).setBindValue( value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type type) {
		final QueryParameterBinding<P> binding = paramBindings.getBinding( parameter );
		binding.setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(String name, Object value, Type type) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().getQueryParameter( name ) );
		binding.setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(int position, Object value, Type type) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().getQueryParameter( position ) );
		binding.setBindValue( value, type );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( parameter );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(String name, Object value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().getQueryParameter( name ) );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(int position, Object value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().getQueryParameter( position ) );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Calendar value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().resolve( parameter ) );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Date value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( getParameterMetadata().resolve( parameter ) );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( name );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( name );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( position );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		final QueryParameterBinding binding = paramBindings.getBinding( position );
		binding.setBindValue( value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Stream getResultStream() {
		return getResultList().stream();
	}

}
