/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * @author Gavin King
 */
public final class QueryParameters {
	private static final Logger LOG = CoreLogging.logger( QueryParameters.class );

	/**
	 * Symbols used to split SQL string into tokens in {@link #processFilters(String, Map, SessionFactoryImplementor)}.
	 */
	private static final String SYMBOLS = ParserHelper.HQL_SEPARATORS.replace( "'", "" );

	private Type[] positionalParameterTypes;
	private Object[] positionalParameterValues;
	private Map<String,TypedValue> namedParameters;

	private LockOptions lockOptions;
	private RowSelection rowSelection;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private List<String> queryHints;
	private ScrollMode scrollMode;
	private Serializable[] collectionKeys;
	private Object optionalObject;
	private String optionalEntityName;
	private Serializable optionalId;
	private boolean isReadOnlyInitialized;
	private boolean readOnly;
	private boolean callable;
	private boolean autodiscovertypes;
	private boolean isNaturalKeyLookup;
	private boolean passDistinctThrough = true;

	private final ResultTransformer resultTransformer; // why is all others non final ?

	private String processedSQL;
	private Type[] processedPositionalParameterTypes;
	private Object[] processedPositionalParameterValues;

	private HQLQueryPlan queryPlan;

	public QueryParameters() {
		this( ArrayHelper.EMPTY_TYPE_ARRAY, ArrayHelper.EMPTY_OBJECT_ARRAY );
	}

