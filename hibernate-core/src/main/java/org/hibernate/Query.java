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
package org.hibernate;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * An object-oriented representation of a Hibernate query. A <tt>Query</tt>
 * instance is obtained by calling <tt>Session.createQuery()</tt>. This
 * interface exposes some extra functionality beyond that provided by
 * <tt>Session.iterate()</tt> and <tt>Session.find()</tt>:
 * <ul>
 * <li>a particular page of the result set may be selected by calling <tt>
 * setMaxResults(), setFirstResult()</tt>
 * <li>named query parameters may be used
 * <li>the results may be returned as an instance of <tt>ScrollableResults</tt>
 * </ul>
 * <br>
 * Named query parameters are tokens of the form <tt>:name</tt> in the
 * query string. A value is bound to the <tt>integer</tt> parameter
 * <tt>:foo</tt> by calling<br>
 * <br>
 * <tt>setParameter("foo", foo, Hibernate.INTEGER);</tt><br>
 * <br>
 * for example. A name may appear multiple times in the query string.<br>
 * <br>
 * JDBC-style <tt>?</tt> parameters are also supported. To bind a
 * value to a JDBC-style parameter use a set method that accepts an
 * <tt>int</tt> positional argument (numbered from zero, contrary
 * to JDBC).<br>
 * <br>
 * You may not mix and match JDBC-style parameters and named parameters
 * in the same query.<br>
 * <br>
 * Queries are executed by calling <tt>list()</tt>, <tt>scroll()</tt> or
 * <tt>iterate()</tt>. A query may be re-executed by subsequent invocations.
 * Its lifespan is, however, bounded by the lifespan of the <tt>Session</tt>
 * that created it.<br>
 * <br>
 * Implementors are not intended to be threadsafe.
 *
 * @see org.hibernate.Session#createQuery(java.lang.String)
 * @see org.hibernate.ScrollableResults
 *
 * @author Gavin King
 */
public interface Query {
	/**
	 * Get the query string.
	 *
	 * @return the query string
	 */
	public String getQueryString();

	/**
	 * Obtains the limit set on the maximum number of rows to retrieve.  No set limit means there is no limit set
	 * on the number of rows returned.  Technically both {@code null} and any negative values are interpreted as no
	 * limit; however, this method should always return null in such case.
	 *
	 * @return The
	 */
	public Integer getMaxResults();

	/**
	 * Set the maximum number of rows to retrieve.
	 *
	 * @param maxResults the maximum number of rows
	 *
	 * @see #getMaxResults()
	 */
	public Query setMaxResults(int maxResults);

	/**
	 * Obtain the value specified (if any) for the first row to be returned from the query results; zero-based.  Used,
	 * in conjunction with {@link #getMaxResults()} in "paginated queries".  No value specified means the first result
	 * is returned.  Zero and negative numbers are the same as no setting.
	 *
	 * @return The first result number.
	 */
	public Integer getFirstResult();

	/**
	 * Set the first row to retrieve.
	 *
	 * @param firstResult a row number, numbered from <tt>0</tt>
	 *
	 * @see #getFirstResult()
	 */
	public Query setFirstResult(int firstResult);

	/**
	 * Obtain the FlushMode in effect for this query.  By default, the query inherits the FlushMode of the Session
	 * from which is originates.
	 *
	 * @return The query FlushMode.
	 *
	 * @see Session#getFlushMode()
	 * @see FlushMode
	 */
	public FlushMode getFlushMode();

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @see #getFlushMode()
	 */
	public Query setFlushMode(FlushMode flushMode);

	/**
	 * Obtain the CacheMode in effect for this query.  By default, the query inherits the CacheMode of the Session
	 * from which is originates.
	 *
	 * NOTE: The CacheMode here only effects reading/writing of the query cache, not the
	 * entity/collection caches.
	 *
	 * @return The query CacheMode.
	 *
	 * @see Session#getCacheMode()
	 * @see CacheMode
	 */
	public CacheMode getCacheMode();

	/**
	 * (Re)set the current CacheMode in effect for this query.
	 *
	 * @param cacheMode The new CacheMode to use.
	 *
	 * @see #getCacheMode()
	 */
	public Query setCacheMode(CacheMode cacheMode);

