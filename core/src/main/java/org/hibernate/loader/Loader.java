/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.loader;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.LockOptions;
import org.hibernate.cache.FilterKey;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.QueryKey;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.EntityUniqueKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.engine.TypedValue;
import org.hibernate.engine.jdbc.ColumnNameCache;
import org.hibernate.event.EventSource;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.HolderInstantiator;
import org.hibernate.impl.FetchingScrollableResultsImpl;
import org.hibernate.impl.ScrollableResultsImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.util.StringHelper;

/**
 * Abstract superclass of object loading (and querying) strategies. This class implements
 * useful common functionality that concrete loaders delegate to. It is not intended that this
 * functionality would be directly accessed by client code. (Hence, all methods of this class
 * are declared <tt>protected</tt> or <tt>private</tt>.) This class relies heavily upon the
 * <tt>Loadable</tt> interface, which is the contract between this class and
 * <tt>EntityPersister</tt>s that may be loaded by it.<br>
 * <br>
 * The present implementation is able to load any number of columns of entities and at most
 * one collection role per query.
 *
 * @author Gavin King
 * @see org.hibernate.persister.entity.Loadable
 */
public abstract class Loader {

	private static final Logger log = LoggerFactory.getLogger( Loader.class );

	private final SessionFactoryImplementor factory;
	private ColumnNameCache columnNameCache;

	public Loader(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 *
	 * @return The sql command this loader should use to get its {@link ResultSet}.
	 */
	protected abstract String getSQLString();

	/**
	 * An array of persisters of entity classes contained in each row of results;
	 * implemented by all subclasses
	 *
	 * @return The entity persisters.
	 */
	protected abstract Loadable[] getEntityPersisters();
	
	/**
	 * An array indicating whether the entities have eager property fetching
	 * enabled.
	 *
	 * @return Eager property fetching indicators.
	 */
	protected boolean[] getEntityEagerPropertyFetches() {
		return null;
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner").  The
	 * indexes contained here are relative to the result of
	 * {@link #getEntityPersisters}.
	 *
	 * @return The owner indicators (see discussion above).
	 */
	protected int[] getOwners() {
		return null;
	}

	/**
	 * An array of the owner types corresponding to the {@link #getOwners()}
	 * returns.  Indices indicating no owner would be null here.
	 *
	 * @return The types for the owners.
	 */
	protected EntityType[] getOwnerAssociationTypes() {
		return null;
	}

	/**
	 * An (optional) persister for a collection to be initialized; only 
	 * collection loaders return a non-null value
	 */
	protected CollectionPersister[] getCollectionPersisters() {
		return null;
	}

	/**
	 * Get the index of the entity that owns the collection, or -1
	 * if there is no owner in the query results (ie. in the case of a
	 * collection initializer) or no collection.
	 */
	protected int[] getCollectionOwners() {
		return null;
	}

	/**
	 * What lock options does this load entities with?
	 *
	 * @param lockOptions a collection of lock options specified dynamically via the Query interface
	 */
	//protected abstract LockOptions[] getLockOptions(Map lockOptions);
	protected abstract LockMode[] getLockModes(LockOptions lockOptions);

	/**
	 * Append <tt>FOR UPDATE OF</tt> clause, if necessary. This
	 * empty superclass implementation merely returns its first
	 * argument.
	 */
	protected String applyLocks(String sql, LockOptions lockOptions, Dialect dialect) throws HibernateException {
		return sql;
	}

	/**
	 * Does this query return objects that might be already cached
	 * by the session, whose lock mode may need upgrading
	 */
	protected boolean upgradeLocks() {
		return false;
	}

	/**
	 * Return false is this loader is a batch entity loader
	 */
	protected boolean isSingleRowLoader() {
		return false;
	}

	/**
	 * Get the SQL table aliases of entities whose
	 * associations are subselect-loadable, returning
	 * null if this loader does not support subselect
	 * loading
	 */
	protected String[] getAliases() {
		return null;
	}

	/**
	 * Modify the SQL, adding lock hints and comments, if necessary
	 */
	protected String preprocessSQL(String sql, QueryParameters parameters, Dialect dialect)
			throws HibernateException {

		sql = applyLocks( sql, parameters.getLockOptions(), dialect );
		
		return getFactory().getSettings().isCommentsEnabled() ?
				prependComment( sql, parameters ) : sql;
	}

	private String prependComment(String sql, QueryParameters parameters) {
		String comment = parameters.getComment();
		if ( comment == null ) {
			return sql;
		}
		else {
			return new StringBuffer( comment.length() + sql.length() + 5 )
					.append( "/* " )
					.append( comment )
					.append( " */ " )
					.append( sql )
					.toString();
		}
	}

	/**
	 * Execute an SQL query and attempt to instantiate instances of the class mapped by the given
	 * persister from each row of the <tt>ResultSet</tt>. If an object is supplied, will attempt to
	 * initialize that object. If a collection is supplied, attempt to initialize that collection.
	 */
	private List doQueryAndInitializeNonLazyCollections(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies) throws HibernateException, SQLException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		if ( queryParameters.isReadOnlyInitialized() ) {
			// The read-only/modifiable mode for the query was explicitly set.
			// Temporarily set the default read-only/modifiable setting to the query's setting.
			persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
		}
		else {
			// The read-only/modifiable setting for the query was not initialized.
			// Use the default read-only/modifiable from the persistence context instead.
			queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
		}
		persistenceContext.beforeLoad();
		List result;
		try {
			try {
				result = doQuery( session, queryParameters, returnProxies );
			}
			finally {
				persistenceContext.afterLoad();
			}
			persistenceContext.initializeNonLazyCollections();
		}
		finally {
			// Restore the original default
			persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
		}
		return result;
	}

	/**
	 * Loads a single row from the result set.  This is the processing used from the
	 * ScrollableResults where no collection fetches were encountered.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 * @return The loaded "row".
	 * @throws HibernateException
	 */
	public Object loadSingleRow(
	        final ResultSet resultSet,
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final boolean returnProxies) throws HibernateException {

		final int entitySpan = getEntityPersisters().length;
		final List hydratedObjects = entitySpan == 0 ? 
				null : new ArrayList( entitySpan );

		final Object result;
		try {
			result = getRowFromResultSet(
			        resultSet,
					session,
					queryParameters,
					getLockModes( queryParameters.getLockOptions() ),
					null,
					hydratedObjects,
					new EntityKey[entitySpan],
					returnProxies
				);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not read next row of results",
			        getSQLString()
				);
		}

		initializeEntitiesAndCollections( 
				hydratedObjects, 
				resultSet, 
				session, 
				queryParameters.isReadOnly( session )
			);
		session.getPersistenceContext().initializeNonLazyCollections();
		return result;
	}

	private Object sequentialLoad(
	        final ResultSet resultSet,
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final boolean returnProxies,
	        final EntityKey keyToRead) throws HibernateException {

		final int entitySpan = getEntityPersisters().length;
		final List hydratedObjects = entitySpan == 0 ? 
				null : new ArrayList( entitySpan );

		Object result = null;
		final EntityKey[] loadedKeys = new EntityKey[entitySpan];

		try {
			do {
				Object loaded = getRowFromResultSet(
						resultSet,
						session,
						queryParameters,
						getLockModes( queryParameters.getLockOptions() ),
						null,
						hydratedObjects,
						loadedKeys,
						returnProxies
					);
				if ( result == null ) {
					result = loaded;
				}
			} 
			while ( keyToRead.equals( loadedKeys[0] ) && resultSet.next() );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not doAfterTransactionCompletion sequential read of results (forward)",
			        getSQLString()
				);
		}

		initializeEntitiesAndCollections( 
				hydratedObjects, 
				resultSet, 
				session, 
				queryParameters.isReadOnly( session )
			);
		session.getPersistenceContext().initializeNonLazyCollections();
		return result;
	}

