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
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;
import org.hibernate.sql.results.NoMoreOutputsException;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl<R>
		extends AbstractQuery<R>
		implements ProcedureCallImplementor<R>, ResultContext, DomainParameterBindingContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ProcedureCallImpl.class.getName()
	);

	private final String procedureName;

	private FunctionReturnImpl functionReturn;

	private final ProcedureParameterMetadataImpl parameterMetadata;
	private final ProcedureParamBindings paramBindings;

	private final List<DomainResultProducer<?>> domainResultProducers;

	private Set<String> synchronizedQuerySpaces;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private ProcedureOutputsImpl outputs;


	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(SharedSessionContractImplementor session, String procedureName) {
		super( session );
		this.procedureName = procedureName;

		this.parameterMetadata = new ProcedureParameterMetadataImpl();
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, getSessionFactory() );

		this.synchronizedQuerySpaces = null;
		this.domainResultProducers = null;
	}
	/**
	 * The result Class(es) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultClasses The classes making up the result
	 */
	public ProcedureCallImpl(SharedSessionContractImplementor session, String procedureName, Class... resultClasses) {
		super( session );

		assert resultClasses != null && resultClasses.length > 0;

		this.procedureName = procedureName;

		this.parameterMetadata = new ProcedureParameterMetadataImpl();
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, getSessionFactory() );

		this.domainResultProducers = CollectionHelper.arrayList( resultClasses.length );
		this.synchronizedQuerySpaces = new HashSet<>();

		Util.resolveResultSetMappingClasses(
				resultClasses,
				domainResultProducers::add,
				synchronizedQuerySpaces::add,
				getSession().getFactory()
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

		assert resultSetMappingNames != null && resultSetMappingNames.length > 0;

		this.procedureName = procedureName;

		this.parameterMetadata = new ProcedureParameterMetadataImpl();
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, getSessionFactory() );

		this.domainResultProducers = CollectionHelper.arrayList( resultSetMappingNames.length );
		this.synchronizedQuerySpaces = new HashSet<>();

		Util.resolveResultSetMappingNames(
				resultSetMappingNames,
				domainResultProducers::add,
				synchronizedQuerySpaces::add,
				getSession().getFactory()
		);
	}

	/**
	 * The named/stored copy constructor
	 *
	 * @param session The session
	 * @param memento The named/stored memento
	 */
	ProcedureCallImpl(SharedSessionContractImplementor session, NamedCallableQueryMemento memento) {
		super( session );
		this.procedureName = memento.getCallableName();

		this.parameterMetadata = new ProcedureParameterMetadataImpl( memento, session );
		this.paramBindings = new ProcedureParamBindings( parameterMetadata, getSessionFactory() );

		this.domainResultProducers = new ArrayList<>();
		this.synchronizedQuerySpaces = CollectionHelper.makeCopy( memento.getQuerySpaces() );

		Util.resolveResultSetMappings(
				memento.getResultSetMappingNames(),
				memento.getResultSetMappingClasses(),
				domainResultProducers::add,
				synchronizedQuerySpaces::add,
				getSession().getFactory()
		);

		applyOptions( memento );
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
	public ProcedureParameterMetadataImpl getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return paramBindings;
	}

	public ParameterStrategy getParameterStrategy() {
		return getParameterMetadata().getParameterStrategy();
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
	public DomainParameterBindingContext getDomainParameterBindingContext() {
		return this;
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return paramBindings;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainParameterBindingContext


	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	public <T> List<T> getLoadIdentifiers() {
		return Collections.emptyList();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Parameter registrations

	@Override
	@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
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
	@Override
	@SuppressWarnings("unchecked")
	public <T> ProcedureParameter<T> registerParameter(int position, Class<T> javaType, ParameterMode mode) {
		final ProcedureParameterImpl procedureParameter = new ProcedureParameterImpl(
				position,
				mode,
				javaType,
				getSessionFactory().getDomainModel().resolveQueryParameterType( javaType )
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
	public ProcedureParameterImplementor getParameterRegistration(int position) {
		return getParameterMetadata().getQueryParameter( position );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ProcedureParameterImplementor<T> registerParameter(String name, Class<T> javaType, ParameterMode mode) {
		final ProcedureParameterImpl parameter = new ProcedureParameterImpl(
				name,
				mode,
				javaType,
				getSessionFactory().getDomainModel().resolveQueryParameterType( javaType )
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
	public ProcedureParameterImplementor getParameterRegistration(String name) {
		return getParameterMetadata().getQueryParameter( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getRegisteredParameters() {
		return new ArrayList( getParameterMetadata().getRegistrations() );
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

//		final String call = getSession().getJdbcServices().getJdbcEnvironment().getDialect().getCallableStatementSupport().renderCallableStatement(
//				procedureName,
//				getParameterMetadata(),
//				paramBindings,
//				getSession()
//		);
//
//		LOG.debugf( "Preparing procedure call : %s", call );
//		final CallableStatement statement = (CallableStatement) getSession()
//				.getJdbcCoordinator()
//				.getStatementPreparer()
//				.prepareStatement( call, true );
//
//
//		// prepare parameters
//
//		getParameterMetadata().visitRegistrations(
//				new Consumer<QueryParameter<?>>() {
//					int i = 1;
//
//					@Override
//					public void accept(QueryParameter queryParameter) {
//						try {
//							final ProcedureParameterImplementor registration = (ProcedureParameterImplementor) queryParameter;
//							registration.prepare( statement, i, ProcedureCallImpl.this );
//							if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
//								i++;
//							}
//							else {
//								i += registration.getSqlTypes().length;
//							}
//						}
//						catch (SQLException e) {
//							throw getSession().getJdbcServices().getSqlExceptionHelper().convert(
//									e,
//									"Error preparing registered callable parameter",
//									getProcedureName()
//							);
//						}
//					}
//				}
//		);
//
//		return new ProcedureOutputsImpl( this, statement );
		throw new NotYetImplementedFor6Exception( getClass() );
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
	@SuppressWarnings("WeakerAccess")
	protected Set<String> synchronizedQuerySpaces() {
		if ( synchronizedQuerySpaces == null ) {
			synchronizedQuerySpaces = new HashSet<>();
		}
		return synchronizedQuerySpaces;
	}

	@Override
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

	/**
	 * Collects any parameter registrations which indicate a REF_CURSOR parameter type/mode.
	 *
	 * @return The collected REF_CURSOR type parameters.
	 */
	public ProcedureParameterImplementor[] collectRefCursorParameters() {
		final List<ProcedureParameterImplementor> refCursorParams = new ArrayList<>();

		getParameterMetadata().visitRegistrations(
				queryParameter -> {
					final ProcedureParameterImplementor registration = (ProcedureParameterImplementor) queryParameter;
					if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
						refCursorParams.add( registration );
					}
				}
		);
		return refCursorParams.toArray( new ProcedureParameterImplementor[0] );
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
				getHibernateFlushMode(),
				isReadOnly(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	private static List<NamedCallableQueryMemento.ParameterMemento> toParameterMementos(
			ProcedureParameterMetadataImpl parameterMetadata) {
		if ( parameterMetadata.getParameterStrategy() == ParameterStrategy.UNKNOWN ) {
			// none...
			return Collections.emptyList();
		}

		final List<NamedCallableQueryMemento.ParameterMemento> mementos = new ArrayList<>();

		parameterMetadata.visitRegistrations(
				queryParameter -> {
					final ProcedureParameterImplementor procedureParameter = (ProcedureParameterImplementor) queryParameter;
					mementos.add(
							new NamedCallableQueryMementoImpl.ParameterMementoImpl(
									procedureParameter.getPosition(),
									procedureParameter.getName(),
									procedureParameter.getMode(),
									procedureParameter.getParameterType(),
									procedureParameter.getHibernateType()
							)
					);
				}
		);

		return mementos;
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
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph) {
		throw new IllegalStateException( "EntityGraph hints are not supported for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	public Query<R> applyGraph(RootGraph<?> graph, GraphSemantic semantic) {
		throw new IllegalStateException( "EntityGraph hints are not supported for ProcedureCall/StoredProcedureQuery" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// outputs

	private ProcedureOutputs procedureResult;

	@Override
	public boolean execute() {
		try {
			final Output rtn = outputs().getCurrent();
			return ResultSetOutput.class.isInstance( rtn );
		}
		catch (NoMoreOutputsException e) {
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
		catch (NoMoreOutputsException e) {
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
		catch (NoMoreOutputsException e) {
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
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		throw new UnsupportedOperationException( "Query#scroll is not valid for ProcedureCall/StoredProcedureQuery" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<R> getResultList() {
		if ( getMaxResults() == 0 ) {
			return Collections.EMPTY_LIST;
		}
		try {
			final Output rtn = outputs().getCurrent();
			if ( !(rtn instanceof ResultSetOutput) ) {
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

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( paramBindings ) ) {
			return (T) paramBindings;
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
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value );
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(Parameter<P> parameter, P value) {
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value) {
		return (ProcedureCallImplementor<R>) super.setParameter( name, value );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value) {
		return (ProcedureCallImplementor<R>) super.setParameter( position, value );
	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, AllowableParameterType type) {
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value, type );
	}

//	@Override
//	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, Type type) {
//		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value, type );
//	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value, AllowableParameterType type) {
		return (ProcedureCallImplementor<R>) super.setParameter( name, value, type );
	}

//	@Override
//	public ProcedureCallImplementor<R> setParameter(String name, Object value, Type type) {
//		return (ProcedureCallImplementor<R>) super.setParameter( name, value, type );
//	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value, AllowableParameterType type) {
		//noinspection unchecked
		return (ProcedureCallImplementor<R>) super.setParameter( position, value, type );
	}

//	@Override
//	public ProcedureCallImplementor<R> setParameter(int position, Object value, Type type) {
//		return (ProcedureCallImplementor<R>) super.setParameter( position, value, type );
//	}

	@Override
	public <P> ProcedureCallImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Object value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( name, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Object value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( position, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Calendar value, TemporalType temporalPrecision) {
		//noinspection unchecked
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(Parameter parameter, Date value, TemporalType temporalPrecision) {
		//noinspection unchecked
		return (ProcedureCallImplementor<R>) super.setParameter( parameter, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( name, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( name, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( position, value, temporalPrecision );
	}

	@Override
	public ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalPrecision) {
		return (ProcedureCallImplementor<R>) super.setParameter( position, value, temporalPrecision );
	}
}