	public QueryParameters(Type type, Object value) {
		this( new Type[] { type }, new Object[] { value } );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalObjectId) {
		this( positionalParameterTypes, positionalParameterValues );
		this.optionalObject = optionalObject;
		this.optionalId = optionalObjectId;
		this.optionalEntityName = optionalEntityName;
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues) {
		this( positionalParameterTypes, positionalParameterValues, null, null, false, false, false, null, null, null, false, null );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Serializable[] collectionKeys) {
		this( positionalParameterTypes, positionalParameterValues, null, collectionKeys );
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final Serializable[] collectionKeys) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				namedParameters,
				null,
				null,
				false,
				false,
				false,
				null,
				null,
				null,
				collectionKeys,
				null
		);
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final List<String> queryHints,
			final boolean isLookupByNaturalKey,
			final ResultTransformer transformer) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				null,
				lockOptions,
				rowSelection,
				isReadOnlyInitialized,
				readOnly,
				cacheable,
				cacheRegion,
				comment,
				queryHints,
				null,
				transformer
		);
		isNaturalKeyLookup = isLookupByNaturalKey;
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final List<String> queryHints,
			final Serializable[] collectionKeys,
			ResultTransformer transformer) {
		this.positionalParameterTypes = positionalParameterTypes;
		this.positionalParameterValues = positionalParameterValues;
		this.namedParameters = namedParameters;
		this.lockOptions = lockOptions;
		this.rowSelection = rowSelection;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		//this.forceCacheRefresh = forceCacheRefresh;
		this.comment = comment;
		this.queryHints = queryHints;
		this.collectionKeys = collectionKeys;
		this.isReadOnlyInitialized = isReadOnlyInitialized;
		this.readOnly = readOnly;
		this.resultTransformer = transformer;
	}

	public QueryParameters(
			final Type[] positionalParameterTypes,
			final Object[] positionalParameterValues,
			final Map<String,TypedValue> namedParameters,
			final LockOptions lockOptions,
			final RowSelection rowSelection,
			final boolean isReadOnlyInitialized,
			final boolean readOnly,
			final boolean cacheable,
			final String cacheRegion,
			//final boolean forceCacheRefresh,
			final String comment,
			final List<String> queryHints,
			final Serializable[] collectionKeys,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final ResultTransformer transformer) {
		this(
				positionalParameterTypes,
				positionalParameterValues,
				namedParameters,
				lockOptions,
				rowSelection,
				isReadOnlyInitialized,
				readOnly,
				cacheable,
				cacheRegion,
				comment,
				queryHints,
				collectionKeys,
				transformer
		);
		this.optionalEntityName = optionalEntityName;
		this.optionalId = optionalId;
		this.optionalObject = optionalObject;
	}

	public QueryParameters(
			QueryParameterBindings queryParameterBindings,
			LockOptions lockOptions,
			RowSelection selection,
			final boolean isReadOnlyInitialized,
			boolean readOnly,
			boolean cacheable,
			String cacheRegion,
			String comment,
			List<String> dbHints,
			final Serializable[] collectionKeys,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			ResultTransformer resultTransformer) {
		this(
				queryParameterBindings.collectPositionalBindTypes(),
				queryParameterBindings.collectPositionalBindValues(),
				queryParameterBindings.collectNamedParameterBindings(),
				lockOptions,
				selection,
				isReadOnlyInitialized,
				readOnly,
				cacheable,
				cacheRegion,
				comment,
				dbHints,
				collectionKeys,
				optionalObject,
				optionalEntityName,
				optionalId,
				resultTransformer
		);

	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public boolean hasRowSelection() {
		return rowSelection != null;
	}

	public Map<String,TypedValue> getNamedParameters() {
		return namedParameters;
	}

	public Type[] getPositionalParameterTypes() {
		return positionalParameterTypes;
	}

	public Object[] getPositionalParameterValues() {
		return positionalParameterValues;
	}

	public RowSelection getRowSelection() {
		return rowSelection;
	}

	public ResultTransformer getResultTransformer() {
		return resultTransformer;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public void setNamedParameters(Map<String,TypedValue> map) {
		namedParameters = map;
	}

	public void setPositionalParameterTypes(Type[] types) {
		positionalParameterTypes = types;
	}

	public void setPositionalParameterValues(Object[] objects) {
		positionalParameterValues = objects;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public void setRowSelection(RowSelection selection) {
		rowSelection = selection;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public void setLockOptions(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
	}

	public void traceParameters(SessionFactoryImplementor factory) throws HibernateException {
		EntityPrinter print = new EntityPrinter( factory );
		if ( positionalParameterValues.length != 0 ) {
			LOG.tracev( "Parameters: {0}", print.toString( positionalParameterTypes, positionalParameterValues ) );
		}
		if ( namedParameters != null ) {
			LOG.tracev( "Named parameters: {0}", print.toString( namedParameters ) );
		}
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public void setCacheable(boolean b) {
		cacheable = b;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public void setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion;
	}

	public void validateParameters() throws QueryException {
		final int types = positionalParameterTypes == null ? 0 : positionalParameterTypes.length;
		final int values = positionalParameterValues == null ? 0 : positionalParameterValues.length;
		if ( types != values ) {
			throw new QueryException(
					"Number of positional parameter types:" + types +
							" does not match number of positional parameters: " + values
			);
		}
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
	  
	public List<String> getQueryHints() {
		return queryHints;
	}

	public void setQueryHints(List<String> queryHints) {
		this.queryHints = queryHints;
	}

	public ScrollMode getScrollMode() {
		return scrollMode;
	}

	public void setScrollMode(ScrollMode scrollMode) {
		this.scrollMode = scrollMode;
	}

	public Serializable[] getCollectionKeys() {
		return collectionKeys;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public void setCollectionKeys(Serializable[] collectionKeys) {
		this.collectionKeys = collectionKeys;
	}

	public String getOptionalEntityName() {
		return optionalEntityName;
	}

	public void setOptionalEntityName(String optionalEntityName) {
		this.optionalEntityName = optionalEntityName;
	}

	public Serializable getOptionalId() {
		return optionalId;
	}

	public void setOptionalId(Serializable optionalId) {
		this.optionalId = optionalId;
	}

	public Object getOptionalObject() {
		return optionalObject;
	}

	public void setOptionalObject(Object optionalObject) {
		this.optionalObject = optionalObject;
	}

	/**
	 * Has the read-only/modifiable mode been explicitly set?
	 * @see QueryParameters#setReadOnly(boolean)
	 * @see QueryParameters#isReadOnly(SharedSessionContractImplementor)
	 *
	 * @return true, the read-only/modifiable mode was explicitly set
	 *         false, the read-only/modifiable mode was not explicitly set
	 */
	public boolean isReadOnlyInitialized() {
		return isReadOnlyInitialized;
	}

	/**
	 * Should entities and proxies loaded by the Query be put in read-only mode? The
	 * read-only/modifiable setting must be initialized via QueryParameters#setReadOnly(boolean)
	 * before calling this method.
	 *
	 * @see QueryParameters#isReadOnlyInitialized()
	 * @see QueryParameters#isReadOnly(SharedSessionContractImplementor)
	 * @see QueryParameters#setReadOnly(boolean)
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 * @return true, entities and proxies loaded by the Query will be put in read-only mode
	 *         false, entities and proxies loaded by the Query will be put in modifiable mode
	 * @throws IllegalStateException if the read-only/modifiable setting has not been
	 * initialized (i.e., isReadOnlyInitialized() == false).
	 */
	public boolean isReadOnly() {
		if ( !isReadOnlyInitialized() ) {
			throw new IllegalStateException( "cannot call isReadOnly() when isReadOnlyInitialized() returns false" );
		}
		return readOnly;
	}

	/**
	 * Should entities and proxies loaded by the Query be put in read-only mode?  If the
	 * read-only/modifiable setting was not initialized (i.e., QueryParameters#isReadOnlyInitialized() == false),
	 * then the default read-only/modifiable setting for the persistence context is returned instead.
	 * <p/>
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 * @param session The originating session
	 *
	 * @return {@code true} indicates that entities and proxies loaded by the query will be put in read-only mode;
	 * {@code false} indicates that entities and proxies loaded by the query will be put in modifiable mode
	 *
	 * @see QueryParameters#isReadOnlyInitialized()
	 * @see QueryParameters#setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 */
	public boolean isReadOnly(SharedSessionContractImplementor session) {
		return isReadOnlyInitialized
				? isReadOnly()
				: session.getPersistenceContextInternal().isDefaultReadOnly();
	}

	/**
	 * Set the read-only/modifiable mode for entities and proxies loaded by the query.
	 * <p/>
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 * @param readOnly if {@code true}, entities and proxies loaded by the query will be put in read-only mode; if
	 * {@code false}, entities and proxies loaded by the query will be put in modifiable mode
	 *
	 * @see QueryParameters#isReadOnlyInitialized()
	 * @see QueryParameters#isReadOnly(SharedSessionContractImplementor)
	 * @see QueryParameters#setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		this.isReadOnlyInitialized = true;
	}

	public void setCallable(boolean callable) {
		this.callable = callable;
	}

	public boolean isCallable() {
		return callable;
	}

	public boolean hasAutoDiscoverScalarTypes() {
		return autodiscovertypes;
	}

	/**
	 * Check if this query should pass the {@code distinct} to the database.
	 * @return the query passes {@code distinct} to the database
	 */
	public boolean isPassDistinctThrough() {
		return passDistinctThrough;
	}

	/**
	 * Set if this query should pass the {@code distinct} to the database.
	 * @param passDistinctThrough the query passes {@code distinct} to the database
	 */
	public void setPassDistinctThrough(boolean passDistinctThrough) {
		this.passDistinctThrough = passDistinctThrough;
	}

	public void processFilters(String sql, SharedSessionContractImplementor session) {
		processFilters( sql, session.getLoadQueryInfluencers().getEnabledFilters(), session.getFactory() );
	}

	@SuppressWarnings( {"unchecked"})
	public void processFilters(String sql, Map filters, SessionFactoryImplementor factory) {
		if ( filters.size() == 0 || !sql.contains( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
			// HELLA IMPORTANT OPTIMIZATION!!!
			processedPositionalParameterValues = getPositionalParameterValues();
			processedPositionalParameterTypes = getPositionalParameterTypes();
			processedSQL = sql;
		}
		else {
			final StringTokenizer tokens = new StringTokenizer( sql, SYMBOLS, true );
			StringBuilder result = new StringBuilder();
			List parameters = new ArrayList();
			List parameterTypes = new ArrayList();
			int positionalIndex = 0;
			while ( tokens.hasMoreTokens() ) {
				final String token = tokens.nextToken();
				if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
					final String filterParameterName = token.substring( 1 );
					final String[] parts = LoadQueryInfluencers.parseFilterParameterName( filterParameterName );
					final FilterImpl filter = (FilterImpl) filters.get( parts[0] );
					final Object value = filter.getParameter( parts[1] );
					final Type type = filter.getFilterDefinition().getParameterType( parts[1] );
					if ( value != null && Collection.class.isAssignableFrom( value.getClass() ) ) {
						Iterator itr = ( (Collection) value ).iterator();
						while ( itr.hasNext() ) {
							final Object elementValue = itr.next();
							result.append( '?' );
							parameters.add( elementValue );
							parameterTypes.add( type );
							if ( itr.hasNext() ) {
								result.append( ", " );
							}
						}
					}
					else {
						result.append( '?' );
						parameters.add( value );
						parameterTypes.add( type );
					}
				}
				else {
					result.append( token );
					if ( "?".equals( token ) && positionalIndex < getPositionalParameterValues().length ) {
						final Type type = getPositionalParameterTypes()[positionalIndex];
						if ( type.isComponentType() ) {
							// should process tokens till reaching the number of "?" corresponding to the
							// numberOfParametersCoveredBy of the compositeType
							int paramIndex = 1;
							final int numberOfParametersCoveredBy = getNumberOfParametersCoveredBy( ((ComponentType) type).getSubtypes() );
							while ( paramIndex < numberOfParametersCoveredBy ) {
								final String nextToken = tokens.nextToken();
								if ( "?".equals( nextToken ) ) {
									paramIndex++;
								}
								result.append( nextToken );
							}
						}
						parameters.add( getPositionalParameterValues()[positionalIndex] );
						parameterTypes.add( type );
						positionalIndex++;
					}
				}
			}
			processedPositionalParameterValues = parameters.toArray();
			processedPositionalParameterTypes = ( Type[] ) parameterTypes.toArray( new Type[parameterTypes.size()] );
			processedSQL = result.toString();
		}
	}

	private int getNumberOfParametersCoveredBy(Type[] subtypes) {
		int numberOfParameters = 0;
		for ( Type type : subtypes ) {
			if ( type.isComponentType() ) {
				numberOfParameters = numberOfParameters + getNumberOfParametersCoveredBy( ((ComponentType) type).getSubtypes() );
			}
			else {
				numberOfParameters++;
			}
		}
		return numberOfParameters;
	}

	public String getFilteredSQL() {
		return processedSQL;
	}

	public Object[] getFilteredPositionalParameterValues() {
		return processedPositionalParameterValues;
	}

	public Type[] getFilteredPositionalParameterTypes() {
		return processedPositionalParameterTypes;
	}

	public boolean isNaturalKeyLookup() {
		return isNaturalKeyLookup;
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public void setNaturalKeyLookup(boolean isNaturalKeyLookup) {
		this.isNaturalKeyLookup = isNaturalKeyLookup;
	}

	public void setAutoDiscoverScalarTypes(boolean autodiscovertypes) {
		this.autodiscovertypes = autodiscovertypes;
	}

	public QueryParameters createCopyUsing(RowSelection selection) {
		QueryParameters copy = new QueryParameters(
				this.positionalParameterTypes,
				this.positionalParameterValues,
				this.namedParameters,
				this.lockOptions,
				selection,
				this.isReadOnlyInitialized,
				this.readOnly,
				this.cacheable,
				this.cacheRegion,
				this.comment,
				this.queryHints,
				this.collectionKeys,
				this.optionalObject,
				this.optionalEntityName,
				this.optionalId,
				this.resultTransformer
		);
		copy.processedSQL = this.processedSQL;
		copy.processedPositionalParameterTypes = this.processedPositionalParameterTypes;
		copy.processedPositionalParameterValues = this.processedPositionalParameterValues;
		copy.passDistinctThrough = this.passDistinctThrough;
		return copy;
	}

	public HQLQueryPlan getQueryPlan() {
		return queryPlan;
	}

	public void setQueryPlan(HQLQueryPlan queryPlan) {
		this.queryPlan = queryPlan;
	}

	public void bindDynamicParameter(Type paramType, Object paramValue) {
		if(processedPositionalParameterTypes != null) {
			int length = processedPositionalParameterTypes.length;
			Type[] types = new Type[length + 1];
			Object[] values = new Object[length + 1];
			for ( int i = 0; i < length; i++ ) {
				types[i] = processedPositionalParameterTypes[i];
				values[i] = processedPositionalParameterValues[i];
			}
			types[length] = paramType;
			values[length] = paramValue;
			processedPositionalParameterTypes = types;
			processedPositionalParameterValues = values;
		}
	}
}
