/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.hql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.HolderInstantiator;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl;
import org.hibernate.hql.internal.ast.tree.AggregatedSelectExpression;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.QueryNode;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.internal.IteratorImpl;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A delegate that implements the Loader part of QueryTranslator.
 *
 * @author josh
 */
public class QueryLoader extends BasicLoader {

	/**
	 * The query translator that is delegating to this object.
	 */
	private QueryTranslatorImpl queryTranslator;

	private Queryable[] entityPersisters;
	private String[] entityAliases;
	private String[] sqlAliases;
	private String[] sqlAliasSuffixes;
	private boolean[] includeInSelect;

	private String[] collectionSuffixes;

	private boolean hasScalars;
	private String[][] scalarColumnNames;
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;

	private final Map<String, String> sqlAliasByEntityAlias = new HashMap<String, String>( 8 );

	private EntityType[] ownerAssociationTypes;
	private int[] owners;
	private boolean[] entityEagerPropertyFetches;

	private int[] collectionOwners;
	private QueryableCollection[] collectionPersisters;

	private int selectLength;

	private AggregatedSelectExpression aggregatedSelectExpression;
	private String[] queryReturnAliases;

	private LockMode[] defaultLockModes;


	/**
	 * Creates a new Loader implementation.
	 *
	 * @param queryTranslator The query translator that is the delegator.
	 * @param factory The factory from which this loader is being created.
	 * @param selectClause The AST representing the select clause for loading.
	 */
	public QueryLoader(
			final QueryTranslatorImpl queryTranslator,
			final SessionFactoryImplementor factory,
			final SelectClause selectClause) {
		super( factory );
		this.queryTranslator = queryTranslator;
		initialize( selectClause );
		postInstantiate();
	}

	private void initialize(SelectClause selectClause) {

		List fromElementList = selectClause.getFromElementsForLoad();

		hasScalars = selectClause.isScalarSelect();
		scalarColumnNames = selectClause.getColumnNames();
		//sqlResultTypes = selectClause.getSqlResultTypes();
		queryReturnTypes = selectClause.getQueryReturnTypes();

		aggregatedSelectExpression = selectClause.getAggregatedSelectExpression();
		queryReturnAliases = selectClause.getQueryReturnAliases();

		List collectionFromElements = selectClause.getCollectionFromElements();
		if ( collectionFromElements != null && collectionFromElements.size() != 0 ) {
			int length = collectionFromElements.size();
			collectionPersisters = new QueryableCollection[length];
			collectionOwners = new int[length];
			collectionSuffixes = new String[length];
			for ( int i = 0; i < length; i++ ) {
				FromElement collectionFromElement = (FromElement) collectionFromElements.get( i );
				collectionPersisters[i] = collectionFromElement.getQueryableCollection();
				collectionOwners[i] = fromElementList.indexOf( collectionFromElement.getOrigin() );
//				collectionSuffixes[i] = collectionFromElement.getColumnAliasSuffix();
//				collectionSuffixes[i] = Integer.toString( i ) + "_";
				collectionSuffixes[i] = collectionFromElement.getCollectionSuffix();
			}
		}

		int size = fromElementList.size();
		entityPersisters = new Queryable[size];
		entityEagerPropertyFetches = new boolean[size];
		entityAliases = new String[size];
		sqlAliases = new String[size];
		sqlAliasSuffixes = new String[size];
		includeInSelect = new boolean[size];
		owners = new int[size];
		ownerAssociationTypes = new EntityType[size];

		for ( int i = 0; i < size; i++ ) {
			final FromElement element = (FromElement) fromElementList.get( i );
			entityPersisters[i] = (Queryable) element.getEntityPersister();

			if ( entityPersisters[i] == null ) {
				throw new IllegalStateException( "No entity persister for " + element.toString() );
			}

			entityEagerPropertyFetches[i] = element.isAllPropertyFetch();
			sqlAliases[i] = element.getTableAlias();
			entityAliases[i] = element.getClassAlias();
			sqlAliasByEntityAlias.put( entityAliases[i], sqlAliases[i] );
			// TODO should we just collect these like with the collections above?
			sqlAliasSuffixes[i] = ( size == 1 ) ? "" : Integer.toString( i ) + "_";
//			sqlAliasSuffixes[i] = element.getColumnAliasSuffix();
			includeInSelect[i] = !element.isFetch();
			if ( includeInSelect[i] ) {
				selectLength++;
			}

			owners[i] = -1; //by default
			if ( element.isFetch() ) {
				if ( element.isCollectionJoin() || element.getQueryableCollection() != null ) {
					// This is now handled earlier in this method.
				}
				else if ( element.getDataType().isEntityType() ) {
					EntityType entityType = (EntityType) element.getDataType();
					if ( entityType.isOneToOne() ) {
						owners[i] = fromElementList.indexOf( element.getOrigin() );
					}
					ownerAssociationTypes[i] = entityType;
				}
			}
		}

		//NONE, because its the requested lock mode, not the actual! 
		defaultLockModes = ArrayHelper.fillArray( LockMode.NONE, size );
	}

