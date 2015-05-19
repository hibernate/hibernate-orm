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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ParameterMode;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractBasicQueryContractImpl;
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
import org.hibernate.result.spi.ResultContext;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl extends AbstractBasicQueryContractImpl implements ProcedureCall, ResultContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ProcedureCallImpl.class.getName()
	);

	private static final NativeSQLQueryReturn[] NO_RETURNS = new NativeSQLQueryReturn[0];

	private final String procedureName;
	private final NativeSQLQueryReturn[] queryReturns;

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ParameterRegistrationImplementor<?>> registeredParameters = new ArrayList<ParameterRegistrationImplementor<?>>();

	private Set<String> synchronizedQuerySpaces;

	private ProcedureOutputsImpl outputs;

	/**
	 * The no-returns form.
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 */
	public ProcedureCallImpl(SessionImplementor session, String procedureName) {
		super( session );
		this.procedureName = procedureName;
		this.queryReturns = NO_RETURNS;
	}

	/**
	 * The result Class(es) return form
	 *
	 * @param session The session
	 * @param procedureName The name of the procedure to call
	 * @param resultClasses The classes making up the result
	 */
	public ProcedureCallImpl(final SessionImplementor session, String procedureName, Class... resultClasses) {
		super( session );
		this.procedureName = procedureName;

		final List<NativeSQLQueryReturn> collectedQueryReturns = new ArrayList<NativeSQLQueryReturn>();
		final Set<String> collectedQuerySpaces = new HashSet<String>();

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
	public ProcedureCallImpl(final SessionImplementor session, String procedureName, String... resultSetMappings) {
		super( session );
		this.procedureName = procedureName;

		final List<NativeSQLQueryReturn> collectedQueryReturns = new ArrayList<NativeSQLQueryReturn>();
		final Set<String> collectedQuerySpaces = new HashSet<String>();

		Util.resolveResultSetMappings(
				new Util.ResultSetMappingResolutionContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return session.getFactory();
					}

					@Override
					public ResultSetMappingDefinition findResultSetMapping(String name) {
						return session.getFactory().getResultSetMapping( name );
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
	ProcedureCallImpl(SessionImplementor session, ProcedureCallMementoImpl memento) {
		super( session );
		this.procedureName = memento.getProcedureName();

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
						storedRegistration.getHibernateType()
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
						storedRegistration.getHibernateType()
				);
			}
			parameterRegistrations.add( registration );
		}
		this.registeredParameters = parameterRegistrations;
	}

	@Override
	public SessionImplementor getSession() {
		return super.session();
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
				new PositionalParameterRegistration( this, position, mode, type );
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
		final NamedParameterRegistration parameterRegistration = new NamedParameterRegistration( this, name, mode, type );
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
		return new ArrayList<ParameterRegistration>( registeredParameters );
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

		final String call = session().getFactory().getDialect().getCallableStatementSupport().renderCallableStatement(
				procedureName,
				parameterStrategy,
				registeredParameters,
				session()
		);

		try {
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
			throw getSession().getFactory().getSQLExceptionHelper().convert(
					e,
					"Error preparing CallableStatement",
					getProcedureName()
			);
		}
	}

	@Override
	public Type[] getReturnTypes() throws HibernateException {
		throw new NotYetImplementedException();
	}

	/**
	 * Use this form instead of {@link #getSynchronizedQuerySpaces()} when you want to make sure the
	 * underlying Set is instantiated (aka, on add)
	 *
	 * @return The spaces
	 */
	protected Set<String> synchronizedQuerySpaces() {
		if ( synchronizedQuerySpaces == null ) {
			synchronizedQuerySpaces = new HashSet<String>();
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
	public ProcedureCallImpl addSynchronizedQuerySpace(String querySpace) {
		synchronizedQuerySpaces().add( querySpace );
		return this;
	}

	@Override
	public ProcedureCallImpl addSynchronizedEntityName(String entityName) {
		addSynchronizedQuerySpaces( getSession().getFactory().getEntityPersister( entityName ) );
		return this;
	}

	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( Arrays.asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	public ProcedureCallImpl addSynchronizedEntityClass(Class entityClass) {
		addSynchronizedQuerySpaces( getSession().getFactory().getEntityPersister( entityClass.getName() ) );
		return this;
	}

	@Override
	public QueryParameters getQueryParameters() {
		return buildQueryParametersObject();
	}

	@Override
	public QueryParameters buildQueryParametersObject() {
		final QueryParameters qp = super.buildQueryParametersObject();
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
		final List<ParameterRegistrationImplementor> refCursorParams = new ArrayList<ParameterRegistrationImplementor>();
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
}
