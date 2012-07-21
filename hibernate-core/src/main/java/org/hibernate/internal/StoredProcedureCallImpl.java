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
package org.hibernate.internal;

import javax.persistence.ParameterMode;
import javax.persistence.TemporalType;
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
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.StoredProcedureCall;
import org.hibernate.StoredProcedureOutputs;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.type.DateType;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class StoredProcedureCallImpl extends AbstractBasicQueryContractImpl implements StoredProcedureCall {
	private static final Logger log = Logger.getLogger( StoredProcedureCallImpl.class );

	private final String procedureName;
	private final NativeSQLQueryReturn[] queryReturns;

	private TypeOfParameter typeOfParameters = TypeOfParameter.UNKNOWN;
	private List<StoredProcedureParameterImplementor> registeredParameters = new ArrayList<StoredProcedureParameterImplementor>();

	private Set<String> synchronizedQuerySpaces;

	@SuppressWarnings("unchecked")
	public StoredProcedureCallImpl(SessionImplementor session, String procedureName) {
		this( session, procedureName, (List) null );
	}

	public StoredProcedureCallImpl(SessionImplementor session, String procedureName, List<NativeSQLQueryReturn> queryReturns) {
		super( session );
		this.procedureName = procedureName;

		if ( queryReturns == null || queryReturns.isEmpty() ) {
			this.queryReturns = new NativeSQLQueryReturn[0];
		}
		else {
			this.queryReturns = queryReturns.toArray( new NativeSQLQueryReturn[ queryReturns.size() ] );
		}
	}

	public StoredProcedureCallImpl(SessionImplementor session, String procedureName, Class... resultClasses) {
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

	public StoredProcedureCallImpl(SessionImplementor session, String procedureName, String... resultSetMappings) {
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

//	public StoredProcedureCallImpl(
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
	public String getProcedureName() {
		return procedureName;
	}

	NativeSQLQueryReturn[] getQueryReturns() {
		return queryReturns;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureCall registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		registerParameter( new PositionalStoredProcedureParameter( this, position, mode, type ) );
		return this;
	}

	private void registerParameter(StoredProcedureParameterImplementor parameter) {
		if ( StringHelper.isNotEmpty( parameter.getName() ) ) {
			prepareForNamedParameters();
		}
		else if ( parameter.getPosition() != null ) {
			prepareForPositionalParameters();
		}
		else {
			throw new IllegalArgumentException( "Given parameter did not define name nor position [" + parameter + "]" );
		}
		registeredParameters.add( parameter );
	}

	private void prepareForPositionalParameters() {
		if ( typeOfParameters == TypeOfParameter.NAMED ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		typeOfParameters = TypeOfParameter.POSITIONAL;
	}

	private void prepareForNamedParameters() {
		if ( typeOfParameters == TypeOfParameter.POSITIONAL ) {
			throw new QueryException( "Cannot mix named and positional parameters" );
		}
		if ( typeOfParameters == null ) {
			// protect to only do this check once
			final ExtractedDatabaseMetaData databaseMetaData = session().getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.getJdbcServices()
					.getExtractedMetaDataSupport();
			if ( ! databaseMetaData.supportsNamedParameters() ) {
				throw new QueryException(
						"Named stored procedure parameters used, but JDBC driver does not support named parameters"
				);
			}
			typeOfParameters = TypeOfParameter.NAMED;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureCall registerStoredProcedureParameter(String name, Class type, ParameterMode mode) {
		registerParameter( new NamedStoredProcedureParameter( this, name, mode, type ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StoredProcedureParameter> getRegisteredParameters() {
		return new ArrayList<StoredProcedureParameter>( registeredParameters );
	}

	@Override
	public StoredProcedureParameterImplementor getRegisteredParameter(String name) {
		if ( typeOfParameters != TypeOfParameter.NAMED ) {
			throw new IllegalArgumentException( "Names were not used to register parameters with this stored procedure call" );
		}
		for ( StoredProcedureParameterImplementor parameter : registeredParameters ) {
			if ( name.equals( parameter.getName() ) ) {
				return parameter;
			}
		}
		throw new IllegalArgumentException( "Could not locate parameter registered under that name [" + name + "]" );
	}

	@Override
	public StoredProcedureParameterImplementor getRegisteredParameter(int position) {
		try {
			return registeredParameters.get( position );
		}
		catch ( Exception e ) {
			throw new QueryException( "Could not locate parameter registered using that position [" + position + "]" );
		}
	}

	@Override
	public StoredProcedureOutputs getOutputs() {

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
		for ( StoredProcedureParameterImplementor parameter : registeredParameters ) {
			for ( int i = 0; i < parameter.getSqlTypes().length; i++ ) {
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
		}
		buffer.append( ")}" );

		try {
			final CallableStatement statement = session().getTransactionCoordinator()
					.getJdbcCoordinator()
					.getLogicalConnection()
					.getShareableConnectionProxy()
					.prepareCall( buffer.toString() );

			// prepare parameters
			int i = 1;
			for ( StoredProcedureParameterImplementor parameter : registeredParameters ) {
				if ( parameter == null ) {
					throw new QueryException( "Registered stored procedure parameters had gaps" );
				}

				parameter.prepare( statement, i );
				i += parameter.getSqlTypes().length;
			}

			return new StoredProcedureOutputsImpl( this, statement );
		}
		catch (SQLException e) {
			throw session().getFactory().getSQLExceptionHelper().convert(
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
	public Collection<String> getSynchronizedQuerySpaces() {
		if ( synchronizedQuerySpaces == null ) {
			return Collections.emptySet();
		}
		else {
			return Collections.unmodifiableSet( synchronizedQuerySpaces );
		}
	}

	public Set<String> getSynchronizedQuerySpacesSet() {
		return (Set<String>) getSynchronizedQuerySpaces();
	}

	@Override
	public StoredProcedureCallImpl addSynchronizedQuerySpace(String querySpace) {
		synchronizedQuerySpaces().add( querySpace );
		return this;
	}

	@Override
	public StoredProcedureCallImpl addSynchronizedEntityName(String entityName) {
		addSynchronizedQuerySpaces( session().getFactory().getEntityPersister( entityName ) );
		return this;
	}

	protected void addSynchronizedQuerySpaces(EntityPersister persister) {
		synchronizedQuerySpaces().addAll( Arrays.asList( (String[]) persister.getQuerySpaces() ) );
	}

	@Override
	public StoredProcedureCallImpl addSynchronizedEntityClass(Class entityClass) {
		addSynchronizedQuerySpaces( session().getFactory().getEntityPersister( entityClass.getName() ) );
		return this;
	}

	public QueryParameters buildQueryParametersObject() {
		QueryParameters qp = super.buildQueryParametersObject();
		// both of these are for documentation purposes, they are actually handled directly...
		qp.setAutoDiscoverScalarTypes( true );
		qp.setCallable( true );
		return qp;
	}

	public StoredProcedureParameterImplementor[] collectRefCursorParameters() {
		List<StoredProcedureParameterImplementor> refCursorParams = new ArrayList<StoredProcedureParameterImplementor>();
		for ( StoredProcedureParameterImplementor param : registeredParameters ) {
			if ( param.getMode() == ParameterMode.REF_CURSOR ) {
				refCursorParams.add( param );
			}
		}
		return refCursorParams.toArray( new StoredProcedureParameterImplementor[refCursorParams.size()] );
	}

	/**
	 * Ternary logic enum
	 */
	private static enum TypeOfParameter {
		NAMED,
		POSITIONAL,
		UNKNOWN
	}

	protected static interface StoredProcedureParameterImplementor<T> extends StoredProcedureParameter<T> {
		public void prepare(CallableStatement statement, int i) throws SQLException;

		public int[] getSqlTypes();

		public T extract(CallableStatement statement);
	}

	public static abstract class AbstractStoredProcedureParameterImpl<T> implements StoredProcedureParameterImplementor<T> {
		private final StoredProcedureCallImpl procedureCall;

		private final ParameterMode mode;
		private final Class<T> type;

		private int startIndex;
		private Type hibernateType;
		private int[] sqlTypes;

		private StoredProcedureParameterBindImpl bind;

		protected AbstractStoredProcedureParameterImpl(
				StoredProcedureCallImpl procedureCall,
				ParameterMode mode,
				Class<T> type) {
			this.procedureCall = procedureCall;
			this.mode = mode;
			this.type = type;

			setHibernateType( session().getFactory().getTypeResolver().heuristicType( type.getName() ) );
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public Integer getPosition() {
			return null;
		}

		@Override
		public Class<T> getType() {
			return type;
		}

		@Override
		public ParameterMode getMode() {
			return mode;
		}

		@Override
		public void setHibernateType(Type type) {
			if ( type == null ) {
				throw new IllegalArgumentException( "Type cannot be null" );
			}
			this.hibernateType = type;
			this.sqlTypes = hibernateType.sqlTypes( session().getFactory() );
		}

		protected SessionImplementor session() {
			return procedureCall.session();
		}

		@Override
		public void prepare(CallableStatement statement, int startIndex) throws SQLException {
			if ( mode == ParameterMode.REF_CURSOR ) {
				throw new NotYetImplementedException( "Support for REF_CURSOR parameters not yet supported" );
			}

			this.startIndex = startIndex;
			if ( mode == ParameterMode.IN || mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
				if ( mode == ParameterMode.INOUT || mode == ParameterMode.OUT ) {
					if ( sqlTypes.length > 1 ) {
						if ( ProcedureParameterExtractionAware.class.isInstance( hibernateType )
								&& ( (ProcedureParameterExtractionAware) hibernateType ).canDoExtraction() ) {
							// the type can handle multi-param extraction...
						}
						else {
							// it cannot...
							throw new UnsupportedOperationException(
									"Type [" + hibernateType + "] does support multi-parameter value extraction"
							);
						}
					}
					for ( int i = 0; i < sqlTypes.length; i++ ) {
						statement.registerOutParameter( startIndex + i, sqlTypes[i] );
					}
				}

				if ( mode == ParameterMode.INOUT || mode == ParameterMode.IN ) {
					if ( bind == null || bind.getValue() == null ) {
						log.debugf(
								"Stored procedure [%s] IN/INOUT parameter [%s] not bound; assuming procedure defines default value",
								procedureCall.getProcedureName(),
								this
						);
					}
					else {
						final Type typeToUse;
						if ( bind.getExplicitTemporalType() != null && bind.getExplicitTemporalType() == TemporalType.TIMESTAMP ) {
							typeToUse = hibernateType;
						}
						else if ( bind.getExplicitTemporalType() != null && bind.getExplicitTemporalType() == TemporalType.DATE ) {
							typeToUse = DateType.INSTANCE;
						}
						else {
							typeToUse = hibernateType;
						}
						typeToUse.nullSafeSet( statement, bind.getValue(), startIndex, session() );
					}
				}
			}
			else {
				// we have a REF_CURSOR type param
				if ( procedureCall.typeOfParameters == TypeOfParameter.NAMED ) {
					session().getFactory().getServiceRegistry()
							.getService( RefCursorSupport.class )
							.registerRefCursorParameter( statement, getName() );
				}
				else {
					session().getFactory().getServiceRegistry()
							.getService( RefCursorSupport.class )
							.registerRefCursorParameter( statement, getPosition() );
				}
			}
		}

		public int[] getSqlTypes() {
			return sqlTypes;
		}

		@Override
		public StoredProcedureParameterBind getParameterBind() {
			return bind;
		}

		@Override
		public void bindValue(T value) {
			this.bind = new StoredProcedureParameterBindImpl<T>( value );
		}

		@Override
		public void bindValue(T value, TemporalType explicitTemporalType) {
			if ( explicitTemporalType != null ) {
				if ( ! isDateTimeType() ) {
					throw new IllegalArgumentException( "TemporalType should not be specified for non date/time type" );
				}
			}
			this.bind = new StoredProcedureParameterBindImpl<T>( value, explicitTemporalType );
		}

		private boolean isDateTimeType() {
			return Date.class.isAssignableFrom( type )
					|| Calendar.class.isAssignableFrom( type );
		}

		@Override
		@SuppressWarnings("unchecked")
		public T extract(CallableStatement statement) {
			if ( mode == ParameterMode.IN ) {
				throw new QueryException( "IN parameter not valid for output extraction" );
			}
			else if ( mode == ParameterMode.REF_CURSOR ) {
				throw new QueryException( "REF_CURSOR parameters should be accessed via results" );
			}

			try {
				if ( ProcedureParameterExtractionAware.class.isInstance( hibernateType ) ) {
					return (T) ( (ProcedureParameterExtractionAware) hibernateType ).extract( statement, startIndex, session() );
				}
				else {
					return (T) statement.getObject( startIndex );
				}
			}
			catch (SQLException e) {
				throw procedureCall.session().getFactory().getSQLExceptionHelper().convert(
						e,
						"Unable to extract OUT/INOUT parameter value"
				);
			}
		}
	}

	public static class StoredProcedureParameterBindImpl<T> implements StoredProcedureParameterBind<T> {
		private final T value;
		private final TemporalType explicitTemporalType;

		public StoredProcedureParameterBindImpl(T value) {
			this( value, null );
		}

		public StoredProcedureParameterBindImpl(T value, TemporalType explicitTemporalType) {
			this.value = value;
			this.explicitTemporalType = explicitTemporalType;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public TemporalType getExplicitTemporalType() {
			return explicitTemporalType;
		}
	}

	public static class NamedStoredProcedureParameter<T> extends AbstractStoredProcedureParameterImpl<T> {
		private final String name;

		public NamedStoredProcedureParameter(
				StoredProcedureCallImpl procedureCall,
				String name,
				ParameterMode mode,
				Class<T> type) {
			super( procedureCall, mode, type );
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class PositionalStoredProcedureParameter<T> extends AbstractStoredProcedureParameterImpl<T> {
		private final Integer position;

		public PositionalStoredProcedureParameter(
				StoredProcedureCallImpl procedureCall,
				Integer position,
				ParameterMode mode,
				Class<T> type) {
			super( procedureCall, mode, type );
			this.position = position;
		}

		@Override
		public Integer getPosition() {
			return position;
		}
	}
}