	/**
	 * Loads a single logical row from the result set moving forward.  This is the
	 * processing used from the ScrollableResults where there were collection fetches
	 * encountered; thus a single logical row may have multiple rows in the underlying
	 * result set.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 * @return The loaded "row".
	 * @throws HibernateException
	 */
	public Object loadSequentialRowsForward(
	        final ResultSet resultSet,
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final boolean returnProxies) throws HibernateException {

		// note that for sequential scrolling, we make the assumption that
		// the first persister element is the "root entity"

		try {
			if ( resultSet.isAfterLast() ) {
				// don't even bother trying to read further
				return null;
			}

			if ( resultSet.isBeforeFirst() ) {
				resultSet.next();
			}

			// We call getKeyFromResultSet() here so that we can know the
			// key value upon which to doAfterTransactionCompletion the breaking logic.  However,
			// it is also then called from getRowFromResultSet() which is certainly
			// not the most efficient.  But the call here is needed, and there
			// currently is no other way without refactoring of the doQuery()/getRowFromResultSet()
			// methods
			final EntityKey currentKey = getKeyFromResultSet(
					0,
					getEntityPersisters()[0],
					null,
					resultSet,
					session
				);

			return sequentialLoad( resultSet, session, queryParameters, returnProxies, currentKey );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not doAfterTransactionCompletion sequential read of results (forward)",
			        getSQLString()
				);
		}
	}

	/**
	 * Loads a single logical row from the result set moving forward.  This is the
	 * processing used from the ScrollableResults where there were collection fetches
	 * encountered; thus a single logical row may have multiple rows in the underlying
	 * result set.
	 *
	 * @param resultSet The result set from which to do the load.
	 * @param session The session from which the request originated.
	 * @param queryParameters The query parameters specified by the user.
	 * @param returnProxies Should proxies be generated
	 * @return The loaded "row".
	 * @throws HibernateException
	 */
	public Object loadSequentialRowsReverse(
	        final ResultSet resultSet,
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final boolean returnProxies,
	        final boolean isLogicallyAfterLast) throws HibernateException {

		// note that for sequential scrolling, we make the assumption that
		// the first persister element is the "root entity"

		try {
			if ( resultSet.isFirst() ) {
				// don't even bother trying to read any further
				return null;
			}

			EntityKey keyToRead = null;
			// This check is needed since processing leaves the cursor
			// after the last physical row for the current logical row;
			// thus if we are after the last physical row, this might be
			// caused by either:
			//      1) scrolling to the last logical row
			//      2) scrolling past the last logical row
			// In the latter scenario, the previous logical row
			// really is the last logical row.
			//
			// In all other cases, we should process back two
			// logical records (the current logic row, plus the
			// previous logical row).
			if ( resultSet.isAfterLast() && isLogicallyAfterLast ) {
				// position cursor to the last row
				resultSet.last();
				keyToRead = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
					);
			}
			else {
				// Since the result set cursor is always left at the first
				// physical row after the "last processed", we need to jump
				// back one position to get the key value we are interested
				// in skipping
				resultSet.previous();

				// sequentially read the result set in reverse until we recognize
				// a change in the key value.  At that point, we are pointed at
				// the last physical sequential row for the logical row in which
				// we are interested in processing
				boolean firstPass = true;
				final EntityKey lastKey = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
					);
				while ( resultSet.previous() ) {
					EntityKey checkKey = getKeyFromResultSet(
							0,
							getEntityPersisters()[0],
							null,
							resultSet,
							session
						);

					if ( firstPass ) {
						firstPass = false;
						keyToRead = checkKey;
					}

					if ( !lastKey.equals( checkKey ) ) {
						break;
					}
				}

			}

			// Read backwards until we read past the first physical sequential
			// row with the key we are interested in loading
			while ( resultSet.previous() ) {
				EntityKey checkKey = getKeyFromResultSet(
						0,
						getEntityPersisters()[0],
						null,
						resultSet,
						session
					);

				if ( !keyToRead.equals( checkKey ) ) {
					break;
				}
			}

			// Finally, read ahead one row to position result set cursor
			// at the first physical row we are interested in loading
			resultSet.next();

			// and doAfterTransactionCompletion the load
			return sequentialLoad( resultSet, session, queryParameters, returnProxies, keyToRead );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not doAfterTransactionCompletion sequential read of results (forward)",
			        getSQLString()
				);
		}
	}

	private static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return new EntityKey( 
					optionalId,
					session.getEntityPersister( optionalEntityName, optionalObject ), 
					session.getEntityMode()
				);
		}
		else {
			return null;
		}

	}

	private Object getRowFromResultSet(
	        final ResultSet resultSet,
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final LockMode[] lockModesArray,
	        final EntityKey optionalObjectKey,
	        final List hydratedObjects,
	        final EntityKey[] keys,
	        boolean returnProxies) throws SQLException, HibernateException {

		final Loadable[] persisters = getEntityPersisters();
		final int entitySpan = persisters.length;

		for ( int i = 0; i < entitySpan; i++ ) {
			keys[i] = getKeyFromResultSet(
			        i,
					persisters[i],
					i == entitySpan - 1 ?
							queryParameters.getOptionalId() :
							null,
					resultSet,
					session
				);
			//TODO: the i==entitySpan-1 bit depends upon subclass implementation (very bad)
		}

		registerNonExists( keys, persisters, session );

		// this call is side-effecty
		Object[] row = getRow(
		        resultSet,
				persisters,
				keys,
				queryParameters.getOptionalObject(),
				optionalObjectKey,
				lockModesArray,
				hydratedObjects,
				session
		);

		readCollectionElements( row, resultSet, session );

		if ( returnProxies ) {
			// now get an existing proxy for each row element (if there is one)
			for ( int i = 0; i < entitySpan; i++ ) {
				Object entity = row[i];
				Object proxy = session.getPersistenceContext().proxyFor( persisters[i], keys[i], entity );
				if ( entity != proxy ) {
					// force the proxy to resolve itself
					( (HibernateProxy) proxy ).getHibernateLazyInitializer().setImplementation(entity);
					row[i] = proxy;
				}
			}
		}

		applyPostLoadLocks( row, lockModesArray, session );

		return getResultColumnOrRow( row, queryParameters.getResultTransformer(), resultSet, session );

	}

	protected void applyPostLoadLocks(Object[] row, LockMode[] lockModesArray, SessionImplementor session) {
	}

	/**
	 * Read any collection elements contained in a single row of the result set
	 */
	private void readCollectionElements(Object[] row, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {

		//TODO: make this handle multiple collection roles!

		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {

			final CollectionAliases[] descriptors = getCollectionAliases();
			final int[] collectionOwners = getCollectionOwners();

			for ( int i=0; i<collectionPersisters.length; i++ ) {

				final boolean hasCollectionOwners = collectionOwners !=null && 
						collectionOwners[i] > -1;
				//true if this is a query and we are loading multiple instances of the same collection role
				//otherwise this is a CollectionInitializer and we are loading up a single collection or batch
				
				final Object owner = hasCollectionOwners ?
						row[ collectionOwners[i] ] :
						null; //if null, owner will be retrieved from session

				final CollectionPersister collectionPersister = collectionPersisters[i];
				final Serializable key;
				if ( owner == null ) {
					key = null;
				}
				else {
					key = collectionPersister.getCollectionType().getKeyOfOwner( owner, session );
					//TODO: old version did not require hashmap lookup:
					//keys[collectionOwner].getIdentifier()
				}
	
				readCollectionElement( 
						owner, 
						key, 
						collectionPersister, 
						descriptors[i], 
						resultSet, 
						session 
					);
				
			}

		}
	}

	private List doQuery(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final boolean returnProxies) throws SQLException, HibernateException {

		final RowSelection selection = queryParameters.getRowSelection();
		final int maxRows = hasMaxRows( selection ) ?
				selection.getMaxRows().intValue() :
				Integer.MAX_VALUE;

		final int entitySpan = getEntityPersisters().length;

		final ArrayList hydratedObjects = entitySpan == 0 ? null : new ArrayList( entitySpan * 10 );
		final PreparedStatement st = prepareQueryStatement( queryParameters, false, session );
		final ResultSet rs = getResultSet( st, queryParameters.hasAutoDiscoverScalarTypes(), queryParameters.isCallable(), selection, session );

// would be great to move all this below here into another method that could also be used
// from the new scrolling stuff.
//
// Would need to change the way the max-row stuff is handled (i.e. behind an interface) so
// that I could do the control breaking at the means to know when to stop

		final EntityKey optionalObjectKey = getOptionalObjectKey( queryParameters, session );
		final LockMode[] lockModesArray = getLockModes( queryParameters.getLockOptions() );
		final boolean createSubselects = isSubselectLoadingEnabled();
		final List subselectResultKeys = createSubselects ? new ArrayList() : null;
		final List results = new ArrayList();

		try {

			handleEmptyCollections( queryParameters.getCollectionKeys(), rs, session );

			EntityKey[] keys = new EntityKey[entitySpan]; //we can reuse it for each row

			if ( log.isTraceEnabled() ) log.trace( "processing result set" );

			int count;
			for ( count = 0; count < maxRows && rs.next(); count++ ) {
				
				if ( log.isTraceEnabled() ) log.debug("result set row: " + count);

				Object result = getRowFromResultSet( 
						rs,
						session,
						queryParameters,
						lockModesArray,
						optionalObjectKey,
						hydratedObjects,
						keys,
						returnProxies 
				);
				results.add( result );

				if ( createSubselects ) {
					subselectResultKeys.add(keys);
					keys = new EntityKey[entitySpan]; //can't reuse in this case
				}
				
			}

			if ( log.isTraceEnabled() ) {
				log.trace( "done processing result set (" + count + " rows)" );
			}

		}
		finally {
			session.getBatcher().closeQueryStatement( st, rs );
		}

		initializeEntitiesAndCollections( hydratedObjects, rs, session, queryParameters.isReadOnly( session ) );

		if ( createSubselects ) createSubselects( subselectResultKeys, queryParameters, session );

		return results; //getResultList(results);

	}

	protected boolean isSubselectLoadingEnabled() {
		return false;
	}
	
	protected boolean hasSubselectLoadableCollections() {
		final Loadable[] loadables = getEntityPersisters();
		for (int i=0; i<loadables.length; i++ ) {
			if ( loadables[i].hasSubselectLoadableCollections() ) return true;
		}
		return false;
	}
	
	private static Set[] transpose( List keys ) {
		Set[] result = new Set[ ( ( EntityKey[] ) keys.get(0) ).length ];
		for ( int j=0; j<result.length; j++ ) {
			result[j] = new HashSet( keys.size() );
			for ( int i=0; i<keys.size(); i++ ) {
				result[j].add( ( ( EntityKey[] ) keys.get(i) ) [j] );
			}
		}
		return result;
	}

	private void createSubselects(List keys, QueryParameters queryParameters, SessionImplementor session) {
		if ( keys.size() > 1 ) { //if we only returned one entity, query by key is more efficient
			
			Set[] keySets = transpose(keys);
			
			Map namedParameterLocMap = buildNamedParameterLocMap( queryParameters );
			
			final Loadable[] loadables = getEntityPersisters();
			final String[] aliases = getAliases();
			final Iterator iter = keys.iterator();
			while ( iter.hasNext() ) {
				
				final EntityKey[] rowKeys = (EntityKey[]) iter.next();
				for ( int i=0; i<rowKeys.length; i++ ) {
					
					if ( rowKeys[i]!=null && loadables[i].hasSubselectLoadableCollections() ) {
						
						SubselectFetch subselectFetch = new SubselectFetch( 
								//getSQLString(), 
								aliases[i], 
								loadables[i], 
								queryParameters, 
								keySets[i],
								namedParameterLocMap
							);
						
						session.getPersistenceContext()
								.getBatchFetchQueue()
								.addSubselect( rowKeys[i], subselectFetch );
					}
					
				}
				
			}
		}
	}

	private Map buildNamedParameterLocMap(QueryParameters queryParameters) {
		if ( queryParameters.getNamedParameters()!=null ) {
			final Map namedParameterLocMap = new HashMap();
			Iterator piter = queryParameters.getNamedParameters().keySet().iterator();
			while ( piter.hasNext() ) {
				String name = (String) piter.next();
				namedParameterLocMap.put(
						name,
						getNamedParameterLocs(name)
					);
			}
			return namedParameterLocMap;
		}
		else {
			return null;
		}
	}

	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SessionImplementor session,
			final boolean readOnly) 
	throws HibernateException {
		
		final CollectionPersister[] collectionPersisters = getCollectionPersisters();
		if ( collectionPersisters != null ) {
			for ( int i=0; i<collectionPersisters.length; i++ ) {
				if ( collectionPersisters[i].isArray() ) {
					//for arrays, we should end the collection load before resolving
					//the entities, since the actual array instances are not instantiated
					//during loading
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersisters[i] );
				}
			}
		}

		//important: reuse the same event instances for performance!
		final PreLoadEvent pre;
		final PostLoadEvent post;
		if ( session.isEventSource() ) {
			pre = new PreLoadEvent( (EventSource) session );
			post = new PostLoadEvent( (EventSource) session );
		}
		else {
			pre = null;
			post = null;
		}
		
		if ( hydratedObjects!=null ) {
			int hydratedObjectsSize = hydratedObjects.size();
			if ( log.isTraceEnabled() ) {
				log.trace( "total objects hydrated: " + hydratedObjectsSize );
			}
			for ( int i = 0; i < hydratedObjectsSize; i++ ) {
				TwoPhaseLoad.initializeEntity( hydratedObjects.get(i), readOnly, session, pre, post );
			}
		}
		
		if ( collectionPersisters != null ) {
			for ( int i=0; i<collectionPersisters.length; i++ ) {
				if ( !collectionPersisters[i].isArray() ) {
					//for sets, we should end the collection load after resolving
					//the entities, since we might call hashCode() on the elements
					//TODO: or we could do this polymorphically, and have two
					//      different operations implemented differently for arrays
					endCollectionLoad( resultSetId, session, collectionPersisters[i] );
				}
			}
		}
		
	}

	private void endCollectionLoad(
			final Object resultSetId, 
			final SessionImplementor session, 
			final CollectionPersister collectionPersister) {
		//this is a query and we are loading multiple instances of the same collection role
		session.getPersistenceContext()
				.getLoadContexts()
				.getCollectionLoadContext( ( ResultSet ) resultSetId )
				.endLoadingCollections( collectionPersister );
	}

	protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		return results;
	}

	/**
	 * Get the actual object that is returned in the user-visible result list.
	 * This empty implementation merely returns its first argument. This is
	 * overridden by some subclasses.
	 */
	protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		return row;
	}

	/**
	 * For missing objects associated by one-to-one with another object in the
	 * result set, register the fact that the the object is missing with the
	 * session.
	 */
	private void registerNonExists(
	        final EntityKey[] keys,
	        final Loadable[] persisters,
	        final SessionImplementor session) {
		
		final int[] owners = getOwners();
		if ( owners != null ) {
			
			EntityType[] ownerAssociationTypes = getOwnerAssociationTypes();
			for ( int i = 0; i < keys.length; i++ ) {
				
				int owner = owners[i];
				if ( owner > -1 ) {
					EntityKey ownerKey = keys[owner];
					if ( keys[i] == null && ownerKey != null ) {
						
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						
						/*final boolean isPrimaryKey;
						final boolean isSpecialOneToOne;
						if ( ownerAssociationTypes == null || ownerAssociationTypes[i] == null ) {
							isPrimaryKey = true;
							isSpecialOneToOne = false;
						}
						else {
							isPrimaryKey = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName()==null;
							isSpecialOneToOne = ownerAssociationTypes[i].getLHSPropertyName()!=null;
						}*/
						
						//TODO: can we *always* use the "null property" approach for everything?
						/*if ( isPrimaryKey && !isSpecialOneToOne ) {
							persistenceContext.addNonExistantEntityKey( 
									new EntityKey( ownerKey.getIdentifier(), persisters[i], session.getEntityMode() ) 
							);
						}
						else if ( isSpecialOneToOne ) {*/
						boolean isOneToOneAssociation = ownerAssociationTypes!=null && 
								ownerAssociationTypes[i]!=null && 
								ownerAssociationTypes[i].isOneToOne();
						if ( isOneToOneAssociation ) {
							persistenceContext.addNullProperty( ownerKey, 
									ownerAssociationTypes[i].getPropertyName() );
						}
						/*}
						else {
							persistenceContext.addNonExistantEntityUniqueKey( new EntityUniqueKey( 
									persisters[i].getEntityName(),
									ownerAssociationTypes[i].getRHSUniqueKeyPropertyName(),
									ownerKey.getIdentifier(),
									persisters[owner].getIdentifierType(),
									session.getEntityMode()
							) );
						}*/
					}
				}
			}
		}
	}

	/**
	 * Read one collection element from the current row of the JDBC result set
	 */
	private void readCollectionElement(
	        final Object optionalOwner,
	        final Serializable optionalKey,
	        final CollectionPersister persister,
	        final CollectionAliases descriptor,
	        final ResultSet rs,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();

		final Serializable collectionRowKey = (Serializable) persister.readKey( 
				rs, 
				descriptor.getSuffixedKeyAliases(), 
				session 
			);
		
		if ( collectionRowKey != null ) {
			// we found a collection element in the result set

			if ( log.isDebugEnabled() ) {
				log.debug( 
						"found row of collection: " +
						MessageHelper.collectionInfoString( persister, collectionRowKey, getFactory() ) 
					);
			}

			Object owner = optionalOwner;
			if ( owner == null ) {
				owner = persistenceContext.getCollectionOwner( collectionRowKey, persister );
				if ( owner == null ) {
					//TODO: This is assertion is disabled because there is a bug that means the
					//	  original owner of a transient, uninitialized collection is not known
					//	  if the collection is re-referenced by a different object associated
					//	  with the current Session
					//throw new AssertionFailure("bug loading unowned collection");
				}
			}

			PersistentCollection rowCollection = persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, collectionRowKey );

			if ( rowCollection != null ) {
				rowCollection.readFrom( rs, persister, descriptor, owner );
			}

		}
		else if ( optionalKey != null ) {
			// we did not find a collection element in the result set, so we
			// ensure that a collection is created with the owner's identifier,
			// since what we have is an empty collection

			if ( log.isDebugEnabled() ) {
				log.debug( 
						"result set contains (possibly empty) collection: " +
						MessageHelper.collectionInfoString( persister, optionalKey, getFactory() ) 
					);
			}

			persistenceContext.getLoadContexts()
					.getCollectionLoadContext( rs )
					.getLoadingCollection( persister, optionalKey ); // handle empty collection

		}

		// else no collection element, but also no owner

	}

	/**
	 * If this is a collection initializer, we need to tell the session that a collection
	 * is being initialized, to account for the possibility of the collection having
	 * no elements (hence no rows in the result set).
	 */
	private void handleEmptyCollections(
	        final Serializable[] keys,
	        final Object resultSetId,
	        final SessionImplementor session) {

		if ( keys != null ) {
			// this is a collection initializer, so we must create a collection
			// for each of the passed-in keys, to account for the possibility
			// that the collection is empty and has no rows in the result set

			CollectionPersister[] collectionPersisters = getCollectionPersisters();
			for ( int j=0; j<collectionPersisters.length; j++ ) {
				for ( int i = 0; i < keys.length; i++ ) {
					//handle empty collections
	
					if ( log.isDebugEnabled() ) {
						log.debug( 
								"result set contains (possibly empty) collection: " +
								MessageHelper.collectionInfoString( collectionPersisters[j], keys[i], getFactory() ) 
							);
					}

					session.getPersistenceContext()
							.getLoadContexts()
							.getCollectionLoadContext( ( ResultSet ) resultSetId )
							.getLoadingCollection( collectionPersisters[j], keys[i] );
				}
			}
		}

		// else this is not a collection initializer (and empty collections will
		// be detected by looking for the owner's identifier in the result set)
	}

	/**
	 * Read a row of <tt>Key</tt>s from the <tt>ResultSet</tt> into the given array.
	 * Warning: this method is side-effecty.
	 * <p/>
	 * If an <tt>id</tt> is given, don't bother going to the <tt>ResultSet</tt>.
	 */
	private EntityKey getKeyFromResultSet(
	        final int i,
	        final Loadable persister,
	        final Serializable id,
	        final ResultSet rs,
	        final SessionImplementor session) throws HibernateException, SQLException {

		Serializable resultId;

		// if we know there is exactly 1 row, we can skip.
		// it would be great if we could _always_ skip this;
		// it is a problem for <key-many-to-one>

		if ( isSingleRowLoader() && id != null ) {
			resultId = id;
		}
		else {
			
			Type idType = persister.getIdentifierType();
			resultId = (Serializable) idType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedKeyAliases(),
					session,
					null //problematic for <key-many-to-one>!
				);
			
			final boolean idIsResultId = id != null && 
					resultId != null && 
					idType.isEqual( id, resultId, session.getEntityMode(), factory );
			
			if ( idIsResultId ) resultId = id; //use the id passed in
		}

		return resultId == null ?
				null :
				new EntityKey( resultId, persister, session.getEntityMode() );
	}

	/**
	 * Check the version of the object in the <tt>ResultSet</tt> against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(
	        final int i,
	        final Loadable persister,
	        final Serializable id,
	        final Object entity,
	        final ResultSet rs,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {

		Object version = session.getPersistenceContext().getEntry( entity ).getVersion();

		if ( version != null ) { //null version means the object is in the process of being loaded somewhere else in the ResultSet
			VersionType versionType = persister.getVersionType();
			Object currentVersion = versionType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedVersionAliases(),
					session,
					null
				);
			if ( !versionType.isEqual(version, currentVersion) ) {
				if ( session.getFactory().getStatistics().isStatisticsEnabled() ) {
					session.getFactory().getStatisticsImplementor()
							.optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}

	}

	/**
	 * Resolve any IDs for currently loaded objects, duplications within the
	 * <tt>ResultSet</tt>, etc. Instantiate empty objects to be initialized from the
	 * <tt>ResultSet</tt>. Return an array of objects (a row of results) and an
	 * array of booleans (by side-effect) that determine whether the corresponding
	 * object should be initialized.
	 */
	private Object[] getRow(
	        final ResultSet rs,
	        final Loadable[] persisters,
	        final EntityKey[] keys,
	        final Object optionalObject,
	        final EntityKey optionalObjectKey,
	        final LockMode[] lockModes,
	        final List hydratedObjects,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {

		final int cols = persisters.length;
		final EntityAliases[] descriptors = getEntityAliases();

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"result row: " + 
					StringHelper.toString( keys ) 
				);
		}

		final Object[] rowResults = new Object[cols];

		for ( int i = 0; i < cols; i++ ) {

			Object object = null;
			EntityKey key = keys[i];

			if ( keys[i] == null ) {
				//do nothing
			}
			else {

				//If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor( key );
				if ( object != null ) {
					//its already loaded so don't need to hydrate it
					instanceAlreadyLoaded( 
							rs,
							i,
							persisters[i],
							key,
							object,
							lockModes[i],
							session 
						);
				}
				else {
					object = instanceNotYetLoaded( 
							rs,
							i,
							persisters[i],
							descriptors[i].getRowIdAlias(),
							key,
							lockModes[i],
							optionalObjectKey,
							optionalObject,
							hydratedObjects,
							session 
						);
				}

			}

			rowResults[i] = object;

		}

		return rowResults;
	}

	/**
	 * The entity instance is already in the session cache
	 */
	private void instanceAlreadyLoaded(
	        final ResultSet rs,
	        final int i,
	        final Loadable persister,
	        final EntityKey key,
	        final Object object,
	        final LockMode lockMode,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {
		if ( !persister.isInstance( object, session.getEntityMode() ) ) {
			throw new WrongClassException( 
					"loaded object was of wrong class " + object.getClass(), 
					key.getIdentifier(), 
					persister.getEntityName() 
				);
		}

		if ( LockMode.NONE != lockMode && upgradeLocks() ) { //no point doing this if NONE was requested

			final boolean isVersionCheckNeeded = persister.isVersioned() &&
					session.getPersistenceContext().getEntry(object)
							.getLockMode().lessThan( lockMode );
			// we don't need to worry about existing version being uninitialized
			// because this block isn't called by a re-entrant load (re-entrant
			// loads _always_ have lock mode NONE)
			if (isVersionCheckNeeded) {
				//we only check the version when _upgrading_ lock modes
				checkVersion( i, persister, key.getIdentifier(), object, rs, session );
				//we need to upgrade the lock mode to the mode requested
				session.getPersistenceContext().getEntry(object)
						.setLockMode(lockMode);
			}
		}
	}

	/**
	 * The entity instance is not in the session cache
	 */
	private Object instanceNotYetLoaded(
	        final ResultSet rs,
	        final int i,
	        final Loadable persister,
	        final String rowIdAlias,
	        final EntityKey key,
	        final LockMode lockMode,
	        final EntityKey optionalObjectKey,
	        final Object optionalObject,
	        final List hydratedObjects,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {
		final String instanceClass = getInstanceClass(
				rs, 
				i, 
				persister, 
				key.getIdentifier(), 
				session 
			);

		final Object object;
		if ( optionalObjectKey != null && key.equals( optionalObjectKey ) ) {
			//its the given optional object
			object = optionalObject;
		}
		else {
			// instantiate a new instance
			object = session.instantiate( instanceClass, key.getIdentifier() );
		}

		//need to hydrate it.

		// grab its state from the ResultSet and keep it in the Session
		// (but don't yet initialize the object itself)
		// note that we acquire LockMode.READ even if it was not requested
		LockMode acquiredLockMode = lockMode == LockMode.NONE ? LockMode.READ : lockMode;
		loadFromResultSet( 
				rs, 
				i, 
				object, 
				instanceClass, 
				key, 
				rowIdAlias, 
				acquiredLockMode, 
				persister, 
				session 
			);

		//materialize associations (and initialize the object) later
		hydratedObjects.add( object );

		return object;
	}
	
	private boolean isEagerPropertyFetchEnabled(int i) {
		boolean[] array = getEntityEagerPropertyFetches();
		return array!=null && array[i];
	}


	/**
	 * Hydrate the state an object from the SQL <tt>ResultSet</tt>, into
	 * an array or "hydrated" values (do not resolve associations yet),
	 * and pass the hydrates state to the session.
	 */
	private void loadFromResultSet(
	        final ResultSet rs,
	        final int i,
	        final Object object,
	        final String instanceEntityName,
	        final EntityKey key,
	        final String rowIdAlias,
	        final LockMode lockMode,
	        final Loadable rootPersister,
	        final SessionImplementor session) 
	throws SQLException, HibernateException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable persister = (Loadable) getFactory().getEntityPersister( instanceEntityName );

		if ( log.isTraceEnabled() ) {
			log.trace( 
					"Initializing object from ResultSet: " + 
					MessageHelper.infoString( persister, id, getFactory() ) 
				);
		}
		
		boolean eagerPropertyFetch = isEagerPropertyFetchEnabled(i);

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity( 
				key, 
				object, 
				persister, 
				lockMode, 
				!eagerPropertyFetch, 
				session 
			);

		//This is not very nice (and quite slow):
		final String[][] cols = persister == rootPersister ?
				getEntityAliases()[i].getSuffixedPropertyAliases() :
				getEntityAliases()[i].getSuffixedPropertyAliases(persister);

		final Object[] values = persister.hydrate( 
				rs, 
				id, 
				object, 
				rootPersister, 
				cols, 
				eagerPropertyFetch, 
				session 
			);

		final Object rowId = persister.hasRowId() ? rs.getObject(rowIdAlias) : null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if ( ownerAssociationTypes != null && ownerAssociationTypes[i] != null ) {
			String ukName = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName();
			if (ukName!=null) {
				final int index = ( (UniqueKeyLoadable) persister ).getPropertyIndex(ukName);
				final Type type = persister.getPropertyTypes()[index];
	
				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be
	
				EntityUniqueKey euk = new EntityUniqueKey( 
						rootPersister.getEntityName(), //polymorphism comment above
						ukName,
						type.semiResolve( values[index], session, object ),
						type,
						session.getEntityMode(), session.getFactory()
					);
				session.getPersistenceContext().addEntity( euk, object );
			}
		}

		TwoPhaseLoad.postHydrate( 
				persister, 
				id, 
				values, 
				rowId, 
				object, 
				lockMode, 
				!eagerPropertyFetch, 
				session 
			);

	}

	/**
	 * Determine the concrete class of an instance in the <tt>ResultSet</tt>
	 */
	private String getInstanceClass(
	        final ResultSet rs,
	        final int i,
	        final Loadable persister,
	        final Serializable id,
	        final SessionImplementor session) 
	throws HibernateException, SQLException {

		if ( persister.hasSubclasses() ) {

			// Code to handle subclasses of topClass
			Object discriminatorValue = persister.getDiscriminatorType().nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedDiscriminatorAlias(),
					session,
					null
				);

			final String result = persister.getSubclassForDiscriminatorValue( discriminatorValue );

			if ( result == null ) {
				//woops we got an instance of another class hierarchy branch
				throw new WrongClassException( 
						"Discriminator: " + discriminatorValue,
						id,
						persister.getEntityName() 
					);
			}

			return result;

		}
		else {
			return persister.getEntityName();
		}
	}

	/**
	 * Advance the cursor to the first required row of the <tt>ResultSet</tt>
	 */
	private void advance(final ResultSet rs, final RowSelection selection)
			throws SQLException {

		final int firstRow = getFirstRow( selection );
		if ( firstRow != 0 ) {
			if ( getFactory().getSettings().isScrollableResultSetsEnabled() ) {
				// we can go straight to the first required row
				rs.absolute( firstRow );
			}
			else {
				// we need to step through the rows one row at a time (slow)
				for ( int m = 0; m < firstRow; m++ ) rs.next();
			}
		}
	}

	private static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null;
	}

	private static int getFirstRow(RowSelection selection) {
		if ( selection == null || selection.getFirstRow() == null ) {
			return 0;
		}
		else {
			return selection.getFirstRow().intValue();
		}
	}

	private int interpretFirstRow(int zeroBasedFirstResult) {
		return getFactory().getDialect().convertToFirstRowValue( zeroBasedFirstResult );
	}

	/**
	 * Should we pre-process the SQL string, adding a dialect-specific
	 * LIMIT clause.
	 */
	private static boolean useLimit(final RowSelection selection, final Dialect dialect) {
		return dialect.supportsLimit() && hasMaxRows( selection );
	}

	/**
	 * Obtain a <tt>PreparedStatement</tt> with all parameters pre-bound.
	 * Bind JDBC-style <tt>?</tt> parameters, named parameters, and
	 * limit parameters.
	 */
	protected final PreparedStatement prepareQueryStatement(
	        final QueryParameters queryParameters,
	        final boolean scroll,
	        final SessionImplementor session) throws SQLException, HibernateException {

		queryParameters.processFilters( getSQLString(), session );
		String sql = queryParameters.getFilteredSQL();
		final Dialect dialect = getFactory().getDialect();
		final RowSelection selection = queryParameters.getRowSelection();
		boolean useLimit = useLimit( selection, dialect );
		boolean hasFirstRow = getFirstRow( selection ) > 0;
		boolean useOffset = hasFirstRow && useLimit && dialect.supportsLimitOffset();
		boolean callable = queryParameters.isCallable();
		
		boolean useScrollableResultSetToSkip = hasFirstRow &&
				!useOffset &&
				getFactory().getSettings().isScrollableResultSetsEnabled();
		ScrollMode scrollMode = scroll ? queryParameters.getScrollMode() : ScrollMode.SCROLL_INSENSITIVE;

		if ( useLimit ) {
			sql = dialect.getLimitString( 
					sql.trim(), //use of trim() here is ugly?
					useOffset ? getFirstRow(selection) : 0, 
					getMaxOrLimit(selection, dialect) 
				);
		}

		sql = preprocessSQL( sql, queryParameters, dialect );
		
		PreparedStatement st = null;
		
		if (callable) {
			st = session.getBatcher()
				.prepareCallableQueryStatement( sql, scroll || useScrollableResultSetToSkip, scrollMode );
		} 
		else {
			st = session.getBatcher()
				.prepareQueryStatement( sql, scroll || useScrollableResultSetToSkip, scrollMode );
		}
				

		try {

			int col = 1;
			//TODO: can we limit stored procedures ?!
			if ( useLimit && dialect.bindLimitParametersFirst() ) {
				col += bindLimitParameters( st, col, selection );
			}
			if (callable) {
				col = dialect.registerResultSetOutParameter( (CallableStatement)st, col );
			}

			col += bindParameterValues( st, queryParameters, col, session );

			if ( useLimit && !dialect.bindLimitParametersFirst() ) {
				col += bindLimitParameters( st, col, selection );
			}

			if ( !useLimit ) {
				setMaxRows( st, selection );
			}

			if ( selection != null ) {
				if ( selection.getTimeout() != null ) {
					st.setQueryTimeout( selection.getTimeout().intValue() );
				}
				if ( selection.getFetchSize() != null ) {
					st.setFetchSize( selection.getFetchSize().intValue() );
				}
			}

			// handle lock timeout...
			LockOptions lockOptions = queryParameters.getLockOptions();
			if ( lockOptions != null ) {
				if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
					if ( !dialect.supportsLockTimeouts() ) {
						log.debug(
								"Lock timeout [" + lockOptions.getTimeOut() +
										"] requested but dialect reported to not support lock timeouts"
						);
					}
					else if ( dialect.isLockTimeoutParameterized() ) {
						st.setInt( col++, lockOptions.getTimeOut() );
					}
				}
			}

			log.trace( "Bound [" + col + "] parameters total" );
		}
		catch ( SQLException sqle ) {
			session.getBatcher().closeQueryStatement( st, null );
			throw sqle;
		}
		catch ( HibernateException he ) {
			session.getBatcher().closeQueryStatement( st, null );
			throw he;
		}

		return st;
	}

	/**
	 * Some dialect-specific LIMIT clauses require the maximum last row number
	 * (aka, first_row_number + total_row_count), while others require the maximum
	 * returned row count (the total maximum number of rows to return).
	 *
	 * @param selection The selection criteria
	 * @param dialect The dialect
	 * @return The appropriate value to bind into the limit clause.
	 */
	private static int getMaxOrLimit(final RowSelection selection, final Dialect dialect) {
		final int firstRow = dialect.convertToFirstRowValue( getFirstRow( selection ) );
		final int lastRow = selection.getMaxRows().intValue();
		if ( dialect.useMaxForLimit() ) {
			return lastRow + firstRow;
		}
		else {
			return lastRow;
		}
	}

	/**
	 * Bind parameter values needed by the dialect-specific LIMIT clause.
	 *
	 * @param statement The statement to which to bind limit param values.
	 * @param index The bind position from which to start binding
	 * @param selection The selection object containing the limit information.
	 * @return The number of parameter values bound.
	 * @throws java.sql.SQLException Indicates problems binding parameter values.
	 */
	private int bindLimitParameters(
			final PreparedStatement statement,
			final int index,
			final RowSelection selection) throws SQLException {
		Dialect dialect = getFactory().getDialect();
		if ( !dialect.supportsVariableLimit() ) {
			return 0;
		}
		if ( !hasMaxRows( selection ) ) {
			throw new AssertionFailure( "no max results set" );
		}
		int firstRow = interpretFirstRow( getFirstRow( selection ) );
		int lastRow = getMaxOrLimit( selection, dialect );
		boolean hasFirstRow = dialect.supportsLimitOffset() && ( firstRow > 0 || dialect.forceLimitUsage() );
		boolean reverse = dialect.bindLimitParametersInReverseOrder();
		if ( hasFirstRow ) {
			statement.setInt( index + ( reverse ? 1 : 0 ), firstRow );
		}
		statement.setInt( index + ( reverse || !hasFirstRow ? 0 : 1 ), lastRow );
		return hasFirstRow ? 2 : 1;
	}

	/**
	 * Use JDBC API to limit the number of rows returned by the SQL query if necessary
	 */
	private void setMaxRows(
			final PreparedStatement st,
			final RowSelection selection) throws SQLException {
		if ( hasMaxRows( selection ) ) {
			st.setMaxRows( selection.getMaxRows().intValue() + interpretFirstRow( getFirstRow( selection ) ) );
		}
	}

	/**
	 * Bind all parameter values into the prepared statement in preparation
	 * for execution.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 */
	protected int bindParameterValues(
			PreparedStatement statement,
			QueryParameters queryParameters,
			int startIndex,
			SessionImplementor session) throws SQLException {
		int span = 0;
		span += bindPositionalParameters( statement, queryParameters, startIndex, session );
		span += bindNamedParameters( statement, queryParameters.getNamedParameters(), startIndex + span, session );
		return span;
	}

	/**
	 * Bind positional parameter values to the JDBC prepared statement.
	 * <p/>
	 * Positional parameters are those specified by JDBC-style ? parameters
	 * in the source query.  It is (currently) expected that these come
	 * before any named parameters in the source query.
	 *
	 * @param statement The JDBC prepared statement
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindPositionalParameters(
	        final PreparedStatement statement,
	        final QueryParameters queryParameters,
	        final int startIndex,
	        final SessionImplementor session) throws SQLException, HibernateException {
		final Object[] values = queryParameters.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters.getFilteredPositionalParameterTypes();
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( statement, values[i], startIndex + span, session );
			span += types[i].getColumnSpan( getFactory() );
		}
		return span;
	}

	/**
	 * Bind named parameters to the JDBC prepared statement.
	 * <p/>
	 * This is a generic implementation, the problem being that in the
	 * general case we do not know enough information about the named
	 * parameters to perform this in a complete manner here.  Thus this
	 * is generally overridden on subclasses allowing named parameters to
	 * apply the specific behavior.  The most usual limitation here is that
	 * we need to assume the type span is always one...
	 *
	 * @param statement The JDBC prepared statement
	 * @param namedParams A map of parameter names to values
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 * @throws SQLException Indicates problems performing the binding.
	 * @throws org.hibernate.HibernateException Indicates problems delegating binding to the types.
	 */
	protected int bindNamedParameters(
			final PreparedStatement statement,
			final Map namedParams,
			final int startIndex,
			final SessionImplementor session) throws SQLException, HibernateException {
		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = ( Map.Entry ) iter.next();
				String name = ( String ) e.getKey();
				TypedValue typedval = ( TypedValue ) e.getValue();
				int[] locs = getNamedParameterLocs( name );
				for ( int i = 0; i < locs.length; i++ ) {
					if ( log.isDebugEnabled() ) {
						log.debug(
								"bindNamedParameters() " +
								typedval.getValue() + " -> " + name +
								" [" + ( locs[i] + startIndex ) + "]"
							);
					}
					typedval.getType().nullSafeSet( statement, typedval.getValue(), locs[i] + startIndex, session );
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}

	public int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure("no named parameters");
	}

	/**
	 * Fetch a <tt>PreparedStatement</tt>, call <tt>setMaxRows</tt> and then execute it,
	 * advance to the first result and return an SQL <tt>ResultSet</tt>
	 */
	protected final ResultSet getResultSet(
	        final PreparedStatement st,
	        final boolean autodiscovertypes,
	        final boolean callable,
	        final RowSelection selection,
	        final SessionImplementor session) 
	throws SQLException, HibernateException {
	
		ResultSet rs = null;
		try {
			Dialect dialect = getFactory().getDialect();
			if (callable) {
				rs = session.getBatcher().getResultSet( (CallableStatement) st, dialect );
			} 
			else {
				rs = session.getBatcher().getResultSet( st );
			}
			rs = wrapResultSetIfEnabled( rs , session );
			
			if ( !dialect.supportsLimitOffset() || !useLimit( selection, dialect ) ) {
				advance( rs, selection );
			}
			
			if ( autodiscovertypes ) {
				autoDiscoverTypes( rs );
			}
			return rs;
		}
		catch ( SQLException sqle ) {
			session.getBatcher().closeQueryStatement( st, rs );
			throw sqle;
		}
	}

	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure("Auto discover types not supported in this loader");
		
	}

	private synchronized ResultSet wrapResultSetIfEnabled(final ResultSet rs, final SessionImplementor session) {
		// synchronized to avoid multi-thread access issues; defined as method synch to avoid
		// potential deadlock issues due to nature of code.
		if ( session.getFactory().getSettings().isWrapResultSetsEnabled() ) {
			try {
				log.debug("Wrapping result set [" + rs + "]");
				return session.getFactory()
						.getSettings()
						.getJdbcSupport().wrap( rs, retreiveColumnNameToIndexCache( rs ) );
			}
			catch(SQLException e) {
				log.info("Error wrapping result set", e);
				return rs;
			}
		}
		else {
			return rs;
		}
	}

	private ColumnNameCache retreiveColumnNameToIndexCache(ResultSet rs) throws SQLException {
		if ( columnNameCache == null ) {
			log.trace("Building columnName->columnIndex cache");
			columnNameCache = new ColumnNameCache( rs.getMetaData().getColumnCount() );
		}

		return columnNameCache;
	}

	/**
	 * Called by subclasses that load entities
	 * @param persister only needed for logging
	 * @param lockOptions
	 */
	protected final List loadEntity(
			final SessionImplementor session,
			final Object id,
			final Type identifierType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalIdentifier,
			final EntityPersister persister,
			LockOptions lockOptions) throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			log.debug( 
					"loading entity: " + 
					MessageHelper.infoString( persister, id, identifierType, getFactory() ) 
				);
		}

		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( new Type[] { identifierType } );
			qp.setPositionalParameterValues( new Object[] { id } );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( optionalEntityName );
			qp.setOptionalId( optionalIdentifier );
			qp.setLockOptions( lockOptions );
			result = doQueryAndInitializeNonLazyCollections( session, qp, false );
		}
		catch ( SQLException sqle ) {
			final Loadable[] persisters = getEntityPersisters();
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load an entity: " + 
			        MessageHelper.infoString( persisters[persisters.length-1], id, identifierType, getFactory() ),
			        getSQLString()
				);
		}

		log.debug("done entity load");
		
		return result;
		
	}

	/**
	 * Called by subclasses that load entities
	 * @param persister only needed for logging
	 */
	protected final List loadEntity(
	        final SessionImplementor session,
	        final Object key,
	        final Object index,
	        final Type keyType,
	        final Type indexType,
	        final EntityPersister persister) throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			log.debug( "loading collection element by index" );
		}

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections(
					session,
					new QueryParameters(
							new Type[] { keyType, indexType },
							new Object[] { key, index }
					),
					false
			);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not collection element by index",
			        getSQLString()
				);
		}

		log.debug("done entity load");
		
		return result;
		
	}

	/**
	 * Called by wrappers that batch load entities
	 * @param persister only needed for logging
	 * @param lockOptions
	 */
	public final List loadEntityBatch(
			final SessionImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			LockOptions lockOptions) throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"batch loading entity: " + 
					MessageHelper.infoString(persister, ids, getFactory() ) 
				);
		}

		Type[] types = new Type[ids.length];
		Arrays.fill( types, idType );
		List result;
		try {
			QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( types );
			qp.setPositionalParameterValues( ids );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( optionalEntityName );
			qp.setOptionalId( optionalId );
			qp.setLockOptions( lockOptions );
			result = doQueryAndInitializeNonLazyCollections( session, qp, false );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load an entity batch: " + 
			        MessageHelper.infoString( getEntityPersisters()[0], ids, getFactory() ),
			        getSQLString()
				);
		}

		log.debug("done entity batch load");
		
		return result;

	}

	/**
	 * Called by subclasses that initialize collections
	 */
	public final void loadCollection(
	        final SessionImplementor session,
	        final Serializable id,
	        final Type type) throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"loading collection: "+ 
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], id, getFactory() )
				);
		}

		Serializable[] ids = new Serializable[]{id};
		try {
			doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( new Type[]{type}, ids, ids ),
					true 
				);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					factory.getSQLExceptionConverter(),
					sqle,
					"could not initialize a collection: " + 
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], id, getFactory() ),
					getSQLString()
				);
		}
	
		log.debug("done loading collection");

	}

	/**
	 * Called by wrappers that batch initialize collections
	 */
	public final void loadCollectionBatch(
	        final SessionImplementor session,
	        final Serializable[] ids,
	        final Type type) throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"batch loading collection: "+ 
					MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() )
				);
		}

		Type[] idTypes = new Type[ids.length];
		Arrays.fill( idTypes, type );
		try {
			doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( idTypes, ids, ids ),
					true 
				);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not initialize a collection batch: " + 
			        MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() ),
			        getSQLString()
				);
		}
		
		log.debug("done batch load");

	}

	/**
	 * Called by subclasses that batch initialize collections
	 */
	protected final void loadCollectionSubselect(
	        final SessionImplementor session,
	        final Serializable[] ids,
	        final Object[] parameterValues,
	        final Type[] parameterTypes,
	        final Map namedParameters,
	        final Type type) throws HibernateException {

		Type[] idTypes = new Type[ids.length];
		Arrays.fill( idTypes, type );
		try {
			doQueryAndInitializeNonLazyCollections( session,
					new QueryParameters( parameterTypes, parameterValues, namedParameters, ids ),
					true 
				);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load collection by subselect: " + 
			        MessageHelper.collectionInfoString( getCollectionPersisters()[0], ids, getFactory() ),
			        getSQLString()
				);
		}
	}

	/**
	 * Return the query results, using the query cache, called
	 * by subclasses that implement cacheable queries
	 */
	protected List list(
	        final SessionImplementor session,
	        final QueryParameters queryParameters,
	        final Set querySpaces,
	        final Type[] resultTypes) throws HibernateException {

		final boolean cacheable = factory.getSettings().isQueryCacheEnabled() && 
			queryParameters.isCacheable();

		if ( cacheable ) {
			return listUsingQueryCache( session, queryParameters, querySpaces, resultTypes );
		}
		else {
			return listIgnoreQueryCache( session, queryParameters );
		}
	}

	private List listIgnoreQueryCache(SessionImplementor session, QueryParameters queryParameters) {
		return getResultList( doList( session, queryParameters ), queryParameters.getResultTransformer() );
	}

	private List listUsingQueryCache(
			final SessionImplementor session, 
			final QueryParameters queryParameters, 
			final Set querySpaces, 
			final Type[] resultTypes) {
	
		QueryCache queryCache = factory.getQueryCache( queryParameters.getCacheRegion() );
		
		Set filterKeys = FilterKey.createFilterKeys( 
				session.getLoadQueryInfluencers().getEnabledFilters(),
				session.getEntityMode() 
		);
		QueryKey key = QueryKey.generateQueryKey(
				getSQLString(), 
				queryParameters, 
				filterKeys, 
				session
		);
		
		List result = getResultFromQueryCache(
				session, 
				queryParameters, 
				querySpaces, 
				resultTypes, 
				queryCache, 
				key 
			);

		if ( result == null ) {
			result = doList( session, queryParameters );

			putResultInQueryCache( 
					session, 
					queryParameters, 
					resultTypes,
					queryCache, 
					key, 
					result 
			);
		}

		return getResultList( result, queryParameters.getResultTransformer() );
	}

	private List getResultFromQueryCache(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final Set querySpaces,
			final Type[] resultTypes,
			final QueryCache queryCache,
			final QueryKey key) {
		List result = null;

		if ( session.getCacheMode().isGetEnabled() ) {
			boolean isImmutableNaturalKeyLookup = queryParameters.isNaturalKeyLookup()
					&& getEntityPersisters()[0].getEntityMetamodel().hasImmutableNaturalId();

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
			if ( queryParameters.isReadOnlyInitialized() ) {
				// The read-only/modifiable mode for the query was explicitly set.
				// Temporarily set the default read-only/modifiable setting to the query's setting.
				persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
			}
			else {
				// The read-only/modifiable setting for the query was not initialized.
				// Use the default read-only/modifiable from the persistence context instead.
				queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
			}
			try {
				result = queryCache.get( key, resultTypes, isImmutableNaturalKeyLookup, querySpaces, session );
			}
			finally {
				persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
			}

			if ( factory.getStatistics().isStatisticsEnabled() ) {
				if ( result == null ) {
					factory.getStatisticsImplementor()
							.queryCacheMiss( getQueryIdentifier(), queryCache.getRegion().getName() );
				}
				else {
					factory.getStatisticsImplementor()
							.queryCacheHit( getQueryIdentifier(), queryCache.getRegion().getName() );
				}
			}
		}

		return result;
	}

	private void putResultInQueryCache(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final Type[] resultTypes,
			final QueryCache queryCache,
			final QueryKey key,
			final List result) {
		if ( session.getCacheMode().isPutEnabled() ) {
			boolean put = queryCache.put( key, resultTypes, result, queryParameters.isNaturalKeyLookup(), session );
			if ( put && factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor()
						.queryCachePut( getQueryIdentifier(), queryCache.getRegion().getName() );
			}
		}
	}

	/**
	 * Actually execute a query, ignoring the query cache
	 */
	protected List doList(final SessionImplementor session, final QueryParameters queryParameters)
			throws HibernateException {

		final boolean stats = getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections( session, queryParameters, true );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not execute query",
			        getSQLString()
				);
		}

		if ( stats ) {
			getFactory().getStatisticsImplementor().queryExecuted(
					getQueryIdentifier(),
					result.size(),
					System.currentTimeMillis() - startTime
				);
		}

		return result;
	}

	/**
	 * Check whether the current loader can support returning ScrollableResults.
	 *
	 * @throws HibernateException
	 */
	protected void checkScrollability() throws HibernateException {
		// Allows various loaders (ok mainly the QueryLoader :) to check
		// whether scrolling of their result set should be allowed.
		//
		// By default it is allowed.
		return;
	}

	/**
	 * Does the result set to be scrolled contain collection fetches?
	 *
	 * @return True if it does, and thus needs the special fetching scroll
	 * functionality; false otherwise.
	 */
	protected boolean needsFetchingScroll() {
		return false;
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 *
	 * @param queryParameters The parameters with which the query should be executed.
	 * @param returnTypes The expected return types of the query
	 * @param holderInstantiator If the return values are expected to be wrapped
	 * in a holder, this is the thing that knows how to wrap them.
	 * @param session The session from which the scroll request originated.
	 * @return The ScrollableResults instance.
	 * @throws HibernateException Indicates an error executing the query, or constructing
	 * the ScrollableResults.
	 */
	protected ScrollableResults scroll(
	        final QueryParameters queryParameters,
	        final Type[] returnTypes,
	        final HolderInstantiator holderInstantiator,
	        final SessionImplementor session) throws HibernateException {

		checkScrollability();

		final boolean stats = getQueryIdentifier() != null &&
				getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		try {

			PreparedStatement st = prepareQueryStatement( queryParameters, true, session );
			ResultSet rs = getResultSet(st, queryParameters.hasAutoDiscoverScalarTypes(), queryParameters.isCallable(), queryParameters.getRowSelection(), session);

			if ( stats ) {
				getFactory().getStatisticsImplementor().queryExecuted(
						getQueryIdentifier(),
						0,
						System.currentTimeMillis() - startTime
					);
			}

			if ( needsFetchingScroll() ) {
				return new FetchingScrollableResultsImpl(
						rs,
						st,
						session,
						this,
						queryParameters,
						returnTypes,
						holderInstantiator
					);
			}
			else {
				return new ScrollableResultsImpl(
						rs,
						st,
						session,
						this,
						queryParameters,
						returnTypes,
						holderInstantiator
					);
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not execute query using scroll",
			        getSQLString()
				);
		}

	}

	/**
	 * Calculate and cache select-clause suffixes. Must be
	 * called by subclasses after instantiation.
	 */
	protected void postInstantiate() {}

	/**
	 * Get the result set descriptor
	 */
	protected abstract EntityAliases[] getEntityAliases();

	protected abstract CollectionAliases[] getCollectionAliases();

	/**
	 * Identifies the query for statistics reporting, if null,
	 * no statistics will be reported
	 */
	protected String getQueryIdentifier() {
		return null;
	}

	public final SessionFactoryImplementor getFactory() {
		return factory;
	}

	public String toString() {
		return getClass().getName() + '(' + getSQLString() + ')';
	}

}
