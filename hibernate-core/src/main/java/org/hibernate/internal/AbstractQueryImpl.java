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
package org.hibernate.internal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.property.Getter;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.SerializableType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Abstract implementation of the Query interface.
 *
 * @author Gavin King
 * @author Max Andersen
 */
public abstract class AbstractQueryImpl implements Query {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AbstractQueryImpl.class.getName()
	);

	private static final Object UNSET_PARAMETER = new MarkerObject("<unset parameter>");
	private static final Object UNSET_TYPE = new MarkerObject("<unset type>");

	private final String queryString;
	protected final SessionImplementor session;
	protected final ParameterMetadata parameterMetadata;

	// parameter bind values...
	private List values = new ArrayList(4);
	private List types = new ArrayList(4);
	private Map<String,TypedValue> namedParameters = new HashMap<String, TypedValue>(4);
	private Map<String, TypedValue> namedParameterLists = new HashMap<String, TypedValue>(4);

	private Object optionalObject;
	private Serializable optionalId;
	private String optionalEntityName;

	private RowSelection selection;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private final List<String> queryHints = new ArrayList<String>();
	private FlushMode flushMode;
	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;
	private Serializable collectionKey;
	private Boolean readOnly;
	private ResultTransformer resultTransformer;
	
	private HQLQueryPlan queryPlan;

	public AbstractQueryImpl(
			String queryString,
	        FlushMode flushMode,
	        SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		this.session = session;
		this.queryString = queryString;
		this.selection = new RowSelection();
		this.flushMode = flushMode;
		this.cacheMode = null;
		this.parameterMetadata = parameterMetadata;
	}

	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + queryString + ')';
	}

	@Override
	public final String getQueryString() {
		return queryString;
	}

	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public Query setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	@Override
	public String getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public Query setCacheRegion(String cacheRegion) {
		if (cacheRegion != null) {
			this.cacheRegion = cacheRegion.trim();
		}
		return this;
	}

	@Override
	public FlushMode getFlushMode() {
		return flushMode;
	}

	@Override
	public Query setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public Query setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Query setComment(String comment) {
		this.comment = comment;
		return this;
	}
	  
	@Override
	public Query addQueryHint(String queryHint) {
		queryHints.add( queryHint );
		return this;
	} 

	@Override
	public Integer getFirstResult() {
		return selection.getFirstRow();
	}

	@Override
	public Query setFirstResult(int firstResult) {
		selection.setFirstRow( firstResult);
		return this;
	}

	@Override
	public Integer getMaxResults() {
		return selection.getMaxRows();
	}

	@Override
	public Query setMaxResults(int maxResults) {
		if ( maxResults <= 0 ) {
			// treat zero and negatives specically as meaning no limit...
			selection.setMaxRows( null );
		}
		else {
			selection.setMaxRows( maxResults);
		}
		return this;
	}

	@Override
	public Integer getTimeout() {
		return selection.getTimeout();
	}

	@Override
	public Query setTimeout(int timeout) {
		selection.setTimeout( timeout);
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return selection.getFetchSize();
	}

	@Override
	public Query setFetchSize(int fetchSize) {
		selection.setFetchSize( fetchSize);
		return this;
	}

	public Type[] getReturnTypes() throws HibernateException {
		return session.getFactory().getReturnTypes( queryString );
	}

	public String[] getReturnAliases() throws HibernateException {
		return session.getFactory().getReturnAliases( queryString );
	}

	public Query setCollectionKey(Serializable collectionKey) {
		this.collectionKey = collectionKey;
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return ( readOnly == null ?
				getSession().getPersistenceContext().isDefaultReadOnly() :
				readOnly
		);
	}

	@Override
	public Query setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}
	@Override
	public Query setResultTransformer(ResultTransformer transformer) {
		this.resultTransformer = transformer;
		return this;
	}
	
	public void setOptionalEntityName(String optionalEntityName) {
		this.optionalEntityName = optionalEntityName;
	}

	public void setOptionalId(Serializable optionalId) {
		this.optionalId = optionalId;
	}

	public void setOptionalObject(Object optionalObject) {
		this.optionalObject = optionalObject;
	}

	SessionImplementor getSession() {
		return session;
	}
	@Override
	public abstract LockOptions getLockOptions();


	// Parameter handling code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns a shallow copy of the named parameter value map.
	 *
	 * @return Shallow copy of the named parameter value map
	 */
	protected Map<String, TypedValue> getNamedParams() {
		return new HashMap<String, TypedValue>( namedParameters );
	}

	/**
	 * Returns an array representing all named parameter names encountered
	 * during (intial) parsing of the query.
	 * <p/>
	 * Note <i>initial</i> here means different things depending on whether
	 * this is a native-sql query or an HQL/filter query.  For native-sql, a
	 * precursory inspection of the query string is performed specifically to
	 * locate defined parameters.  For HQL/filter queries, this is the
	 * information returned from the query-translator.  This distinction
	 * holds true for all parameter metadata exposed here.
	 *
	 * @return Array of named parameter names.
	 * @throws HibernateException
	 */
	@Override
	public String[] getNamedParameters() throws HibernateException {
		return ArrayHelper.toStringArray( parameterMetadata.getNamedParameterNames() );
	}

	/**
	 * Does this query contain named parameters?
	 *
	 * @return True if the query was found to contain named parameters; false
	 * otherwise;
	 */
	public boolean hasNamedParameters() {
		return parameterMetadata.getNamedParameterNames().size() > 0;
	}

	/**
	 * Retreive the value map for any named parameter lists (i.e., for
	 * auto-expansion) bound to this query.
	 *
	 * @return The parameter list value map.
	 */
	protected Map<String, TypedValue> getNamedParameterLists() {
		return namedParameterLists;
	}

	/**
	 * Retreives the list of parameter values bound to this query for
	 * ordinal parameters.
	 *
	 * @return The ordinal parameter values.
	 */
	protected List getValues() {
		return values;
	}

	/**
	 * Retreives the list of parameter {@link Type type}s bound to this query for
	 * ordinal parameters.
	 *
	 * @return The ordinal parameter types.
	 */
	protected List getTypes() {
		return types;
	}

	/**
	 * Perform parameter validation.  Used prior to executing the encapsulated
	 * query.
	 *
	 * @throws QueryException
	 */
	protected void verifyParameters() throws QueryException {
		verifyParameters(false);
	}

	/**
	 * Perform parameter validation.  Used prior to executing the encapsulated
	 * query.
	 *
	 * @param reserveFirstParameter if true, the first ? will not be verified since
	 * its needed for e.g. callable statements returning a out parameter
	 * @throws HibernateException
	 */
	protected void verifyParameters(boolean reserveFirstParameter) throws HibernateException {
		if ( parameterMetadata.getNamedParameterNames().size() != namedParameters.size() + namedParameterLists.size() ) {
			Set<String> missingParams = new HashSet<String>( parameterMetadata.getNamedParameterNames() );
			missingParams.removeAll( namedParameterLists.keySet() );
			missingParams.removeAll( namedParameters.keySet() );
			throw new QueryException( "Not all named parameters have been set: " + missingParams, getQueryString() );
		}

		int positionalValueSpan = 0;
		for ( int i = 0; i < values.size(); i++ ) {
			Object object = types.get( i );
			if( values.get( i ) == UNSET_PARAMETER || object == UNSET_TYPE ) {
				if ( reserveFirstParameter && i==0 ) {
					continue;
				}
				else {
					throw new QueryException( "Unset positional parameter at position: " + i, getQueryString() );
				}
			}
			positionalValueSpan += ( (Type) object ).getColumnSpan( session.getFactory() );
		}

		if ( parameterMetadata.getOrdinalParameterCount() != positionalValueSpan ) {
			if ( reserveFirstParameter && parameterMetadata.getOrdinalParameterCount() - 1 != positionalValueSpan ) {
				throw new QueryException(
				 		"Expected positional parameter count: " +
				 		(parameterMetadata.getOrdinalParameterCount()-1) +
				 		", actual parameters: " +
				 		values,
				 		getQueryString()
				 	);
			}
			else if ( !reserveFirstParameter ) {
				throw new QueryException(
				 		"Expected positional parameter count: " +
				 		parameterMetadata.getOrdinalParameterCount() +
				 		", actual parameters: " +
				 		values,
				 		getQueryString()
				 	);
			}
		}
	}

	public Query setParameter(int position, Object val, Type type) {
		if ( parameterMetadata.getOrdinalParameterCount() == 0 ) {
			throw new IllegalArgumentException("No positional parameters in query: " + getQueryString() );
		}
		if ( position < 0 || position > parameterMetadata.getOrdinalParameterCount() - 1 ) {
			throw new IllegalArgumentException("Positional parameter does not exist: " + position + " in query: " + getQueryString() );
		}
		int size = values.size();
		if ( position < size ) {
			values.set( position, val );
			types.set( position, type );
		}
		else {
			// prepend value and type list with null for any positions before the wanted position.
			for ( int i = 0; i < position - size; i++ ) {
				values.add( UNSET_PARAMETER );
				types.add( UNSET_TYPE );
			}
			values.add( val );
			types.add( type );
		}
		return this;
	}

	public Query setParameter(String name, Object val, Type type) {
		if ( !parameterMetadata.getNamedParameterNames().contains( name ) ) {
			throw new IllegalArgumentException("Parameter " + name + " does not exist as a named parameter in [" + getQueryString() + "]");
		}
		else {
			 namedParameters.put( name, new TypedValue( type, val  ) );
			 return this;
		}
	}

	public Query setParameter(int position, Object val) throws HibernateException {
		if (val == null) {
			setParameter( position, val, StandardBasicTypes.SERIALIZABLE );
		}
		else {
			setParameter( position, val, determineType( position, val ) );
		}
		return this;
	}

	public Query setParameter(String name, Object val) throws HibernateException {
		if (val == null) {
			Type type = parameterMetadata.getNamedParameterExpectedType( name );
			if ( type == null ) {
				type = StandardBasicTypes.SERIALIZABLE;
			}
			setParameter( name, val, type );
		}
		else {
			setParameter( name, val, determineType( name, val ) );
		}
		return this;
	}

	protected Type determineType(int paramPosition, Object paramValue, Type defaultType) {
		Type type = parameterMetadata.getOrdinalParameterExpectedType( paramPosition + 1 );
		if ( type == null ) {
			type = defaultType;
		}
		return type;
	}

	protected Type determineType(int paramPosition, Object paramValue) throws HibernateException {
		Type type = parameterMetadata.getOrdinalParameterExpectedType( paramPosition + 1 );
		if ( type == null ) {
			type = guessType( paramValue );
		}
		return type;
	}

	protected Type determineType(String paramName, Object paramValue, Type defaultType) {
		Type type = parameterMetadata.getNamedParameterExpectedType( paramName );
		if ( type == null ) {
			type = defaultType;
		}
		return type;
	}

	protected Type determineType(String paramName, Object paramValue) throws HibernateException {
		Type type = parameterMetadata.getNamedParameterExpectedType( paramName );
		if ( type == null ) {
			type = guessType( paramValue );
		}
		return type;
	}

	protected Type determineType(String paramName, Class clazz) throws HibernateException {
		Type type = parameterMetadata.getNamedParameterExpectedType( paramName );
		if ( type == null ) {
			type = guessType( clazz );
		}
		return type;
	}

	private Type guessType(Object param) throws HibernateException {
		Class clazz = HibernateProxyHelper.getClassWithoutInitializingProxy( param );
		return guessType( clazz );
	}

	private Type guessType(Class clazz) throws HibernateException {
		String typename = clazz.getName();
		Type type = session.getFactory().getTypeResolver().heuristicType(typename);
		boolean serializable = type!=null && type instanceof SerializableType;
		if (type==null || serializable) {
			try {
				session.getFactory().getEntityPersister( clazz.getName() );
			}
			catch (MappingException me) {
				if (serializable) {
					return type;
				}
				else {
					throw new HibernateException("Could not determine a type for class: " + typename);
				}
			}
			return ( (Session) session ).getTypeHelper().entity( clazz );
		}
		else {
			return type;
		}
	}

	public Query setString(int position, String val) {
		setParameter(position, val, StandardBasicTypes.STRING);
		return this;
	}

	public Query setCharacter(int position, char val) {
		setParameter( position, Character.valueOf( val ), StandardBasicTypes.CHARACTER );
		return this;
	}

	public Query setBoolean(int position, boolean val) {
		Boolean valueToUse = val;
		Type typeToUse = determineType( position, valueToUse, StandardBasicTypes.BOOLEAN );
		setParameter( position, valueToUse, typeToUse );
		return this;
	}

	public Query setByte(int position, byte val) {
		setParameter(position, val, StandardBasicTypes.BYTE);
		return this;
	}

	public Query setShort(int position, short val) {
		setParameter(position, val, StandardBasicTypes.SHORT);
		return this;
	}

	public Query setInteger(int position, int val) {
		setParameter(position, val, StandardBasicTypes.INTEGER);
		return this;
	}

	public Query setLong(int position, long val) {
		setParameter(position, val, StandardBasicTypes.LONG);
		return this;
	}

	public Query setFloat(int position, float val) {
		setParameter(position, val, StandardBasicTypes.FLOAT);
		return this;
	}

	public Query setDouble(int position, double val) {
		setParameter(position, val, StandardBasicTypes.DOUBLE);
		return this;
	}

	public Query setBinary(int position, byte[] val) {
		setParameter(position, val, StandardBasicTypes.BINARY);
		return this;
	}

	public Query setText(int position, String val) {
		setParameter(position, val, StandardBasicTypes.TEXT);
		return this;
	}

	public Query setSerializable(int position, Serializable val) {
		setParameter(position, val, StandardBasicTypes.SERIALIZABLE);
		return this;
	}

	public Query setDate(int position, Date date) {
		setParameter(position, date, StandardBasicTypes.DATE);
		return this;
	}

	public Query setTime(int position, Date date) {
		setParameter(position, date, StandardBasicTypes.TIME);
		return this;
	}

	public Query setTimestamp(int position, Date date) {
		setParameter(position, date, StandardBasicTypes.TIMESTAMP);
		return this;
	}

	public Query setEntity(int position, Object val) {
		setParameter( position, val, ( (Session) session ).getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	private String resolveEntityName(Object val) {
		if ( val == null ) {
			throw new IllegalArgumentException( "entity for parameter binding cannot be null" );
		}
		return session.bestGuessEntityName( val );
	}

	public Query setLocale(int position, Locale locale) {
		setParameter(position, locale, StandardBasicTypes.LOCALE);
		return this;
	}

	public Query setCalendar(int position, Calendar calendar) {
		setParameter(position, calendar, StandardBasicTypes.CALENDAR);
		return this;
	}

	public Query setCalendarDate(int position, Calendar calendar) {
		setParameter(position, calendar, StandardBasicTypes.CALENDAR_DATE);
		return this;
	}

	public Query setBinary(String name, byte[] val) {
		setParameter(name, val, StandardBasicTypes.BINARY);
		return this;
	}

	public Query setText(String name, String val) {
		setParameter(name, val, StandardBasicTypes.TEXT);
		return this;
	}

	public Query setBoolean(String name, boolean val) {
		Boolean valueToUse = val;
		Type typeToUse = determineType( name, valueToUse, StandardBasicTypes.BOOLEAN );
		setParameter( name, valueToUse, typeToUse );
		return this;
	}

	public Query setByte(String name, byte val) {
		setParameter(name, val, StandardBasicTypes.BYTE);
		return this;
	}

	public Query setCharacter(String name, char val) {
		setParameter(name, val, StandardBasicTypes.CHARACTER);
		return this;
	}

	public Query setDate(String name, Date date) {
		setParameter(name, date, StandardBasicTypes.DATE);
		return this;
	}

	public Query setDouble(String name, double val) {
		setParameter(name, val, StandardBasicTypes.DOUBLE);
		return this;
	}

	public Query setEntity(String name, Object val) {
		setParameter( name, val, ( (Session) session ).getTypeHelper().entity( resolveEntityName( val ) ) );
		return this;
	}

	public Query setFloat(String name, float val) {
		setParameter(name, val, StandardBasicTypes.FLOAT);
		return this;
	}

	public Query setInteger(String name, int val) {
		setParameter(name, val, StandardBasicTypes.INTEGER);
		return this;
	}

	public Query setLocale(String name, Locale locale) {
		setParameter(name, locale, StandardBasicTypes.LOCALE);
		return this;
	}

	public Query setCalendar(String name, Calendar calendar) {
		setParameter(name, calendar, StandardBasicTypes.CALENDAR);
		return this;
	}

	public Query setCalendarDate(String name, Calendar calendar) {
		setParameter(name, calendar, StandardBasicTypes.CALENDAR_DATE);
		return this;
	}

	public Query setLong(String name, long val) {
		setParameter(name, val, StandardBasicTypes.LONG);
		return this;
	}

	public Query setSerializable(String name, Serializable val) {
		setParameter(name, val, StandardBasicTypes.SERIALIZABLE);
		return this;
	}

	public Query setShort(String name, short val) {
		setParameter(name, val, StandardBasicTypes.SHORT);
		return this;
	}

	public Query setString(String name, String val) {
		setParameter(name, val, StandardBasicTypes.STRING);
		return this;
	}

	public Query setTime(String name, Date date) {
		setParameter(name, date, StandardBasicTypes.TIME);
		return this;
	}

	public Query setTimestamp(String name, Date date) {
		setParameter(name, date, StandardBasicTypes.TIMESTAMP);
		return this;
	}

	public Query setBigDecimal(int position, BigDecimal number) {
		setParameter(position, number, StandardBasicTypes.BIG_DECIMAL);
		return this;
	}

	public Query setBigDecimal(String name, BigDecimal number) {
		setParameter(name, number, StandardBasicTypes.BIG_DECIMAL);
		return this;
	}

	public Query setBigInteger(int position, BigInteger number) {
		setParameter(position, number, StandardBasicTypes.BIG_INTEGER);
		return this;
	}

	public Query setBigInteger(String name, BigInteger number) {
		setParameter(name, number, StandardBasicTypes.BIG_INTEGER);
		return this;
	}

	@Override
	public Query setParameterList(String name, Collection vals, Type type) throws HibernateException {
		if ( !parameterMetadata.getNamedParameterNames().contains( name ) ) {
			throw new IllegalArgumentException("Parameter " + name + " does not exist as a named parameter in [" + getQueryString() + "]");
		}
		namedParameterLists.put( name, new TypedValue( type, vals ) );
		return this;
	}
	
	/**
	 * Warning: adds new parameters to the argument by side-effect, as well as
	 * mutating the query string!
	 */
	protected String expandParameterLists(Map namedParamsCopy) {
		String query = this.queryString;
		for ( Map.Entry<String, TypedValue> stringTypedValueEntry : namedParameterLists.entrySet() ) {
			Map.Entry me = (Map.Entry) stringTypedValueEntry;
			query = expandParameterList( query, (String) me.getKey(), (TypedValue) me.getValue(), namedParamsCopy );
		}
		return query;
	}

	/**
	 * Warning: adds new parameters to the argument by side-effect, as well as
	 * mutating the query string!
	 */
	private String expandParameterList(String query, String name, TypedValue typedList, Map namedParamsCopy) {
		Collection vals = (Collection) typedList.getValue();
		
		// HHH-1123
		// Some DBs limit number of IN expressions.  For now, warn...
		final Dialect dialect = session.getFactory().getDialect();
		final int inExprLimit = dialect.getInExpressionCountLimit();
		if ( inExprLimit > 0 && vals.size() > inExprLimit ) {
			log.tooManyInExpressions( dialect.getClass().getName(), inExprLimit, name, vals.size() );
		}

		Type type = typedList.getType();

		boolean isJpaPositionalParam = parameterMetadata.getNamedParameterDescriptor( name ).isJpaStyle();
		String paramPrefix = isJpaPositionalParam ? "?" : ParserHelper.HQL_VARIABLE_PREFIX;
		String placeholder =
				new StringBuilder( paramPrefix.length() + name.length() )
						.append( paramPrefix ).append(  name )
						.toString();

		if ( query == null ) {
			return query;
		}
		int loc = query.indexOf( placeholder );

		if ( loc < 0 ) {
			return query;
		}

		String beforePlaceholder = query.substring( 0, loc );
		String afterPlaceholder =  query.substring( loc + placeholder.length() );

		// check if placeholder is already immediately enclosed in parentheses
		// (ignoring whitespace)
		boolean isEnclosedInParens =
				StringHelper.getLastNonWhitespaceCharacter( beforePlaceholder ) == '(' &&
				StringHelper.getFirstNonWhitespaceCharacter( afterPlaceholder ) == ')';

		if ( vals.size() == 1  && isEnclosedInParens ) {
			// short-circuit for performance when only 1 value and the
			// placeholder is already enclosed in parentheses...
			namedParamsCopy.put( name, new TypedValue( type, vals.iterator().next() ) );
			return query;
		}

		StringBuilder list = new StringBuilder( 16 );
		Iterator iter = vals.iterator();
		int i = 0;
		while ( iter.hasNext() ) {
			// Variable 'name' can represent a number or contain digit at the end. Surrounding it with
			// characters to avoid ambiguous definition after concatenating value of 'i' counter.
			String alias = ( isJpaPositionalParam ? 'x' + name : name ) + '_' + i++ + '_';
			if ( namedParamsCopy.put( alias, new TypedValue( type, iter.next() ) ) != null ) {
				throw new HibernateException( "Repeated usage of alias '" + alias + "' while expanding list parameter." );
			}
			list.append( ParserHelper.HQL_VARIABLE_PREFIX ).append( alias );
			if ( iter.hasNext() ) {
				list.append( ", " );
			}
		}
		return StringHelper.replace(
				beforePlaceholder,
				afterPlaceholder,
				placeholder.toString(),
				list.toString(),
				true,
				true
		);
	}

	public Query setParameterList(String name, Collection vals) throws HibernateException {
		if ( vals == null ) {
			throw new QueryException( "Collection must be not null!" );
		}

		if( vals.size() == 0 ) {
			setParameterList( name, vals, null );
		}
		else {
			setParameterList(name, vals, determineType( name, vals.iterator().next() ) );
		}

		return this;
	}

	public Query setParameterList(String name, Object[] vals, Type type) throws HibernateException {
		return setParameterList( name, Arrays.asList(vals), type );
	}

	public Query setParameterList(String name, Object[] values) throws HibernateException {
		return setParameterList( name, Arrays.asList( values ) );
	}

	public Query setProperties(Map map) throws HibernateException {
		String[] params = getNamedParameters();
		for (int i = 0; i < params.length; i++) {
			String namedParam = params[i];
				final Object object = map.get(namedParam);
				if(object==null) {
					continue;
				}
				Class retType = object.getClass();
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( namedParam, ( Collection ) object );
				}
				else if ( retType.isArray() ) {
					setParameterList( namedParam, ( Object[] ) object );
				}
				else {
					setParameter( namedParam, object, determineType( namedParam, retType ) );
				}

			
		}
		return this;				
	}
	
	public Query setProperties(Object bean) throws HibernateException {
		Class clazz = bean.getClass();
		String[] params = getNamedParameters();
		for (int i = 0; i < params.length; i++) {
			String namedParam = params[i];
			try {
				Getter getter = ReflectHelper.getGetter( clazz, namedParam );
				Class retType = getter.getReturnType();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom( retType ) ) {
					setParameterList( namedParam, ( Collection ) object );
				}
				else if ( retType.isArray() ) {
				 	setParameterList( namedParam, ( Object[] ) object );
				}
				else {
					setParameter( namedParam, object, determineType( namedParam, retType ) );
				}
			}
			catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		return this;
	}

	public Query setParameters(Object[] values, Type[] types) {
		this.values = Arrays.asList(values);
		this.types = Arrays.asList(types);
		return this;
	}


	// Execution methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Object uniqueResult() throws HibernateException {
		return uniqueElement( list() );
	}

	static Object uniqueElement(List list) throws NonUniqueResultException {
		int size = list.size();
		if (size==0) return null;
		Object first = list.get(0);
		for ( int i=1; i<size; i++ ) {
			if ( list.get(i)!=first ) {
				throw new NonUniqueResultException( list.size() );
			}
		}
		return first;
	}

	protected RowSelection getRowSelection() {
		return selection;
	}

	public Type[] typeArray() {
		return ArrayHelper.toTypeArray( getTypes() );
	}
	
	public Object[] valueArray() {
		return getValues().toArray();
	}

	public QueryParameters getQueryParameters(Map namedParams) {
		QueryParameters queryParameters = new QueryParameters(
				typeArray(),
				valueArray(),
				namedParams,
				getLockOptions(),
				getRowSelection(),
				true,
				isReadOnly(),
				cacheable,
				cacheRegion,
				comment,
				queryHints,
				collectionKey == null ? null : new Serializable[] { collectionKey },
				optionalObject,
				optionalEntityName,
				optionalId,
				resultTransformer
		);
		queryParameters.setQueryPlan( queryPlan );
		return queryParameters;
	}
	
	protected void before() {
		if ( flushMode!=null ) {
			sessionFlushMode = getSession().getFlushMode();
			getSession().setFlushMode(flushMode);
		}
		if ( cacheMode!=null ) {
			sessionCacheMode = getSession().getCacheMode();
			getSession().setCacheMode(cacheMode);
		}
	}
	
	protected void after() {
		if (sessionFlushMode!=null) {
			getSession().setFlushMode(sessionFlushMode);
			sessionFlushMode = null;
		}
		if (sessionCacheMode!=null) {
			getSession().setCacheMode(sessionCacheMode);
			sessionCacheMode = null;
		}
	}

	public HQLQueryPlan getQueryPlan() {
		return queryPlan;
	}

	public void setQueryPlan(HQLQueryPlan queryPlan) {
		this.queryPlan = queryPlan;
	}
}
