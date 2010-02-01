/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.impl;

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
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.LockOptions;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.hql.classic.ParserHelper;
import org.hibernate.property.Getter;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.MarkerObject;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Abstract implementation of the Query interface.
 *
 * @author Gavin King
 * @author Max Andersen
 */
public abstract class AbstractQueryImpl implements Query {

	private static final Object UNSET_PARAMETER = new MarkerObject("<unset parameter>");
	private static final Object UNSET_TYPE = new MarkerObject("<unset type>");

	private final String queryString;
	protected final SessionImplementor session;
	protected final ParameterMetadata parameterMetadata;

	// parameter bind values...
	private List values = new ArrayList(4);
	private List types = new ArrayList(4);
	private Map namedParameters = new HashMap(4);
	private Map namedParameterLists = new HashMap(4);

	private Object optionalObject;
	private Serializable optionalId;
	private String optionalEntityName;

	private RowSelection selection;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private FlushMode flushMode;
	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;
	private Serializable collectionKey;
	private Boolean readOnly;
	private ResultTransformer resultTransformer;

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

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + queryString + ')';
	}

	public final String getQueryString() {
		return queryString;
	}

	//TODO: maybe call it getRowSelection() ?
	public RowSelection getSelection() {
		return selection;
	}
	
	public Query setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}
	
	public Query setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}

	public Query setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public Query setCacheRegion(String cacheRegion) {
		if (cacheRegion != null)
			this.cacheRegion = cacheRegion.trim();
		return this;
	}

	public Query setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Query setFirstResult(int firstResult) {
		selection.setFirstRow( new Integer(firstResult) );
		return this;
	}

	public Query setMaxResults(int maxResults) {
		if ( maxResults < 0 ) {
			// treat negatives specically as meaning no limit...
			selection.setMaxRows( null );
		}
		else {
			selection.setMaxRows( new Integer(maxResults) );
		}
		return this;
	}

	public Query setTimeout(int timeout) {
		selection.setTimeout( new Integer(timeout) );
		return this;
	}
	public Query setFetchSize(int fetchSize) {
		selection.setFetchSize( new Integer(fetchSize) );
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

	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnly() {
		return ( readOnly == null ?
				getSession().getPersistenceContext().isDefaultReadOnly() :
				readOnly.booleanValue() 
		);
	}

	/**
	 * {@inheritDoc}
	 */
	public Query setReadOnly(boolean readOnly) {
		this.readOnly = Boolean.valueOf( readOnly );
		return this;
	}

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

	public abstract LockOptions getLockOptions();


	// Parameter handling code ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns a shallow copy of the named parameter value map.
	 *
	 * @return Shallow copy of the named parameter value map
	 */
	protected Map getNamedParams() {
		return new HashMap( namedParameters );
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
	protected Map getNamedParameterLists() {
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
			Set missingParams = new HashSet( parameterMetadata.getNamedParameterNames() );
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
			 namedParameters.put( name, new TypedValue( type, val, session.getEntityMode() ) );
			 return this;
		}
	}

	public Query setParameter(int position, Object val) throws HibernateException {
		if (val == null) {
			setParameter( position, val, Hibernate.SERIALIZABLE );
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
				type = Hibernate.SERIALIZABLE;
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
		Type type = TypeFactory.heuristicType(typename);
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
			return Hibernate.entity(clazz);
		}
		else {
			return type;
		}
	}

	public Query setString(int position, String val) {
		setParameter(position, val, Hibernate.STRING);
		return this;
	}

	public Query setCharacter(int position, char val) {
		setParameter(position, new Character(val), Hibernate.CHARACTER);
		return this;
	}

	public Query setBoolean(int position, boolean val) {
		Boolean valueToUse = val ? Boolean.TRUE : Boolean.FALSE;
		Type typeToUse = determineType( position, valueToUse, Hibernate.BOOLEAN );
		setParameter( position, valueToUse, typeToUse );
		return this;
	}

	public Query setByte(int position, byte val) {
		setParameter(position, new Byte(val), Hibernate.BYTE);
		return this;
	}

	public Query setShort(int position, short val) {
		setParameter(position, new Short(val), Hibernate.SHORT);
		return this;
	}

	public Query setInteger(int position, int val) {
		setParameter(position, new Integer(val), Hibernate.INTEGER);
		return this;
	}

	public Query setLong(int position, long val) {
		setParameter(position, new Long(val), Hibernate.LONG);
		return this;
	}

	public Query setFloat(int position, float val) {
		setParameter(position, new Float(val), Hibernate.FLOAT);
		return this;
	}

	public Query setDouble(int position, double val) {
		setParameter(position, new Double(val), Hibernate.DOUBLE);
		return this;
	}

	public Query setBinary(int position, byte[] val) {
		setParameter(position, val, Hibernate.BINARY);
		return this;
	}

	public Query setText(int position, String val) {
		setParameter(position, val, Hibernate.TEXT);
		return this;
	}

	public Query setSerializable(int position, Serializable val) {
		setParameter(position, val, Hibernate.SERIALIZABLE);
		return this;
	}

	public Query setDate(int position, Date date) {
		setParameter(position, date, Hibernate.DATE);
		return this;
	}

	public Query setTime(int position, Date date) {
		setParameter(position, date, Hibernate.TIME);
		return this;
	}

	public Query setTimestamp(int position, Date date) {
		setParameter(position, date, Hibernate.TIMESTAMP);
		return this;
	}

	public Query setEntity(int position, Object val) {
		setParameter( position, val, Hibernate.entity( resolveEntityName( val ) ) );
		return this;
	}

	private String resolveEntityName(Object val) {
		if ( val == null ) {
			throw new IllegalArgumentException( "entity for parameter binding cannot be null" );
		}
		return session.bestGuessEntityName( val );
	}

	public Query setLocale(int position, Locale locale) {
		setParameter(position, locale, Hibernate.LOCALE);
		return this;
	}

	public Query setCalendar(int position, Calendar calendar) {
		setParameter(position, calendar, Hibernate.CALENDAR);
		return this;
	}

	public Query setCalendarDate(int position, Calendar calendar) {
		setParameter(position, calendar, Hibernate.CALENDAR_DATE);
		return this;
	}

	public Query setBinary(String name, byte[] val) {
		setParameter(name, val, Hibernate.BINARY);
		return this;
	}

	public Query setText(String name, String val) {
		setParameter(name, val, Hibernate.TEXT);
		return this;
	}

	public Query setBoolean(String name, boolean val) {
		Boolean valueToUse = val ? Boolean.TRUE : Boolean.FALSE;
		Type typeToUse = determineType( name, valueToUse, Hibernate.BOOLEAN );
		setParameter( name, valueToUse, typeToUse );
		return this;
	}

	public Query setByte(String name, byte val) {
		setParameter(name, new Byte(val), Hibernate.BYTE);
		return this;
	}

	public Query setCharacter(String name, char val) {
		setParameter(name, new Character(val), Hibernate.CHARACTER);
		return this;
	}

	public Query setDate(String name, Date date) {
		setParameter(name, date, Hibernate.DATE);
		return this;
	}

	public Query setDouble(String name, double val) {
		setParameter(name, new Double(val), Hibernate.DOUBLE);
		return this;
	}

	public Query setEntity(String name, Object val) {
		setParameter( name, val, Hibernate.entity( resolveEntityName( val ) ) );
		return this;
	}

	public Query setFloat(String name, float val) {
		setParameter(name, new Float(val), Hibernate.FLOAT);
		return this;
	}

	public Query setInteger(String name, int val) {
		setParameter(name, new Integer(val), Hibernate.INTEGER);
		return this;
	}

	public Query setLocale(String name, Locale locale) {
		setParameter(name, locale, Hibernate.LOCALE);
		return this;
	}

	public Query setCalendar(String name, Calendar calendar) {
		setParameter(name, calendar, Hibernate.CALENDAR);
		return this;
	}

	public Query setCalendarDate(String name, Calendar calendar) {
		setParameter(name, calendar, Hibernate.CALENDAR_DATE);
		return this;
	}

	public Query setLong(String name, long val) {
		setParameter(name, new Long(val), Hibernate.LONG);
		return this;
	}

	public Query setSerializable(String name, Serializable val) {
		setParameter(name, val, Hibernate.SERIALIZABLE);
		return this;
	}

	public Query setShort(String name, short val) {
		setParameter(name, new Short(val), Hibernate.SHORT);
		return this;
	}

	public Query setString(String name, String val) {
		setParameter(name, val, Hibernate.STRING);
		return this;
	}

	public Query setTime(String name, Date date) {
		setParameter(name, date, Hibernate.TIME);
		return this;
	}

	public Query setTimestamp(String name, Date date) {
		setParameter(name, date, Hibernate.TIMESTAMP);
		return this;
	}

	public Query setBigDecimal(int position, BigDecimal number) {
		setParameter(position, number, Hibernate.BIG_DECIMAL);
		return this;
	}

	public Query setBigDecimal(String name, BigDecimal number) {
		setParameter(name, number, Hibernate.BIG_DECIMAL);
		return this;
	}

	public Query setBigInteger(int position, BigInteger number) {
		setParameter(position, number, Hibernate.BIG_INTEGER);
		return this;
	}

	public Query setBigInteger(String name, BigInteger number) {
		setParameter(name, number, Hibernate.BIG_INTEGER);
		return this;
	}

	public Query setParameterList(String name, Collection vals, Type type) throws HibernateException {
		if ( !parameterMetadata.getNamedParameterNames().contains( name ) ) {
			throw new IllegalArgumentException("Parameter " + name + " does not exist as a named parameter in [" + getQueryString() + "]");
		}
		namedParameterLists.put( name, new TypedValue( type, vals, session.getEntityMode() ) );
		return this;
	}
	
	/**
	 * Warning: adds new parameters to the argument by side-effect, as well as
	 * mutating the query string!
	 */
	protected String expandParameterLists(Map namedParamsCopy) {
		String query = this.queryString;
		Iterator iter = namedParameterLists.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
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
		Type type = typedList.getType();
		if ( vals.size() == 1 ) {
			// short-circuit for performance...
			namedParamsCopy.put( name, new TypedValue( type, vals.iterator().next(), session.getEntityMode() ) );
			return query;
		}

		StringBuffer list = new StringBuffer( 16 );
		Iterator iter = vals.iterator();
		int i = 0;
		boolean isJpaPositionalParam = parameterMetadata.getNamedParameterDescriptor( name ).isJpaStyle();
		while ( iter.hasNext() ) {
			String alias = ( isJpaPositionalParam ? 'x' + name : name ) + i++ + '_';
			namedParamsCopy.put( alias, new TypedValue( type, iter.next(), session.getEntityMode() ) );
			list.append( ParserHelper.HQL_VARIABLE_PREFIX ).append( alias );
			if ( iter.hasNext() ) {
				list.append( ", " );
			}
		}
		String paramPrefix = isJpaPositionalParam ? "?" : ParserHelper.HQL_VARIABLE_PREFIX;
		return StringHelper.replace( query, paramPrefix + name, list.toString(), true );
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

	public Query setParameterList(String name, Object[] vals) throws HibernateException {
		return setParameterList( name, Arrays.asList(vals) );
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
		return new QueryParameters(
				typeArray(),
				valueArray(),
				namedParams,
				getLockOptions(),
				getSelection(),
				true,
				isReadOnly(),
				cacheable,
				cacheRegion,
				comment,
				collectionKey == null ? null : new Serializable[] { collectionKey },
				optionalObject,
				optionalEntityName,
				optionalId,
				resultTransformer
		);
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
}