	/**
	 * Are the results of this query eligible for second level query caching?  This is different that second level
	 * caching of any returned entities and collections.
	 *
	 * NOTE: the query being "eligible" for caching does not necessarily mean its results will be cached.  Second level
	 * query caching still has to be enabled on the {@link SessionFactory} for this to happen.  Usually that is
	 * controlled by the {@code hibernate.cache.use_query_cache} configuration setting.
	 *
	 * @return {@code true} if the query results are eligible for caching, {@code false} otherwise.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_QUERY_CACHE
	 */
	public boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @param cacheable Should the query results be cacheable?
	 *
	 * @see #isCacheable
	 */
	public Query setCacheable(boolean cacheable);

	/**
	 * Obtain the name of the second level query cache region in which query results will be stored (if they are
	 * cached, see the discussion on {@link #isCacheable()} for more information).  {@code null} indicates that the
	 * default region should be used.
	 *
	 * @return The specified cache region name into which query results should be placed; {@code null} indicates
	 * the default region.
	 */
	public String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached (if cached at all).
	 *
	 * @param cacheRegion the name of a query cache region, or {@code null} to indicate that the default region
	 * should be used.
	 *
	 * @see #getCacheRegion()
	 */
	public Query setCacheRegion(String cacheRegion);

	/**
	 * Obtain the query timeout <b>in seconds</b>.  This value is eventually passed along to the JDBC query via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.  Zero indicates no timeout.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	public Integer getTimeout();

	/**
	 * Set the query timeout <b>in seconds</b>.
	 *
	 * NOTE it is important to understand that any value set here is eventually passed directly through to the JDBC
	 * Statement which expressly disallows negative values.  So negative values should be avoided as a general rule.
	 *
	 * @param timeout the timeout <b>in seconds</b>
	 *
	 * @see #getTimeout()
	 */
	public Query setTimeout(int timeout);

	/**
	 * Obtain the JDBC fetch size hint in effect for this query.  This value is eventually passed along to the JDBC
	 * query via {@link java.sql.Statement#setFetchSize(int)}.  As defined b y JDBC, this value is a hint to the
	 * driver to indicate how many rows to fetch from the database when more rows are needed.
	 *
	 * NOTE : JDBC expressly defines this value as a hint.  It may or may not have any effect on the actual
	 * query execution and ResultSet processing depending on the driver.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getFetchSize()
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	public Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @see #getFetchSize()
	 */
	public Query setFetchSize(int fetchSize);

	/**
	 * Obtains the LockOptions in effect for this query.
	 *
	 * @return The LockOptions
	 *
	 * @see LockOptions
	 */
	public LockOptions getLockOptions();

	/**
	 * Set the lock options for the query.  Specifically only the following are taken into consideration:<ol>
	 *     <li>{@link LockOptions#getLockMode()}</li>
	 *     <li>{@link LockOptions#getScope()}</li>
	 *     <li>{@link LockOptions#getTimeOut()}</li>
	 * </ol>
	 * For alias-specific locking, use {@link #setLockMode(String, LockMode)}.
	 *
	 * @see #getLockOptions()
	 */
	public Query setLockOptions(LockOptions lockOptions);

	/**
	 * Set the LockMode to use for specific alias (as defined in the query's <tt>FROM</tt> clause).
	 *
	 * The alias-specific lock modes specified here are added to the query's internal
	 * {@link #getLockOptions() LockOptions}.
	 *
	 * The effect of these alias-specific LockModes is somewhat dependent on the driver/database in use.  Generally
	 * speaking, for maximum portability, this method should only be used to mark that the rows corresponding to
	 * the given alias should be included in pessimistic locking ({@link LockMode#PESSIMISTIC_WRITE}).
	 *
	 * @param alias a query alias, or <tt>this</tt> for a collection filter
	 *
	 * @see #getLockOptions()
	 */
	public Query setLockMode(String alias, LockMode lockMode);

	/**
	 * Obtain the comment currently associated with this query.  Provided SQL commenting is enabled
	 * (generally by enabling the {@code hibernate.use_sql_comments} config setting), this comment will also be added
	 * to the SQL query sent to the database.  Often useful for identifying the source of troublesome queries on the
	 * database side.
	 *
	 * @return The comment.
	 */
	public String getComment();

