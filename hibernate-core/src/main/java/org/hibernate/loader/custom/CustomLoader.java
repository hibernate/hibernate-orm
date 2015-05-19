/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.custom;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.Loader;
import org.hibernate.loader.spi.AfterLoadAction;
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
	private final Set<Serializable> querySpaces = new HashSet<Serializable>();
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

		List<Queryable> entityPersisters = new ArrayList<Queryable>();
		List<Integer> entityOwners = new ArrayList<Integer>();
		List<EntityAliases> entityAliases = new ArrayList<EntityAliases>();

		List<QueryableCollection> collectionPersisters = new ArrayList<QueryableCollection>();
		List<Integer> collectionOwners = new ArrayList<Integer>();
		List<CollectionAliases> collectionAliases = new ArrayList<CollectionAliases>();

		List<LockMode> lockModes = new ArrayList<LockMode>();
		List<ResultColumnProcessor> resultColumnProcessors = new ArrayList<ResultColumnProcessor>();
		List<Return> nonScalarReturnList = new ArrayList<Return>();
		List<Type> resultTypes = new ArrayList<Type>();
		List<String> specifiedAliases = new ArrayList<String>();

		int returnableCounter = 0;
		boolean hasScalars = false;

		List<Boolean> includeInResultRowList = new ArrayList<Boolean>();

		for ( Return rtn : customQuery.getCustomQueryReturns() ) {
			if ( rtn instanceof ScalarReturn ) {
				ScalarReturn scalarRtn = (ScalarReturn) rtn;
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
			else if ( ConstructorReturn.class.isInstance( rtn ) ) {
				final ConstructorReturn constructorReturn = (ConstructorReturn) rtn;
				resultTypes.add( null ); // this bit makes me nervous
				includeInResultRowList.add( true );
				hasScalars = true;

				ScalarResultColumnProcessor[] scalarProcessors = new ScalarResultColumnProcessor[constructorReturn.getScalars().length];
				int i = 0;
				for ( ScalarReturn scalarReturn : constructorReturn.getScalars() ) {
					scalarProcessors[i++] = new ScalarResultColumnProcessor(
							StringHelper.unquote( scalarReturn.getColumnAlias(), factory.getDialect() ),
							scalarReturn.getType()
					);
				}

				resultColumnProcessors.add(
						new ConstructorResultColumnProcessor( constructorReturn.getTargetClass(), scalarProcessors )
				);
			}
			else if ( rtn instanceof RootReturn ) {
				RootReturn rootRtn = (RootReturn) rtn;
				Queryable persister = (Queryable) factory.getEntityPersister( rootRtn.getEntityName() );
				entityPersisters.add( persister );
				lockModes.add( ( rootRtn.getLockMode() ) );
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
				CollectionReturn collRtn = (CollectionReturn) rtn;
				String role = collRtn.getOwnerEntityName() + "." + collRtn.getOwnerProperty();
				QueryableCollection persister = (QueryableCollection) factory.getCollectionPersister( role );
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
					Queryable elementPersister = (Queryable) ( (EntityType) elementType ).getAssociatedJoinable( factory );
					entityPersisters.add( elementPersister );
					entityOwners.add( -1 );
					entityAliases.add( collRtn.getElementEntityAliases() );
					ArrayHelper.addAll( querySpaces, elementPersister.getQuerySpaces() );
				}
				includeInResultRowList.add( true );
			}
			else if ( rtn instanceof EntityFetchReturn ) {
				EntityFetchReturn fetchRtn = (EntityFetchReturn) rtn;
				NonScalarReturn ownerDescriptor = fetchRtn.getOwner();
				int ownerIndex = nonScalarReturnList.indexOf( ownerDescriptor );
				entityOwners.add( ownerIndex );
				lockModes.add( fetchRtn.getLockMode() );
				Queryable ownerPersister = determineAppropriateOwnerPersister( ownerDescriptor );
				EntityType fetchedType = (EntityType) ownerPersister.getPropertyType( fetchRtn.getOwnerProperty() );
				String entityName = fetchedType.getAssociatedEntityName( getFactory() );
				Queryable persister = (Queryable) factory.getEntityPersister( entityName );
				entityPersisters.add( persister );
				nonScalarReturnList.add( rtn );
				specifiedAliases.add( fetchRtn.getAlias() );
				entityAliases.add( fetchRtn.getEntityAliases() );
				ArrayHelper.addAll( querySpaces, persister.getQuerySpaces() );
				includeInResultRowList.add( false );
			}
			else if ( rtn instanceof CollectionFetchReturn ) {
				CollectionFetchReturn fetchRtn = (CollectionFetchReturn) rtn;
				NonScalarReturn ownerDescriptor = fetchRtn.getOwner();
				int ownerIndex = nonScalarReturnList.indexOf( ownerDescriptor );
				collectionOwners.add( ownerIndex );
				lockModes.add( fetchRtn.getLockMode() );
				Queryable ownerPersister = determineAppropriateOwnerPersister( ownerDescriptor );
				String role = ownerPersister.getEntityName() + '.' + fetchRtn.getOwnerProperty();
				QueryableCollection persister = (QueryableCollection) factory.getCollectionPersister( role );
				collectionPersisters.add( persister );
				nonScalarReturnList.add( rtn );
				specifiedAliases.add( fetchRtn.getAlias() );
				collectionAliases.add( fetchRtn.getCollectionAliases() );
				// determine if the collection elements are entities...
				Type elementType = persister.getElementType();
				if ( elementType.isEntityType() ) {
					Queryable elementPersister = (Queryable) ( (EntityType) elementType ).getAssociatedJoinable( factory );
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

		this.entityPersisters = new Queryable[entityPersisters.size()];
		for ( int i = 0; i < entityPersisters.size(); i++ ) {
			this.entityPersisters[i] = entityPersisters.get( i );
		}
		this.entiytOwners = ArrayHelper.toIntArray( entityOwners );
		this.entityAliases = new EntityAliases[entityAliases.size()];
		for ( int i = 0; i < entityAliases.size(); i++ ) {
			this.entityAliases[i] = entityAliases.get( i );
		}

		this.collectionPersisters = new QueryableCollection[collectionPersisters.size()];
		for ( int i = 0; i < collectionPersisters.size(); i++ ) {
			this.collectionPersisters[i] = collectionPersisters.get( i );
		}
		this.collectionOwners = ArrayHelper.toIntArray( collectionOwners );
		this.collectionAliases = new CollectionAliases[collectionAliases.size()];
		for ( int i = 0; i < collectionAliases.size(); i++ ) {
			this.collectionAliases[i] = collectionAliases.get( i );
		}

		this.lockModes = new LockMode[lockModes.size()];
		for ( int i = 0; i < lockModes.size(); i++ ) {
			this.lockModes[i] = lockModes.get( i );
		}

		this.resultTypes = ArrayHelper.toTypeArray( resultTypes );
		this.transformerAliases = ArrayHelper.toStringArray( specifiedAliases );

		this.rowProcessor = new ResultRowProcessor(
				hasScalars,
				resultColumnProcessors.toArray( new ResultColumnProcessor[resultColumnProcessors.size()] )
		);

		this.includeInResultRow = ArrayHelper.toBooleanArray( includeInResultRowList );
	}

	private Queryable determineAppropriateOwnerPersister(NonScalarReturn ownerDescriptor) {
		String entityName = null;
		if ( ownerDescriptor instanceof RootReturn ) {
			entityName = ( (RootReturn) ownerDescriptor ).getEntityName();
		}
		else if ( ownerDescriptor instanceof CollectionReturn ) {
			CollectionReturn collRtn = (CollectionReturn) ownerDescriptor;
			String role = collRtn.getOwnerEntityName() + "." + collRtn.getOwnerProperty();
			CollectionPersister persister = getFactory().getCollectionPersister( role );
			EntityType ownerType = (EntityType) persister.getElementType();
			entityName = ownerType.getAssociatedEntityName( getFactory() );
		}
		else if ( ownerDescriptor instanceof FetchReturn ) {
			FetchReturn fetchRtn = (FetchReturn) ownerDescriptor;
			Queryable persister = determineAppropriateOwnerPersister( fetchRtn.getOwner() );
			Type ownerType = persister.getPropertyType( fetchRtn.getOwnerProperty() );
			if ( ownerType.isEntityType() ) {
				entityName = ( (EntityType) ownerType ).getAssociatedEntityName( getFactory() );
			}
			else if ( ownerType.isCollectionType() ) {
				Type ownerCollectionElementType = ( (CollectionType) ownerType ).getElementType( getFactory() );
				if ( ownerCollectionElementType.isEntityType() ) {
					entityName = ( (EntityType) ownerCollectionElementType ).getAssociatedEntityName( getFactory() );
				}
			}
		}

		if ( entityName == null ) {
			throw new HibernateException( "Could not determine fetch owner : " + ownerDescriptor );
		}

		return (Queryable) getFactory().getEntityPersister( entityName );
	}

	@Override
	protected String getQueryIdentifier() {
		return sql;
	}

	@Override
	public String getSQLString() {
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

	@Override
	protected String applyLocks(
			String sql,
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) throws QueryException {
		final LockOptions lockOptions = parameters.getLockOptions();
		if ( lockOptions == null ||
				( lockOptions.getLockMode() == LockMode.NONE && lockOptions.getAliasLockCount() == 0 ) ) {
			return sql;
		}

		// user is request locking, lets see if we can apply locking directly to the SQL...

		// 		some dialects wont allow locking with paging...
		afterLoadActions.add(
				new AfterLoadAction() {
					private final LockOptions originalLockOptions = lockOptions.makeCopy();

					@Override
					public void afterLoad(SessionImplementor session, Object entity, Loadable persister) {
						( (Session) session ).buildLockRequest( originalLockOptions ).lock(
								persister.getEntityName(),
								entity
						);
					}
				}
		);
		parameters.getLockOptions().setLockMode( LockMode.READ );

		return sql;
	}

	public ScrollableResults scroll(final QueryParameters queryParameters, final SessionImplementor session)
			throws HibernateException {
		return scroll(
				queryParameters,
				resultTypes,
				getHolderInstantiator( queryParameters.getResultTransformer(), getReturnAliasesForTransformer() ),
				session
		);
	}

	static private HolderInstantiator getHolderInstantiator(
			ResultTransformer resultTransformer,
			String[] queryReturnAliases) {
		if ( resultTransformer == null ) {
			return HolderInstantiator.NOOP_INSTANTIATOR;
		}
		else {
			return new HolderInstantiator( resultTransformer, queryReturnAliases );
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
	@SuppressWarnings("unchecked")
	protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		// meant to handle dynamic instantiation queries...(Copy from QueryLoader)
		HolderInstantiator holderInstantiator = HolderInstantiator.getHolderInstantiator(
				null,
				resultTransformer,
				getReturnAliasesForTransformer()
		);
		if ( holderInstantiator.isRequired() ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = (Object[]) results.get( i );
				Object result = holderInstantiator.instantiate( row );
				results.set( i, result );
			}

			return resultTransformer.transformList( results );
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
			return new int[] {(Integer) loc};
		}
		else {
			return ArrayHelper.toIntArray( (List) loc );
		}
	}


	@Override
	protected void autoDiscoverTypes(ResultSet rs) {
		try {
			JdbcResultMetadata metadata = new JdbcResultMetadata( getFactory(), rs );
			rowProcessor.prepareForAutoDiscovery( metadata );

			List<String> aliases = new ArrayList<String>();
			List<Type> types = new ArrayList<Type>();
			for ( ResultColumnProcessor resultProcessor : rowProcessor.getColumnProcessors() ) {
				resultProcessor.performDiscovery( metadata, types, aliases );
			}

			validateAliases( aliases );

			resultTypes = ArrayHelper.toTypeArray( types );
			transformerAliases = ArrayHelper.toStringArray( aliases );
		}
		catch (SQLException e) {
			throw new HibernateException( "Exception while trying to autodiscover types.", e );
		}
	}

	private void validateAliases(List<String> aliases) {
		// lets make sure we did not end up with duplicate aliases.  this can occur when the user supplied query
		// did not rename same-named columns.  e.g.:
		//		select u.username, u2.username from t_user u, t_user u2 ...
		//
		// the above will lead to an unworkable situation in most cases (the difference is how the driver/db
		// handle this situation.  But if the 'aliases' variable contains duplicate names, then we have that
		// troublesome condition, so lets throw an error.  See HHH-5992
		final HashSet<String> aliasesSet = new HashSet<String>();
		for ( String alias : aliases ) {
			validateAlias( alias );
			boolean alreadyExisted = !aliasesSet.add( alias );
			if ( alreadyExisted ) {
				throw new NonUniqueDiscoveredSqlAliasException(
						"Encountered a duplicated sql alias [" + alias + "] during auto-discovery of a native-sql query"
				);
			}
		}
	}

	@SuppressWarnings("UnusedParameters")
	protected void validateAlias(String alias) {
	}

	/**
	 * {@link #resultTypes} can be overridden by {@link #autoDiscoverTypes(ResultSet)},
	 * *after* {@link #list(SessionImplementor, QueryParameters)} has already been called.  It's a bit of a
	 * chicken-and-the-egg issue since {@link #autoDiscoverTypes(ResultSet)} needs the {@link ResultSet}.
	 * <p/>
	 * As a hacky workaround, override
	 * {@link #putResultInQueryCache(SessionImplementor, QueryParameters, Type[], QueryCache, QueryKey, List)} here
	 * and provide the {@link #resultTypes}.
	 *
	 * see HHH-3051
	 */
	@Override
	protected void putResultInQueryCache(
			final SessionImplementor session,
			final QueryParameters queryParameters,
			final Type[] resultTypes,
			final QueryCache queryCache,
			final QueryKey key,
			final List result) {
		super.putResultInQueryCache( session, queryParameters, this.resultTypes, queryCache, key, result );
	}

}
