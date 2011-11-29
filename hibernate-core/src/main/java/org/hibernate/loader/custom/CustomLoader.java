/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.Loader;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;


/**
 * Extension point for loaders which use a SQL result set with "unexpected" column aliases.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CustomLoader extends Loader {

	// Currently *not* cachable if autodiscover types is in effect (e.g. "select * ...")

	private final String sql;
	private final Set querySpaces = new HashSet();
	private final Map namedParameterBindPoints;

	private final Queryable[] entityPersisters;
	private final int[] entiytOwners;
	private final EntityAliases[] entityAliases;

	private final QueryableCollection[] collectionPersisters;
	private final int[] collectionOwners;
	private final CollectionAliases[] collectionAliases;

	private final LockMode[] lockModes;

	private boolean[] includeInResultRow;

//	private final String[] sqlAliases;
//	private final String[] sqlAliasSuffixes;
	private final ResultRowProcessor rowProcessor;

	// this is only needed (afaict) for processing results from the query cache;
	// however, this cannot possibly work in the case of discovered types...
	private Type[] resultTypes;

	// this is only needed (afaict) for ResultTransformer processing...
	private String[] transformerAliases;

	public CustomLoader(CustomQuery customQuery, SessionFactoryImplementor factory) {
		super( factory );

		this.sql = customQuery.getSQL();
		this.querySpaces.addAll( customQuery.getQuerySpaces() );
		this.namedParameterBindPoints = customQuery.getNamedParameterBindPoints();

		List entityPersisters = new ArrayList();
		List entityOwners = new ArrayList();
		List entityAliases = new ArrayList();

		List collectionPersisters = new ArrayList();
		List collectionOwners = new ArrayList();
		List collectionAliases = new ArrayList();

		List lockModes = new ArrayList();
		List resultColumnProcessors = new ArrayList();
		List nonScalarReturnList = new ArrayList();
		List resultTypes = new ArrayList();
		List specifiedAliases = new ArrayList();
		int returnableCounter = 0;
		boolean hasScalars = false;

		List includeInResultRowList = new ArrayList();

		Iterator itr = customQuery.getCustomQueryReturns().iterator();
		while ( itr.hasNext() ) {
			final Return rtn = ( Return ) itr.next();
			if ( rtn instanceof ScalarReturn ) {
				ScalarReturn scalarRtn = ( ScalarReturn ) rtn;
				resultTypes.add( scalarRtn.getType() );
				specifiedAliases.add( scalarRtn.getColumnAlias() );
				resultColumnProcessors.add(
						new ScalarResultColumnProcessor(
								StringHelper.unquote( scalarRtn.getColumnAlias(), factory.getDialect() ),
								scalarRtn.getType()
						)
				);
				includeInResultRowList.add( true );
				hasScalars = true;
			}
			else if ( rtn instanceof RootReturn ) {
				RootReturn rootRtn = ( RootReturn ) rtn;
				Queryable persister = ( Queryable ) factory.getEntityPersister( rootRtn.getEntityName() );
				entityPersisters.add( persister );
				lockModes.add( (rootRtn.getLockMode()) );
				resultColumnProcessors.add( new NonScalarResultColumnProcessor( returnableCounter++ ) );
				nonScalarReturnList.add( rtn );
				entityOwners.add( -1 );
				resultTypes.add( persister.getType() );
				specifiedAliases.add( rootRtn.getAlias() );
				entityAliases.add( rootRtn.getEntityAliases() );
				ArrayHelper.addAll( querySpaces, persister.getQuerySpaces() );
				includeInResultRowList.add( true );
			}
			else if ( rtn instanceof CollectionReturn ) {
				CollectionReturn collRtn = ( CollectionReturn ) rtn;
				String role = collRtn.getOwnerEntityName() + "." + collRtn.getOwnerProperty();
				QueryableCollection persister = ( QueryableCollection ) factory.getCollectionPersister( role );
				collectionPersisters.add( persister );
				lockModes.add( collRtn.getLockMode() );
				resultColumnProcessors.add( new NonScalarResultColumnProcessor( returnableCounter++ ) );
				nonScalarReturnList.add( rtn );
				collectionOwners.add( -1 );
				resultTypes.add( persister.getType() );
				specifiedAliases.add( collRtn.getAlias() );
				collectionAliases.add( collRtn.getCollectionAliases() );
				// determine if the collection elements are entities...
				Type elementType = persister.getElementType();
				if ( elementType.isEntityType() ) {
					Queryable elementPersister = ( Queryable ) ( ( EntityType ) elementType ).getAssociatedJoinable( factory );
					entityPersisters.add( elementPersister );
					entityOwners.add( -1 );
					entityAliases.add( collRtn.getElementEntityAliases() );
					ArrayHelper.addAll( querySpaces, elementPersister.getQuerySpaces() );
				}
				includeInResultRowList.add( true );
			}
			else if ( rtn instanceof EntityFetchReturn ) {
				EntityFetchReturn fetchRtn = ( EntityFetchReturn ) rtn;
				NonScalarReturn ownerDescriptor = fetchRtn.getOwner();
				int ownerIndex = nonScalarReturnList.indexOf( ownerDescriptor );
				entityOwners.add( ownerIndex );
				lockModes.add( fetchRtn.getLockMode() );
				Queryable ownerPersister = determineAppropriateOwnerPersister( ownerDescriptor );
				EntityType fetchedType = ( EntityType ) ownerPersister.getPropertyType( fetchRtn.getOwnerProperty() );
				String entityName = fetchedType.getAssociatedEntityName( getFactory() );
				Queryable persister = ( Queryable ) factory.getEntityPersister( entityName );
				entityPersisters.add( persister );
				nonScalarReturnList.add( rtn );
				specifiedAliases.add( fetchRtn.getAlias() );
				entityAliases.add( fetchRtn.getEntityAliases() );
				ArrayHelper.addAll( querySpaces, persister.getQuerySpaces() );
				includeInResultRowList.add( false );
			}
			else if ( rtn instanceof CollectionFetchReturn ) {
				CollectionFetchReturn fetchRtn = ( CollectionFetchReturn ) rtn;
				NonScalarReturn ownerDescriptor = fetchRtn.getOwner();
				int ownerIndex = nonScalarReturnList.indexOf( ownerDescriptor );
				collectionOwners.add( ownerIndex );
				lockModes.add( fetchRtn.getLockMode() );
				Queryable ownerPersister = determineAppropriateOwnerPersister( ownerDescriptor );
				String role = ownerPersister.getEntityName() + '.' + fetchRtn.getOwnerProperty();
				QueryableCollection persister = ( QueryableCollection ) factory.getCollectionPersister( role );
				collectionPersisters.add( persister );
				nonScalarReturnList.add( rtn );
				specifiedAliases.add( fetchRtn.getAlias() );
				collectionAliases.add( fetchRtn.getCollectionAliases() );
				// determine if the collection elements are entities...
				Type elementType = persister.getElementType();
				if ( elementType.isEntityType() ) {
					Queryable elementPersister = ( Queryable ) ( ( EntityType ) elementType ).getAssociatedJoinable( factory );
					entityPersisters.add( elementPersister );
					entityOwners.add( ownerIndex );
					entityAliases.add( fetchRtn.getElementEntityAliases() );
					ArrayHelper.addAll( querySpaces, elementPersister.getQuerySpaces() );
				}
				includeInResultRowList.add( false );
			}
			else {
				throw new HibernateException( "unexpected custom query return type : " + rtn.getClass().getName() );
			}
		}

		this.entityPersisters = new Queryable[ entityPersisters.size() ];
		for ( int i = 0; i < entityPersisters.size(); i++ ) {
			this.entityPersisters[i] = ( Queryable ) entityPersisters.get( i );
		}
		this.entiytOwners = ArrayHelper.toIntArray( entityOwners );
		this.entityAliases = new EntityAliases[ entityAliases.size() ];
		for ( int i = 0; i < entityAliases.size(); i++ ) {
			this.entityAliases[i] = ( EntityAliases ) entityAliases.get( i );
		}

		this.collectionPersisters = new QueryableCollection[ collectionPersisters.size() ];
		for ( int i = 0; i < collectionPersisters.size(); i++ ) {
			this.collectionPersisters[i] = ( QueryableCollection ) collectionPersisters.get( i );
		}
		this.collectionOwners = ArrayHelper.toIntArray( collectionOwners );
		this.collectionAliases = new CollectionAliases[ collectionAliases.size() ];
		for ( int i = 0; i < collectionAliases.size(); i++ ) {
			this.collectionAliases[i] = ( CollectionAliases ) collectionAliases.get( i );
		}

		this.lockModes = new LockMode[ lockModes.size() ];
		for ( int i = 0; i < lockModes.size(); i++ ) {
			this.lockModes[i] = ( LockMode ) lockModes.get( i );
		}

		this.resultTypes = ArrayHelper.toTypeArray( resultTypes );
		this.transformerAliases = ArrayHelper.toStringArray( specifiedAliases );

		this.rowProcessor = new ResultRowProcessor(
				hasScalars,
		        ( ResultColumnProcessor[] ) resultColumnProcessors.toArray( new ResultColumnProcessor[ resultColumnProcessors.size() ] )
		);

		this.includeInResultRow = ArrayHelper.toBooleanArray( includeInResultRowList );
	}

	private Queryable determineAppropriateOwnerPersister(NonScalarReturn ownerDescriptor) {
		String entityName = null;
		if ( ownerDescriptor instanceof RootReturn ) {
			entityName = ( ( RootReturn ) ownerDescriptor ).getEntityName();
		}
		else if ( ownerDescriptor instanceof CollectionReturn ) {
			CollectionReturn collRtn = ( CollectionReturn ) ownerDescriptor;
			String role = collRtn.getOwnerEntityName() + "." + collRtn.getOwnerProperty();
			CollectionPersister persister = getFactory().getCollectionPersister( role );
			EntityType ownerType = ( EntityType ) persister.getElementType();
			entityName = ownerType.getAssociatedEntityName( getFactory() );
		}
		else if ( ownerDescriptor instanceof FetchReturn ) {
			FetchReturn fetchRtn = ( FetchReturn ) ownerDescriptor;
			Queryable persister = determineAppropriateOwnerPersister( fetchRtn.getOwner() );
			Type ownerType = persister.getPropertyType( fetchRtn.getOwnerProperty() );
			if ( ownerType.isEntityType() ) {
				entityName = ( ( EntityType ) ownerType ).getAssociatedEntityName( getFactory() );
			}
			else if ( ownerType.isCollectionType() ) {
				Type ownerCollectionElementType = ( ( CollectionType ) ownerType ).getElementType( getFactory() );
				if ( ownerCollectionElementType.isEntityType() ) {
					entityName = ( ( EntityType ) ownerCollectionElementType ).getAssociatedEntityName( getFactory() );
				}
			}
		}

		if ( entityName == null ) {
			throw new HibernateException( "Could not determine fetch owner : " + ownerDescriptor );
		}

		return ( Queryable ) getFactory().getEntityPersister( entityName );
	}

	@Override
    protected String getQueryIdentifier() {
		return sql;
	}

	@Override
    protected String getSQLString() {
		return sql;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	@Override
    protected LockMode[] getLockModes(LockOptions lockOptions) {
		return lockModes;
	}

	@Override
    protected Loadable[] getEntityPersisters() {
		return entityPersisters;
	}

	@Override
    protected CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	@Override
    protected int[] getCollectionOwners() {
		return collectionOwners;
	}

	@Override
    protected int[] getOwners() {
		return entiytOwners;
	}

	public List list(SessionImplementor session, QueryParameters queryParameters) throws HibernateException {
		return list( session, queryParameters, querySpaces, resultTypes );
	}

	public ScrollableResults scroll(
			final QueryParameters queryParameters,
			final SessionImplementor session) throws HibernateException {
		return scroll(
				queryParameters,
				resultTypes,
				getHolderInstantiator( queryParameters.getResultTransformer(), getReturnAliasesForTransformer() ),
				session
		);
	}

	static private HolderInstantiator getHolderInstantiator(ResultTransformer resultTransformer, String[] queryReturnAliases) {
		if ( resultTransformer == null ) {
			return HolderInstantiator.NOOP_INSTANTIATOR;
		}
		else {
			return new HolderInstantiator(resultTransformer, queryReturnAliases);
		}
	}

	@Override
    protected String[] getResultRowAliases() {
		return transformerAliases;
	}

	@Override
    protected ResultTransformer resolveResultTransformer(ResultTransformer resultTransformer) {
		return HolderInstantiator.resolveResultTransformer( null, resultTransformer );
	}

	@Override
    protected boolean[] includeInResultRow() {
		return includeInResultRow;
	}

	@Override
    protected Object getResultColumnOrRow(
			Object[] row,
	        ResultTransformer transformer,
	        ResultSet rs,
	        SessionImplementor session) throws SQLException, HibernateException {
		return rowProcessor.buildResultRow( row, rs, transformer != null, session );
	}

	@Override
    protected Object[] getResultRow(Object[] row, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		return rowProcessor.buildResultRow( row, rs, session );
	}

	@Override
    protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		// meant to handle dynamic instantiation queries...(Copy from QueryLoader)
		HolderInstantiator holderInstantiator = HolderInstantiator.getHolderInstantiator(
				null,
				resultTransformer,
				getReturnAliasesForTransformer()
		);
		if ( holderInstantiator.isRequired() ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = ( Object[] ) results.get( i );
				Object result = holderInstantiator.instantiate(row);
				results.set( i, result );
			}

			return resultTransformer.transformList(results);
		}
		else {
			return results;
		}
	}

	private String[] getReturnAliasesForTransformer() {
		return transformerAliases;
	}

	@Override
    protected EntityAliases[] getEntityAliases() {
		return entityAliases;
	}

	@Override
    protected CollectionAliases[] getCollectionAliases() {
		return collectionAliases;
	}

	@Override
    public int[] getNamedParameterLocs(String name) throws QueryException {
		Object loc = namedParameterBindPoints.get( name );
		if ( loc == null ) {
			throw new QueryException(
					"Named parameter does not appear in Query: " + name,
					sql
			);
		}
		if ( loc instanceof Integer ) {
			return new int[] { ( ( Integer ) loc ).intValue() };
		}
		else {
			return ArrayHelper.toIntArray( ( List ) loc );
		}
	}


	public class ResultRowProcessor {
		private final boolean hasScalars;
		private ResultColumnProcessor[] columnProcessors;

		public ResultRowProcessor(boolean hasScalars, ResultColumnProcessor[] columnProcessors) {
			this.hasScalars = hasScalars || ( columnProcessors == null || columnProcessors.length == 0 );
			this.columnProcessors = columnProcessors;
		}

		public void prepareForAutoDiscovery(Metadata metadata) throws SQLException {
			if ( columnProcessors == null || columnProcessors.length == 0 ) {
				int columns = metadata.getColumnCount();
				columnProcessors = new ResultColumnProcessor[ columns ];
				for ( int i = 1; i <= columns; i++ ) {
					columnProcessors[ i - 1 ] = new ScalarResultColumnProcessor( i );
				}

			}
		}

		/**
		 * Build a logical result row.
		 * <p/>
		 * At this point, Loader has already processed all non-scalar result data.  We
		 * just need to account for scalar result data here...
		 *
		 * @param data Entity data defined as "root returns" and already handled by the
		 * normal Loader mechanism.
		 * @param resultSet The JDBC result set (positioned at the row currently being processed).
		 * @param hasTransformer Does this query have an associated {@link ResultTransformer}
		 * @param session The session from which the query request originated.
		 * @return The logical result row
		 * @throws SQLException
		 * @throws HibernateException
		 */
		public Object buildResultRow(
				Object[] data,
				ResultSet resultSet,
				boolean hasTransformer,
				SessionImplementor session) throws SQLException, HibernateException {
			Object[] resultRow = buildResultRow( data, resultSet, session );
			return ( hasTransformer )
			       ? resultRow
			       : ( resultRow.length == 1 )
			         ? resultRow[0]
			         : resultRow;
		}
		public Object[] buildResultRow(
				Object[] data,
				ResultSet resultSet,
				SessionImplementor session) throws SQLException, HibernateException {
			Object[] resultRow;
			if ( !hasScalars ) {
				resultRow = data;
			}
			else {
				// build an array with indices equal to the total number
				// of actual returns in the result Hibernate will return
				// for this query (scalars + non-scalars)
				resultRow = new Object[ columnProcessors.length ];
				for ( int i = 0; i < columnProcessors.length; i++ ) {
					resultRow[i] = columnProcessors[i].extract( data, resultSet, session );
				}
			}

			return resultRow;
		}
	}

	private static interface ResultColumnProcessor {
		public Object extract(Object[] data, ResultSet resultSet, SessionImplementor session) throws SQLException, HibernateException;
		public void performDiscovery(Metadata metadata, List<Type> types, List<String> aliases) throws SQLException, HibernateException;
	}

	public class NonScalarResultColumnProcessor implements ResultColumnProcessor {
		private final int position;

		public NonScalarResultColumnProcessor(int position) {
			this.position = position;
		}

		@Override
		public Object extract(
				Object[] data,
				ResultSet resultSet,
				SessionImplementor session) throws SQLException, HibernateException {
			return data[ position ];
		}

		@Override
		public void performDiscovery(Metadata metadata, List<Type> types, List<String> aliases) {
		}

	}

	public class ScalarResultColumnProcessor implements ResultColumnProcessor {
		private int position = -1;
		private String alias;
		private Type type;

		public ScalarResultColumnProcessor(int position) {
			this.position = position;
		}

		public ScalarResultColumnProcessor(String alias, Type type) {
			this.alias = alias;
			this.type = type;
		}

		@Override
		public Object extract(
				Object[] data,
				ResultSet resultSet,
				SessionImplementor session) throws SQLException, HibernateException {
			return type.nullSafeGet( resultSet, alias, session, null );
		}

		@Override
		public void performDiscovery(Metadata metadata, List<Type> types, List<String> aliases) throws SQLException {
			if ( alias == null ) {
				alias = metadata.getColumnName( position );
			}
			else if ( position < 0 ) {
				position = metadata.resolveColumnPosition( alias );
			}
			if ( type == null ) {
				type = metadata.getHibernateType( position );
			}
			types.add( type );
			aliases.add( alias );
		}
	}

	@Override
    protected void autoDiscoverTypes(ResultSet rs) {
		try {
			Metadata metadata = new Metadata( getFactory(), rs );
			rowProcessor.prepareForAutoDiscovery( metadata );

			List<String> aliases = new ArrayList<String>();
			List<Type> types = new ArrayList<Type>();
			for ( int i = 0; i < rowProcessor.columnProcessors.length; i++ ) {
				rowProcessor.columnProcessors[i].performDiscovery( metadata, types, aliases );
			}

			// lets make sure we did not end up with duplicate aliases.  this can occur when the user supplied query
			// did not rename same-named columns.  e.g.:
			//		select u.username, u2.username from t_user u, t_user u2 ...
			//
			// the above will lead to an unworkable situation in most cases (the difference is how the driver/db
			// handle this situation.  But if the 'aliases' variable contains duplicate names, then we have that
			// troublesome condition, so lets throw an error.  See HHH-5992
			final HashSet<String> aliasesSet = new HashSet<String>();
			for ( String alias : aliases ) {
				boolean alreadyExisted = !aliasesSet.add( alias );
				if ( alreadyExisted ) {
					throw new NonUniqueDiscoveredSqlAliasException(
							"Encountered a duplicated sql alias [" + alias +
									"] during auto-discovery of a native-sql query"
					);
				}
			}

			resultTypes = ArrayHelper.toTypeArray( types );
			transformerAliases = ArrayHelper.toStringArray( aliases );
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Exception while trying to autodiscover types.", e );
		}
	}

	private static class Metadata {
		private final SessionFactoryImplementor factory;
		private final ResultSet resultSet;
		private final ResultSetMetaData resultSetMetaData;

		public Metadata(SessionFactoryImplementor factory, ResultSet resultSet) throws HibernateException {
			try {
				this.factory = factory;
				this.resultSet = resultSet;
				this.resultSetMetaData = resultSet.getMetaData();
			}
			catch( SQLException e ) {
				throw new HibernateException( "Could not extract result set metadata", e );
			}
		}

		public int getColumnCount() throws HibernateException {
			try {
				return resultSetMetaData.getColumnCount();
			}
			catch( SQLException e ) {
				throw new HibernateException( "Could not determine result set column count", e );
			}
		}

		public int resolveColumnPosition(String columnName) throws HibernateException {
			try {
				return resultSet.findColumn( columnName );
			}
			catch( SQLException e ) {
				throw new HibernateException( "Could not resolve column name in result set [" + columnName + "]", e );
			}
		}

		public String getColumnName(int position) throws HibernateException {
			try {
				return factory.getDialect().getColumnAliasExtractor().extractColumnAlias( resultSetMetaData, position );
			}
			catch( SQLException e ) {
				throw new HibernateException( "Could not resolve column name [" + position + "]", e );
			}
		}

		public Type getHibernateType(int columnPos) throws SQLException {
			int columnType = resultSetMetaData.getColumnType( columnPos );
			int scale = resultSetMetaData.getScale( columnPos );
			int precision = resultSetMetaData.getPrecision( columnPos );
            int length = precision;
            if ( columnType == 1 && precision == 0 ) {
                length = resultSetMetaData.getColumnDisplaySize( columnPos );
            }
			return factory.getTypeResolver().heuristicType(
					factory.getDialect().getHibernateTypeName(
							columnType,
							length,
							precision,
							scale
					)
			);
		}
	}
}
