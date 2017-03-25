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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
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
import org.hibernate.query.procedure.internal.ProcedureParameterImpl;
import org.hibernate.query.procedure.internal.ProcedureParameterMetadata;
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

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ParameterRegistrationImplementor<?>> registeredParameters = new ArrayList<>();

	private Set<String> synchronizedQuerySpaces;

	private ProcedureOutputsImpl outputs;

	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(SharedSessionContractImplementor session, String procedureName) {
		super( session, new ProcedureParameterMetadata() );
		this.procedureName = procedureName;
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

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
		super( session, new ProcedureParameterMetadata() );
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
	}

	/**
	 * The result-set-mapping(s) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultSetMappings The names of the result set mappings making up the result
	 */
	public ProcedureCallImpl(final SharedSessionContractImplementor session, String procedureName, String... resultSetMappings) {
		super( session, new ProcedureParameterMetadata() );
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
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	@SuppressWarnings("unchecked")
	ProcedureCallImpl(SharedSessionContractImplementor session, ProcedureCallMementoImpl memento) {
		super( session, new ProcedureParameterMetadata() );
		this.procedureName = memento.getProcedureName();
		this.globalParameterPassNullsSetting = session.getFactory().getSessionFactoryOptions().isProcedureParameterNullPassingEnabled();

		this.queryReturns = memento.getQueryReturns();
		this.synchronizedQuerySpaces = Util.copy( memento.getSynchronizedQuerySpaces() );
		this.parameterStrategy = memento.getParameterStrategy();
		if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
			// nothing else to do in this case
			return;
		}

		final List<ProcedureCallMementoImpl.ParameterMemento> storedRegistrations = memento.getParameterDeclarations();
		if ( storedRegistrations == null ) {
			// most likely a problem if ParameterStrategy is not UNKNOWN...
			LOG.debugf(
					"ParameterStrategy was [%s] on named copy [%s], but no parameters stored",
					parameterStrategy,
					procedureName
			);
			return;
		}

		final List<ParameterRegistrationImplementor<?>> parameterRegistrations =
				CollectionHelper.arrayList( storedRegistrations.size() );

		for ( ProcedureCallMementoImpl.ParameterMemento storedRegistration : storedRegistrations ) {
			final ParameterRegistrationImplementor<?> registration;
			if ( StringHelper.isNotEmpty( storedRegistration.getName() ) ) {
				if ( parameterStrategy != ParameterStrategy.NAMED ) {
					throw new IllegalStateException(
							"Found named stored procedure parameter associated with positional parameters"
					);
				}
				registration = new NamedParameterRegistration(
						this,
						storedRegistration.getName(),
						storedRegistration.getMode(),
						storedRegistration.getType(),
						storedRegistration.getHibernateType(),
						storedRegistration.isPassNullsEnabled()
				);
			}
			else {
				if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
					throw new IllegalStateException(
							"Found named stored procedure parameter associated with positional parameters"
					);
				}
				registration = new PositionalParameterRegistration(
						this,
						storedRegistration.getPosition(),
						storedRegistration.getMode(),
						storedRegistration.getType(),
						storedRegistration.getHibernateType(),
						storedRegistration.isPassNullsEnabled()
				);
			}
			getParameterMetadata().registerParameter( new ProcedureParameterImpl( registration ) );
			parameterRegistrations.add( registration );
		}
		this.registeredParameters = parameterRegistrations;

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

	public ParameterStrategy getParameterStrategy() {
		return parameterStrategy;
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

		final PositionalParameterRegistration parameterRegistration =
				new PositionalParameterRegistration( this, position, mode, type, globalParameterPassNullsSetting );
		registerParameter( parameterRegistration );
		return parameterRegistration;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(int position, Class type, ParameterMode mode) {
		registerParameter( position, type, mode );
		return this;
	}

	private void registerParameter(ParameterRegistrationImplementor parameter) {
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			prepareForNamedParameters();
		}
		else if ( parameter.getPosition() != null ) {
			prepareForPositionalParameters();
		}
		else {
			throw new IllegalArgumentException( "Given parameter did not define name or position [" + parameter + "]" );
		}
		((ProcedureParameterMetadata)getParameterMetadata()).registerParameter( new ProcedureParameterImpl( parameter ) );

		registeredParameters.add( parameter );
	}

	private void prepareForPositionalParameters() {
		if ( parameterStrategy == ParameterStrategy.NAMED ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		parameterStrategy = ParameterStrategy.POSITIONAL;
	}

	private void prepareForNamedParameters() {
		if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		if ( parameterStrategy == ParameterStrategy.UNKNOWN ) {
			// protect to only do this check once
			final ExtractedDatabaseMetaData databaseMetaData = getSession()
					.getJdbcCoordinator()
					.getJdbcSessionOwner()
					.getJdbcSessionContext()
					.getServiceRegistry().getService( JdbcEnvironment.class )
					.getExtractedDatabaseMetaData();
			if ( ! databaseMetaData.supportsNamedParameters() ) {
				LOG.unsupportedNamedParameters();
			}
			parameterStrategy = ParameterStrategy.NAMED;
		}
	}

	@Override
	public ParameterRegistrationImplementor getParameterRegistration(int position) {
		if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
			throw new ParameterStrategyException(
					"Attempt to access positional parameter [" + position + "] but ProcedureCall using named parameters"
			);
		}
		for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
			if ( position == parameter.getPosition() ) {
				return parameter;
			}
		}
		throw new NoSuchParameterException( "Could not locate parameter registered using that position [" + position + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistration<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
		final NamedParameterRegistration parameterRegistration = new NamedParameterRegistration( this, name, mode, type, globalParameterPassNullsSetting );
		registerParameter( parameterRegistration );
		return parameterRegistration;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ProcedureCall registerParameter0(String name, Class type, ParameterMode mode) {
		registerParameter( name, type, mode );
		return this;
	}

	@Override
	public ParameterRegistrationImplementor getParameterRegistration(String name) {
		if ( parameterStrategy != ParameterStrategy.NAMED ) {
			throw new ParameterStrategyException( "Names were not used to register parameters with this stored procedure call" );
		}
		for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		throw new NoSuchParameterException( "Could not locate parameter registered under that name [" + name + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ParameterRegistration> getRegisteredParameters() {
		return new ArrayList<>( registeredParameters );
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
				parameterStrategy,
				registeredParameters,
				getProducer()
		);

		try {
			LOG.debugf( "Preparing procedure call : %s", call );
			final CallableStatement statement = (CallableStatement) getSession()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( call, true );


			// prepare parameters
			int i = 1;

			for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
				parameter.prepare( statement, i );
				if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
					i++;
				}
				else {
					i += parameter.getSqlTypes().length;
				}
			}

			return new ProcedureOutputsImpl( this, statement );
		}
		catch (SQLException e) {
			throw getSession().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error preparing CallableStatement",
					getProcedureName()
			);
		}
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
		for ( ParameterRegistrationImplementor param : registeredParameters ) {
			if ( param.getMode() == ParameterMode.REF_CURSOR ) {
				refCursorParams.add( param );
			}
		}
		return refCursorParams.toArray( new ParameterRegistrationImplementor[refCursorParams.size()] );
	}

	@Override
	public ProcedureCallMemento extractMemento(Map<String, Object> hints) {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				parameterStrategy,
				toParameterMementos( registeredParameters ),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( hints )
		);
	}

	@Override
	public ProcedureCallMemento extractMemento() {
		return new ProcedureCallMementoImpl(
				procedureName,
				Util.copy( queryReturns ),
				parameterStrategy,
				toParameterMementos( registeredParameters ),
				Util.copy( synchronizedQuerySpaces ),
				Util.copy( getHints() )
		);
	}

	private static List<ProcedureCallMementoImpl.ParameterMemento> toParameterMementos(List<ParameterRegistrationImplementor<?>> registeredParameters) {
		if ( registeredParameters == null ) {
			return null;
		}

		final List<ProcedureCallMementoImpl.ParameterMemento> copy = CollectionHelper.arrayList( registeredParameters.size() );
		for ( ParameterRegistrationImplementor registration : registeredParameters ) {
			copy.add( ProcedureCallMementoImpl.ParameterMemento.fromRegistration( registration ) );
		}
		return copy;
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
			throw getExceptionConverter().convert( he );
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

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		locateParameterRegistration( parameter ).bindValue( value );
		return this;
	}

	@SuppressWarnings("unchecked")
	private  <P> ParameterRegistrationImplementor<P> locateParameterRegistration(Parameter<P> parameter) {
		if ( parameter.getName() != null ) {
			return locateParameterRegistration( parameter.getName() );
		}

		if ( parameter.getPosition() != null ) {
			return locateParameterRegistration( parameter.getPosition() );
		}

		throw getExceptionConverter().convert(
				new IllegalArgumentException( "Could not resolve registration for given parameter reference [" + parameter + "]" )
		);
	}

	@SuppressWarnings("unchecked")
	private <P> ParameterRegistrationImplementor<P> locateParameterRegistration(String name) {
		assert name != null;

		if ( parameterStrategy == ParameterStrategy.POSITIONAL ) {
			throw new IllegalArgumentException( "Expecting positional parameter" );
		}

		for ( ParameterRegistrationImplementor<?> registeredParameter : registeredParameters ) {
			if ( name.equals( registeredParameter.getName() ) ) {
				return (ParameterRegistrationImplementor<P>) registeredParameter;
			}
		}

		throw new IllegalArgumentException( "Unknown parameter registration name [" + name + "]" );
	}

	@SuppressWarnings("unchecked")
	private <P> ParameterRegistrationImplementor<P> locateParameterRegistration(int position) {
		if ( parameterStrategy == ParameterStrategy.NAMED ) {
			throw new IllegalArgumentException( "Expecting named parameter" );
		}

		for ( ParameterRegistrationImplementor<?> registeredParameter : registeredParameters ) {
			if ( registeredParameter.getPosition() != null && registeredParameter.getPosition() == position ) {
				return (ParameterRegistrationImplementor<P>) registeredParameter;
			}
		}

		throw new IllegalArgumentException( "Unknown parameter registration position [" + position + "]" );
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(Parameter<P> parameter, P value) {
		locateParameterRegistration( parameter ).bindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value) {
		locateParameterRegistration( name ).bindValue( value );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value) {
		locateParameterRegistration( position ).bindValue( value );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type type) {
		final ParameterRegistrationImplementor<P> reg = locateParameterRegistration( parameter );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value, Type type) {
		final ParameterRegistrationImplementor reg = locateParameterRegistration( name );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value, Type type) {
		final ParameterRegistrationImplementor reg = locateParameterRegistration( position );
		reg.bindValue( value );
		reg.setHibernateType( type );
		return this;
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		locateParameterRegistration( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value, TemporalType temporalType) {
		locateParameterRegistration( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value, TemporalType temporalType) {
		locateParameterRegistration( position ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(QueryParameter parameter, Collection values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Collection values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Object[] values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Calendar value, TemporalType temporalType) {
		locateParameterRegistration( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Date value, TemporalType temporalType) {
		locateParameterRegistration( parameter ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		locateParameterRegistration( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		locateParameterRegistration( name ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		locateParameterRegistration( position ).bindValue( value, temporalType );
		return this;
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		locateParameterRegistration( position ).bindValue( value, temporalType );
		return this;
	}
}