	/**
	 * Set the comment for this query.
	 *
	 * @param comment The human-readable comment
	 *
	 * @see #getComment()
	 */
	public Query setComment(String comment);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only mode? If the
	 * read-only/modifiable setting was not initialized, then the default
	 * read-only/modifiable setting for the persistence context is returned instead.
	 * @see Query#setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session before the query was executed.
	 *
	 * @return true, entities and proxies loaded by the query will be put in read-only mode
	 *         false, entities and proxies loaded by the query will be put in modifiable mode
	 */
	public boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies
	 * loaded by this Query. This setting overrides the default setting
	 * for the persistence context.
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.engine.spi.PersistenceContext#setDefaultReadOnly(boolean)
	 * @see org.hibernate.Session#setDefaultReadOnly(boolean)
	 *
	 * Read-only entities are not dirty-checked and snapshots of persistent
	 * state are not maintained. Read-only entities can be modified, but
	 * changes are not persisted.
	 *
	 * When a proxy is initialized, the loaded entity will have the same
	 * read-only/modifiable setting as the uninitialized
	 * proxy has, regardless of the session's current setting.
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies
	 * returned by the query that existed in the session before the query was executed.
	 *
	 * @param readOnly true, entities and proxies loaded by the query will be put in read-only mode
	 *                 false, entities and proxies loaded by the query will be put in modifiable mode
	 */
	public Query setReadOnly(boolean readOnly);

	/**
	 * Return the Hibernate types of the query result set.
	 * @return an array of types
	 */
	public Type[] getReturnTypes() throws HibernateException;

	/**
	 * Return the HQL select clause aliases (if any)
	 * @return an array of aliases as strings
	 */
	public String[] getReturnAliases() throws HibernateException;

	/**
	 * Return the names of all named parameters of the query.
	 * @return the parameter names, in no particular order
	 */
	public String[] getNamedParameters() throws HibernateException;

	/**
	 * Return the query results as an <tt>Iterator</tt>. If the query
	 * contains multiple results pre row, the results are returned in
	 * an instance of <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first
	 * SQL query returns identifiers only.<br>
	 *
	 * @return the result iterator
	 * @throws HibernateException
	 */
	public Iterator iterate() throws HibernateException;

	/**
	 * Return the query results as <tt>ScrollableResults</tt>. The
	 * scrollability of the returned results depends upon JDBC driver
	 * support for scrollable <tt>ResultSet</tt>s.<br>
	 *
	 * @see ScrollableResults
	 * @return the result iterator
	 * @throws HibernateException
	 */
	public ScrollableResults scroll() throws HibernateException;

	/**
	 * Return the query results as <tt>ScrollableResults</tt>. The
	 * scrollability of the returned results depends upon JDBC driver
	 * support for scrollable <tt>ResultSet</tt>s.<br>
	 *
	 * @see ScrollableResults
	 * @see ScrollMode
	 * @return the result iterator
	 * @throws HibernateException
	 */
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException;

	/**
	 * Return the query results as a <tt>List</tt>. If the query contains
	 * multiple results pre row, the results are returned in an instance
	 * of <tt>Object[]</tt>.
	 *
	 * @return the result list
	 * @throws HibernateException
	 */
	public List list() throws HibernateException;

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or null if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	public Object uniqueResult() throws HibernateException;

	/**
	 * Execute the update or delete statement.
	 * </p>
	 * The semantics are compliant with the ejb3 Query.executeUpdate()
	 * method.
	 *
	 * @return The number of entities updated or deleted.
	 * @throws HibernateException
	 */
	public int executeUpdate() throws HibernateException;

	/**
	 * Bind a value to a JDBC-style query parameter.
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 */
	public Query setParameter(int position, Object val, Type type);

	/**
	 * Bind a value to a named query parameter.
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 */
	public Query setParameter(String name, Object val, Type type);

	/**
	 * Bind a value to a JDBC-style query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the given object.
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the non-null parameter value
	 * @throws org.hibernate.HibernateException if no type could be determined
	 */
	public Query setParameter(int position, Object val) throws HibernateException;

	/**
	 * Bind a value to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the given object.
	 * @param name the name of the parameter
	 * @param val the non-null parameter value
	 * @throws org.hibernate.HibernateException if no type could be determined
	 */
	public Query setParameter(String name, Object val) throws HibernateException;
	
