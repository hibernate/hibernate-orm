/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.procedure.internal;

import javax.persistence.ParameterMode;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.AbstractBasicQueryContractImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.NamedParametersNotSupportedException;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureResult;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.type.Type;

/**
 * Standard implementation of {@link org.hibernate.procedure.ProcedureCall}
 *
 * @author Steve Ebersole
 */
public class ProcedureCallImpl extends AbstractBasicQueryContractImpl implements ProcedureCall, ResultContext {
	private final String procedureName;
	private final NativeSQLQueryReturn[] queryReturns;

	private ParameterStrategy parameterStrategy = ParameterStrategy.UNKNOWN;
	private List<ParameterRegistrationImplementor<?>> registeredParameters = new ArrayList<ParameterRegistrationImplementor<?>>();

	private Set<String> synchronizedQuerySpaces;

	private ProcedureResultImpl outputs;


	@SuppressWarnings("unchecked")
	public ProcedureCallImpl(SessionImplementor session, String procedureName) {
		this( session, procedureName, (List) null );
	}

	public ProcedureCallImpl(SessionImplementor session, String procedureName, List<NativeSQLQueryReturn> queryReturns) {
		super( session );
		this.procedureName = procedureName;

		if ( queryReturns == null || queryReturns.isEmpty() ) {
			this.queryReturns = new NativeSQLQueryReturn[0];
		}
		else {
			this.queryReturns = queryReturns.toArray( new NativeSQLQueryReturn[ queryReturns.size() ] );
		}
	}

	public ProcedureCallImpl(SessionImplementor session, String procedureName, Class... resultClasses) {
		this( session, procedureName, collectQueryReturns( resultClasses ) );
	}

	private static List<NativeSQLQueryReturn> collectQueryReturns(Class[] resultClasses) {
		if ( resultClasses == null || resultClasses.length == 0 ) {
			return null;
		}

		List<NativeSQLQueryReturn> queryReturns = new ArrayList<NativeSQLQueryReturn>( resultClasses.length );
		int i = 1;
		for ( Class resultClass : resultClasses ) {
			queryReturns.add( new NativeSQLQueryRootReturn( "alias" + i, resultClass.getName(), LockMode.READ ) );
			i++;
		}
		return queryReturns;
	}

	public ProcedureCallImpl(SessionImplementor session, String procedureName, String... resultSetMappings) {
		this( session, procedureName, collectQueryReturns( session, resultSetMappings ) );
	}

	private static List<NativeSQLQueryReturn> collectQueryReturns(SessionImplementor session, String[] resultSetMappings) {
		if ( resultSetMappings == null || resultSetMappings.length == 0 ) {
			return null;
		}

		List<NativeSQLQueryReturn> queryReturns = new ArrayList<NativeSQLQueryReturn>( resultSetMappings.length );
		for ( String resultSetMapping : resultSetMappings ) {
			ResultSetMappingDefinition mapping = session.getFactory().getResultSetMapping( resultSetMapping );
			if ( mapping == null ) {
				throw new MappingException( "Unknown SqlResultSetMapping [" + resultSetMapping + "]" );
			}
			queryReturns.addAll( Arrays.asList( mapping.getQueryReturns() ) );
		}
		return queryReturns;
	}

//	public ProcedureCallImpl(
//			SessionImplementor session,
//			String procedureName,
//			List<StoredProcedureParameter> parameters) {
//		// this form is intended for named stored procedure calls.
//		// todo : introduce a NamedProcedureCallDefinition object to hold all needed info and pass that in here; will help with EM.addNamedQuery as well..
//		this( session, procedureName );
//		for ( StoredProcedureParameter parameter : parameters ) {
//			registerParameter( (StoredProcedureParameterImplementor) parameter );
//		}
//	}

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
		final PositionalParameterRegistration parameterRegistration = new PositionalParameterRegistration( this, position, type, mode );
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
		if ( parameterStrategy == null ) {
			// protect to only do this check once
			final ExtractedDatabaseMetaData databaseMetaData = getSession().getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.getJdbcServices()
					.getExtractedMetaDataSupport();
			if ( ! databaseMetaData.supportsNamedParameters() ) {
				throw new NamedParametersNotSupportedException(
						"Named stored procedure parameters used, but JDBC driver does not support named parameters"
				);
			}
			parameterStrategy = ParameterStrategy.NAMED;
		}
	}

	@Override
	public ParameterRegistrationImplementor getParameterRegistration(int position) {
		if ( parameterStrategy != ParameterStrategy.POSITIONAL ) {
			throw new IllegalArgumentException( "Positions were not used to register parameters with this stored procedure call" );
		}
		try {
			return registeredParameters.get( position );
		}
		catch ( Exception e ) {
			throw new QueryException( "Could not locate parameter registered using that position [" + position + "]" );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> ParameterRegistration<T> registerParameter(String name, Class<T> type, ParameterMode mode) {
		final NamedParameterRegistration parameterRegistration = new NamedParameterRegistration( this, name, type, mode );
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
			throw new IllegalArgumentException( "Names were not used to register parameters with this stored procedure call" );
		}
		for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		throw new IllegalArgumentException( "Could not locate parameter registered under that name [" + name + "]" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ParameterRegistration> getRegisteredParameters() {
		return new ArrayList<ParameterRegistration>( registeredParameters );
	}

	@Override
	public ProcedureResult getResult() {
		if ( outputs == null ) {
			outputs = buildOutputs();
		}

		return outputs;
	}

	private ProcedureResultImpl buildOutputs() {
		// todo : going to need a very specialized Loader for this.
		// or, might be a good time to look at splitting Loader up into:
		//		1) building statement objects
		//		2) executing statement objects
		//		3) processing result sets

		// for now assume there are no resultClasses nor mappings defined..
		// 	TOTAL PROOF-OF-CONCEPT!!!!!!

		final StringBuilder buffer = new StringBuilder().append( "{call " )
				.append( procedureName )
				.append( "(" );
		String sep = "";
		for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
			for ( int i = 0; i < parameter.getSqlTypes().length; i++ ) {
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
		}
		buffer.append( ")}" );

		try {
			final CallableStatement statement = (CallableStatement) getSession().getTransactionCoordinator()
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( buffer.toString(), true );

			// prepare parameters
			int i = 1;
			for ( ParameterRegistrationImplementor parameter : registeredParameters ) {
				if ( parameter == null ) {
					throw new QueryException( "Registered stored procedure parameters had gaps" );
				}

				parameter.prepare( statement, i );
				i += parameter.getSqlTypes().length;
			}

			return new ProcedureResultImpl( this, statement );
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

	public QueryParameters buildQueryParametersObject() {
		QueryParameters qp = super.buildQueryParametersObject();
		// both of these are for documentation purposes, they are actually handled directly...
		qp.setAutoDiscoverScalarTypes( true );
		qp.setCallable( true );
		return qp;
	}

	public ParameterRegistrationImplementor[] collectRefCursorParameters() {
		List<ParameterRegistrationImplementor> refCursorParams = new ArrayList<ParameterRegistrationImplementor>();
		for ( ParameterRegistrationImplementor param : registeredParameters ) {
			if ( param.getMode() == ParameterMode.REF_CURSOR ) {
				refCursorParams.add( param );
			}
		}
		return refCursorParams.toArray( new ParameterRegistrationImplementor[refCursorParams.size()] );
	}
}
