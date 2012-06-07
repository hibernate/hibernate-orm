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
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.util.EntityPrinter;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public final class QueryParameters {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, QueryParameters.class.getName());

	private Type[] positionalParameterTypes;
	private Object[] positionalParameterValues;
	private Map<String,TypedValue> namedParameters;
	private LockOptions lockOptions;
	private RowSelection rowSelection;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private ScrollMode scrollMode;
	private Serializable[] collectionKeys;
	private Object optionalObject;
	private String optionalEntityName;
	private Serializable optionalId;
	private boolean isReadOnlyInitialized;
	private boolean readOnly;
	private boolean callable = false;
	private boolean autodiscovertypes = false;
	private boolean isNaturalKeyLookup;

	private final ResultTransformer resultTransformer; // why is all others non final ?

	private String processedSQL;
	private Type[] processedPositionalParameterTypes;
	private Object[] processedPositionalParameterValues;

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
		this( positionalParameterTypes, positionalParameterValues, null, null, false, false, false, null, null, false, null );
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
				collectionKeys,
				transformer
		);
		this.optionalEntityName = optionalEntityName;
		this.optionalId = optionalId;
		this.optionalObject = optionalObject;
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
		int types = positionalParameterTypes == null ? 0 : positionalParameterTypes.length;
		int values = positionalParameterValues == null ? 0 : positionalParameterValues.length;
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
	 * @see QueryParameters#isReadOnly(org.hibernate.engine.spi.SessionImplementor)
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
	 * @see QueryParameters#isReadOnly(org.hibernate.engine.spi.SessionImplementor)
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
		if ( ! isReadOnlyInitialized() ) {
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
	public boolean isReadOnly(SessionImplementor session) {
		return isReadOnlyInitialized
				? isReadOnly()
				: session.getPersistenceContext().isDefaultReadOnly();
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
	 * @see QueryParameters#isReadOnly(org.hibernate.engine.spi.SessionImplementor)
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

	public void processFilters(String sql, SessionImplementor session) {
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
			final Dialect dialect = factory.getDialect();
			String symbols = new StringBuilder().append( ParserHelper.HQL_SEPARATORS )
					.append( dialect.openQuote() )
					.append( dialect.closeQuote() )
					.toString();
			StringTokenizer tokens = new StringTokenizer( sql, symbols, true );
			StringBuilder result = new StringBuilder();

			List parameters = new ArrayList();
			List parameterTypes = new ArrayList();

			int positionalIndex = 0;
			while ( tokens.hasMoreTokens() ) {
				final String token = tokens.nextToken();
				if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) {
					final String filterParameterName = token.substring( 1 );
					final String[] parts = LoadQueryInfluencers.parseFilterParameterName( filterParameterName );
					final FilterImpl filter = ( FilterImpl ) filters.get( parts[0] );
					final Object value = filter.getParameter( parts[1] );
					final Type type = filter.getFilterDefinition().getParameterType( parts[1] );
					if ( value != null && Collection.class.isAssignableFrom( value.getClass() ) ) {
						Iterator itr = ( ( Collection ) value ).iterator();
						while ( itr.hasNext() ) {
							Object elementValue = itr.next();
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
					if ( "?".equals( token ) && positionalIndex < getPositionalParameterValues().length ) {
						parameters.add( getPositionalParameterValues()[positionalIndex] );
						parameterTypes.add( getPositionalParameterTypes()[positionalIndex] );
						positionalIndex++;
					}
					result.append( token );
				}
			}
			processedPositionalParameterValues = parameters.toArray();
			processedPositionalParameterTypes = ( Type[] ) parameterTypes.toArray( new Type[parameterTypes.size()] );
			processedSQL = result.toString();
		}
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
				this.collectionKeys,
				this.optionalObject,
				this.optionalEntityName,
				this.optionalId,
				this.resultTransformer
		);
		copy.processedSQL = this.processedSQL;
		copy.processedPositionalParameterTypes = this.processedPositionalParameterTypes;
		copy.processedPositionalParameterValues = this.processedPositionalParameterValues;
		return copy;
	}
}