	/**
	 * Bind values and types to positional parameters.
	 */
	public Query setParameters(Object[] values, Type[] types) throws HibernateException;

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 * @param name the name of the parameter
	 * @param vals a collection of values to list
	 * @param type the Hibernate type of the values
	 */
	public Query setParameterList(String name, Collection vals, Type type) throws HibernateException;

	/**
	 * Bind multiple values to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the first object in the collection. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 * @param name the name of the parameter
	 * @param vals a collection of values to list
	 */
	public Query setParameterList(String name, Collection vals) throws HibernateException;

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 * @param name the name of the parameter
	 * @param vals a collection of values to list
	 * @param type the Hibernate type of the values
	 */
	public Query setParameterList(String name, Object[] vals, Type type) throws HibernateException;

	/**
	 * Bind multiple values to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the first object in the array. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 * @param name the name of the parameter
	 * @param vals a collection of values to list
	 */
	public Query setParameterList(String name, Object[] vals) throws HibernateException;

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using hueristics.
	 * @param bean any JavaBean or POJO
	 */	
	public Query setProperties(Object bean) throws HibernateException;
	
	/**
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using hueristics.
	 * @param bean a java.util.Map
	 */
	public Query setProperties(Map bean) throws HibernateException;

	public Query setString(int position, String val);
	public Query setCharacter(int position, char val);
	public Query setBoolean(int position, boolean val);
	public Query setByte(int position, byte val);
	public Query setShort(int position, short val);
	public Query setInteger(int position, int val);
	public Query setLong(int position, long val);
	public Query setFloat(int position, float val);
	public Query setDouble(int position, double val);
	public Query setBinary(int position, byte[] val);
	public Query setText(int position, String val);
	public Query setSerializable(int position, Serializable val);
	public Query setLocale(int position, Locale locale);
	public Query setBigDecimal(int position, BigDecimal number);
	public Query setBigInteger(int position, BigInteger number);

	public Query setDate(int position, Date date);
	public Query setTime(int position, Date date);
	public Query setTimestamp(int position, Date date);

	public Query setCalendar(int position, Calendar calendar);
	public Query setCalendarDate(int position, Calendar calendar);

	public Query setString(String name, String val);
	public Query setCharacter(String name, char val);
	public Query setBoolean(String name, boolean val);
	public Query setByte(String name, byte val);
	public Query setShort(String name, short val);
	public Query setInteger(String name, int val);
	public Query setLong(String name, long val);
	public Query setFloat(String name, float val);
	public Query setDouble(String name, double val);
	public Query setBinary(String name, byte[] val);
	public Query setText(String name, String val);
	public Query setSerializable(String name, Serializable val);
	public Query setLocale(String name, Locale locale);
	public Query setBigDecimal(String name, BigDecimal number);
	public Query setBigInteger(String name, BigInteger number);

        /**
         * Bind the date (time is truncated) of a given Date object to a named query parameter.
         * 
	 * @param name The name of the parameter
	 * @param date The date object
         */
	public Query setDate(String name, Date date);

        /**
         * Bind the time (date is truncated) of a given Date object to a named query parameter.
         * 
	 * @param name The name of the parameter
	 * @param date The date object
         */
	public Query setTime(String name, Date date);

        /**
         * Bind the date and the time of a given Date object to a named query parameter.
         *
	 * @param name The name of the parameter
	 * @param date The date object
         */
	public Query setTimestamp(String name, Date date);

	public Query setCalendar(String name, Calendar calendar);
	public Query setCalendarDate(String name, Calendar calendar);

	/**
	 * Bind an instance of a mapped persistent class to a JDBC-style query parameter.
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val a non-null instance of a persistent class
	 */
	public Query setEntity(int position, Object val); // use setParameter for null values

	/**
	 * Bind an instance of a mapped persistent class to a named query parameter.
	 * @param name the name of the parameter
	 * @param val a non-null instance of a persistent class
	 */
	public Query setEntity(String name, Object val); // use setParameter for null values
	
	
	/**
	 * Set a strategy for handling the query results. This can be used to change
	 * "shape" of the query result.
	 *
	 * @param transformer The transformer to apply
	 * @return this (for method chaining)	
	 */
	public Query setResultTransformer(ResultTransformer transformer);

}