	public AggregatedSelectExpression getAggregatedSelectExpression() {
		return aggregatedSelectExpression;
	}


	// -- Loader implementation --

	public final void validateScrollability() throws HibernateException {
		queryTranslator.validateScrollability();
	}

	@Override
	protected boolean needsFetchingScroll() {
		return queryTranslator.containsCollectionFetches();
	}

	@Override
	public Loadable[] getEntityPersisters() {
		return entityPersisters;
	}

	@Override
	public String[] getAliases() {
		return sqlAliases;
	}

	public String[] getSqlAliasSuffixes() {
		return sqlAliasSuffixes;
	}

	@Override
	public String[] getSuffixes() {
		return getSqlAliasSuffixes();
	}

	@Override
	public String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	@Override
	protected String getQueryIdentifier() {
		return queryTranslator.getQueryIdentifier();
	}

	/**
	 * The SQL query string to be called.
	 */
	@Override
	public String getSQLString() {
		return queryTranslator.getSQLString();
	}

	/**
	 * An (optional) persister for a collection to be initialized; only collection loaders
	 * return a non-null value
	 */
	@Override
	protected CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	@Override
	protected int[] getCollectionOwners() {
		return collectionOwners;
	}

	@Override
	protected boolean[] getEntityEagerPropertyFetches() {
		return entityEagerPropertyFetches;
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner")
	 */
	@Override
	protected int[] getOwners() {
		return owners;
	}

	@Override
	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	// -- Loader overrides --
	@Override
	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}

	/**
	 * @param lockOptions a collection of lock modes specified dynamically via the Query interface
	 */
	@Override
	protected LockMode[] getLockModes(LockOptions lockOptions) {
		if ( lockOptions == null ) {
			return defaultLockModes;
		}

		if ( lockOptions.getAliasLockCount() == 0
				&& ( lockOptions.getLockMode() == null || LockMode.NONE.equals( lockOptions.getLockMode() ) ) ) {
			return defaultLockModes;
		}

		// unfortunately this stuff can't be cached because
		// it is per-invocation, not constant for the
		// QueryTranslator instance

		LockMode[] lockModesArray = new LockMode[entityAliases.length];
		for ( int i = 0; i < entityAliases.length; i++ ) {
			LockMode lockMode = lockOptions.getEffectiveLockMode( entityAliases[i] );
			if ( lockMode == null ) {
				//NONE, because its the requested lock mode, not the actual!
				lockMode = LockMode.NONE;
			}
			lockModesArray[i] = lockMode;
		}

		return lockModesArray;
	}

	@Override
	protected String applyLocks(
			String sql,
			QueryParameters parameters,
			Dialect dialect,
			List<AfterLoadAction> afterLoadActions) throws QueryException {
		// can't cache this stuff either (per-invocation)
		// we are given a map of user-alias -> lock mode
		// create a new map of sql-alias -> lock mode

		final LockOptions lockOptions = parameters.getLockOptions();

		if ( lockOptions == null ||
				( lockOptions.getLockMode() == LockMode.NONE && lockOptions.getAliasLockCount() == 0 ) ) {
			return sql;
		}


		// user is request locking, lets see if we can apply locking directly to the SQL...

		// 		some dialects wont allow locking with paging...
		if ( shouldUseFollowOnLocking( parameters, dialect, afterLoadActions ) ) {
			return sql;
		}

		//		there are other conditions we might want to add here, such as checking the result types etc
		//		but those are better served after we have redone the SQL generation to use ASTs.


		// we need both the set of locks and the columns to reference in locks
		// as the ultimate output of this section...
		final LockOptions locks = new LockOptions( lockOptions.getLockMode() );
		final Map<String, String[]> keyColumnNames = dialect.forUpdateOfColumns() ?
				new HashMap<String, String[]>() :
				null;

		locks.setScope( lockOptions.getScope() );
		locks.setTimeOut( lockOptions.getTimeOut() );

		for ( Map.Entry<String, String> entry : sqlAliasByEntityAlias.entrySet() ) {
			final String userAlias = entry.getKey();
			final String drivingSqlAlias = entry.getValue();
			if ( drivingSqlAlias == null ) {
				throw new IllegalArgumentException( "could not locate alias to apply lock mode : " + userAlias );
			}
			// at this point we have (drivingSqlAlias) the SQL alias of the driving table
			// corresponding to the given user alias.  However, the driving table is not
			// (necessarily) the table against which we want to apply locks.  Mainly,
			// the exception case here is joined-subclass hierarchies where we instead
			// want to apply the lock against the root table (for all other strategies,
			// it just happens that driving and root are the same).
			final QueryNode select = (QueryNode) queryTranslator.getSqlAST();
			final Lockable drivingPersister = (Lockable) select.getFromClause()
					.findFromElementByUserOrSqlAlias( userAlias, drivingSqlAlias )
					.getQueryable();
			final String sqlAlias = drivingPersister.getRootTableAlias( drivingSqlAlias );

			final LockMode effectiveLockMode = lockOptions.getEffectiveLockMode( userAlias );
			locks.setAliasSpecificLockMode( sqlAlias, effectiveLockMode );

			if ( keyColumnNames != null ) {
				keyColumnNames.put( sqlAlias, drivingPersister.getRootTableIdentifierColumnNames() );
			}
		}

		// apply the collected locks and columns
		return dialect.applyLocksToSql( sql, locks, keyColumnNames );
	}

	@Override
	protected void applyPostLoadLocks(Object[] row, LockMode[] lockModesArray, SessionImplementor session) {
		// todo : scalars???
//		if ( row.length != lockModesArray.length ) {
//			return;
//		}
//
//		for ( int i = 0; i < lockModesArray.length; i++ ) {
//			if ( LockMode.OPTIMISTIC_FORCE_INCREMENT.equals( lockModesArray[i] ) ) {
//				final EntityEntry pcEntry =
//			}
//			else if ( LockMode.PESSIMISTIC_FORCE_INCREMENT.equals( lockModesArray[i] ) ) {
//
//			}
//		}
	}

	@Override
	protected boolean upgradeLocks() {
		return true;
	}

	private boolean hasSelectNew() {
		return aggregatedSelectExpression != null && aggregatedSelectExpression.getResultTransformer() != null;
	}

	@Override
	protected String[] getResultRowAliases() {
		return queryReturnAliases;
	}

	@Override
	protected ResultTransformer resolveResultTransformer(ResultTransformer resultTransformer) {
		final ResultTransformer implicitResultTransformer = aggregatedSelectExpression == null
				? null
				: aggregatedSelectExpression.getResultTransformer();
		return HolderInstantiator.resolveResultTransformer( implicitResultTransformer, resultTransformer );
	}

	@Override
	protected boolean[] includeInResultRow() {
		boolean[] includeInResultTuple = includeInSelect;
		if ( hasScalars ) {
			includeInResultTuple = new boolean[queryReturnTypes.length];
			Arrays.fill( includeInResultTuple, true );
		}
		return includeInResultTuple;
	}

	@Override
	protected Object getResultColumnOrRow(
			Object[] row,
			ResultTransformer transformer,
			ResultSet rs,
			SessionImplementor session)
			throws SQLException, HibernateException {

		Object[] resultRow = getResultRow( row, rs, session );
		boolean hasTransform = hasSelectNew() || transformer != null;
		return ( !hasTransform && resultRow.length == 1 ?
				resultRow[0] :
				resultRow
		);
	}

	@Override
	protected Object[] getResultRow(Object[] row, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		Object[] resultRow;
		if ( hasScalars ) {
			String[][] scalarColumns = scalarColumnNames;
			int queryCols = queryReturnTypes.length;
			resultRow = new Object[queryCols];
			for ( int i = 0; i < queryCols; i++ ) {
				resultRow[i] = queryReturnTypes[i].nullSafeGet( rs, scalarColumns[i], session, null );
			}
		}
		else {
			resultRow = toResultRow( row );
		}
		return resultRow;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List getResultList(List results, ResultTransformer resultTransformer) throws QueryException {
		// meant to handle dynamic instantiation queries...
		HolderInstantiator holderInstantiator = buildHolderInstantiator( resultTransformer );
		if ( holderInstantiator.isRequired() ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = (Object[]) results.get( i );
				Object result = holderInstantiator.instantiate( row );
				results.set( i, result );
			}

			if ( !hasSelectNew() && resultTransformer != null ) {
				return resultTransformer.transformList( results );
			}
			else {
				return results;
			}
		}
		else {
			return results;
		}
	}

	private HolderInstantiator buildHolderInstantiator(ResultTransformer queryLocalResultTransformer) {
		final ResultTransformer implicitResultTransformer = aggregatedSelectExpression == null
				? null
				: aggregatedSelectExpression.getResultTransformer();
		return HolderInstantiator.getHolderInstantiator(
				implicitResultTransformer,
				queryLocalResultTransformer,
				queryReturnAliases
		);
	}
	// --- Query translator methods ---

	public List list(
			SessionImplementor session,
			QueryParameters queryParameters) throws HibernateException {
		checkQuery( queryParameters );
		return list( session, queryParameters, queryTranslator.getQuerySpaces(), queryReturnTypes );
	}

	private void checkQuery(QueryParameters queryParameters) {
		if ( hasSelectNew() && queryParameters.getResultTransformer() != null ) {
			throw new QueryException( "ResultTransformer is not allowed for 'select new' queries." );
		}
	}

	public Iterator iterate(
			QueryParameters queryParameters,
			EventSource session) throws HibernateException {
		checkQuery( queryParameters );
		final boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) {
			startTime = System.nanoTime();
		}

		try {
			if ( queryParameters.isCallable() ) {
				throw new QueryException( "iterate() not supported for callable statements" );
			}
			final SqlStatementWrapper wrapper = executeQueryStatement(
					queryParameters,
					false,
					Collections.<AfterLoadAction>emptyList(),
					session
			);
			final ResultSet rs = wrapper.getResultSet();
			final PreparedStatement st = (PreparedStatement) wrapper.getStatement();
			final Iterator result = new IteratorImpl(
					rs,
					st,
					session,
					queryParameters.isReadOnly( session ),
					queryReturnTypes,
					queryTranslator.getColumnNames(),
					buildHolderInstantiator( queryParameters.getResultTransformer() )
			);

			if ( stats ) {
				final long endTime = System.nanoTime();
				final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
				session.getFactory().getStatisticsImplementor().queryExecuted(
//						"HQL: " + queryTranslator.getQueryString(),
						getQueryIdentifier(),
						0,
						milliseconds
				);
			}

			return result;

		}
		catch (SQLException sqle) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not execute query using iterate",
					getSQLString()
			);
		}

	}

	public ScrollableResults scroll(
			final QueryParameters queryParameters,
			final SessionImplementor session) throws HibernateException {
		checkQuery( queryParameters );
		return scroll(
				queryParameters,
				queryReturnTypes,
				buildHolderInstantiator( queryParameters.getResultTransformer() ),
				session
		);
	}

	// -- Implementation private methods --

	private Object[] toResultRow(Object[] row) {
		if ( selectLength == row.length ) {
			return row;
		}
		else {
			Object[] result = new Object[selectLength];
			int j = 0;
			for ( int i = 0; i < row.length; i++ ) {
				if ( includeInSelect[i] ) {
					result[j++] = row[i];
				}
			}
			return result;
		}
	}

	/**
	 * Returns the locations of all occurrences of the named parameter.
	 */
	@Override
	public int[] getNamedParameterLocs(String name) throws QueryException {
		return queryTranslator.getParameterTranslations().getNamedParameterSqlLocations( name );
	}

	/**
	 * We specifically override this method here, because in general we know much more
	 * about the parameters and their appropriate bind positions here then we do in
	 * our super because we track them explicitly here through the ParameterSpecification
	 * interface.
	 *
	 * @param queryParameters The encapsulation of the parameter values to be bound.
	 * @param startIndex The position from which to start binding parameter values.
	 * @param session The originating session.
	 *
	 * @return The number of JDBC bind positions actually bound during this method execution.
	 *
	 * @throws SQLException Indicates problems performing the binding.
	 */
	@Override
	protected int bindParameterValues(
			final PreparedStatement statement,
			final QueryParameters queryParameters,
			final int startIndex,
			final SessionImplementor session) throws SQLException {
		int position = startIndex;
		List<ParameterSpecification> parameterSpecs = queryTranslator.getCollectedParameterSpecifications();
		for ( ParameterSpecification spec : parameterSpecs ) {
			position += spec.bind( statement, queryParameters, session, position );
		}
		return position - startIndex;
	}
}
