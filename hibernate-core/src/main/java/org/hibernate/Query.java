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
@SuppressWarnings("UnusedDeclaration")
public interface Query extends BasicQueryContract {
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
	 * @return {@code this}, for method chaining
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
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFirstResult()
	 */
	public Query setFirstResult(int firstResult);

	@Override
	public Query setFlushMode(FlushMode flushMode);

	@Override
	public Query setCacheMode(CacheMode cacheMode);

	@Override
	public Query setCacheable(boolean cacheable);

	@Override
	public Query setCacheRegion(String cacheRegion);

	@Override
	public Query setTimeout(int timeout);

	@Override
	public Query setFetchSize(int fetchSize);

	@Override
	public Query setReadOnly(boolean readOnly);

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
	 * @param lockOptions The lock options to apply to the query.
	 *
	 * @return {@code this}, for method chaining
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
	 * @param alias a query alias, or {@code "this"} for a collection filter
	 * @param lockMode The lock mode to apply.
	 *
	 * @return {@code this}, for method chaining
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
	 * @return {@code this}, for method chaining
	 *
	 * @see #getComment()
	 */
	public Query setComment(String comment);
	
	/**
	 * Add a DB query hint to the SQL.  These differ from JPA's {@link javax.persistence.QueryHint}, which is specific
	 * to the JPA implementation and ignores DB vendor-specific hints.  Instead, these are intended solely for the
	 * vendor-specific hints, such as Oracle's optimizers.  Multiple query hints are supported; the Dialect will
	 * determine concatenation and placement.
	 * 
	 * @param hint The database specific query hint to add.
	 */
	public Query addQueryHint(String hint);

	/**
	 * Return the HQL select clause aliases, if any.
	 *
	 * @return an array of aliases as strings
	 */
	public String[] getReturnAliases();

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names, in no particular order
	 */
	public String[] getNamedParameters();

	/**
	 * Return the query results as an <tt>Iterator</tt>. If the query
	 * contains multiple results pre row, the results are returned in
	 * an instance of <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first
	 * SQL query returns identifiers only.<br>
	 *
	 * @return the result iterator
	 */
	public Iterator iterate();

	/**
	 * Return the query results as <tt>ScrollableResults</tt>. The
	 * scrollability of the returned results depends upon JDBC driver
	 * support for scrollable <tt>ResultSet</tt>s.<br>
	 *
	 * @see ScrollableResults
	 *
	 * @return the result iterator
	 */
	public ScrollableResults scroll();

	/**
	 * Return the query results as ScrollableResults. The scrollability of the returned results
	 * depends upon JDBC driver support for scrollable ResultSets.
	 *
	 * @param scrollMode The scroll mode
	 *
	 * @return the result iterator
	 *
	 * @see ScrollableResults
	 * @see ScrollMode
	 *
	 */
	public ScrollableResults scroll(ScrollMode scrollMode);

	/**
	 * Return the query results as a <tt>List</tt>. If the query contains
	 * multiple results per row, the results are returned in an instance
	 * of <tt>Object[]</tt>.
	 *
	 * @return the result list
	 */
	public List list();

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or null if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	public Object uniqueResult();

	/**
	 * Execute the update or delete statement.
	 *
	 * The semantics are compliant with the ejb3 Query.executeUpdate() method.
	 *
	 * @return The number of entities updated or deleted.
	 */
	public int executeUpdate();

	/**
	 * Bind a value to a JDBC-style query parameter.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameter(int position, Object val, Type type);

	/**
	 * Bind a value to a named query parameter.
	 *
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameter(String name, Object val, Type type);

	/**
	 * Bind a value to a JDBC-style query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the given object.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the non-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameter(int position, Object val);

	/**
	 * Bind a value to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the given object.
	 *
	 * @param name the name of the parameter
	 * @param val the non-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameter(String name, Object val);
	
	/**
	 * Bind values and types to positional parameters.  Allows binding more than one at a time; no real performance
	 * impact.
	 *
	 * The number of elements in each array should match.  That is, element number-0 in types array corresponds to
	 * element-0 in the values array, etc,
	 *
	 * @param types The types
	 * @param values The values
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameters(Object[] values, Type[] types);

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameterList(String name, Collection values, Type type);

	/**
	 * Bind multiple values to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the first object in the collection. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameterList(String name, Collection values);

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameterList(String name, Object[] values, Type type);

	/**
	 * Bind multiple values to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly 
	 * guessed from the class of the first object in the array. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setParameterList(String name, Object[] values);

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */	
	public Query setProperties(Object bean);
	
	/**
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean a java.util.Map
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setProperties(Map bean);

	/**
	 * Bind a positional String-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setString(int position, String val);

	/**
	 * Bind a positional char-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCharacter(int position, char val);

	/**
	 * Bind a positional boolean-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBoolean(int position, boolean val);

	/**
	 * Bind a positional byte-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setByte(int position, byte val);

	/**
	 * Bind a positional short-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setShort(int position, short val);

	/**
	 * Bind a positional int-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setInteger(int position, int val);

	/**
	 * Bind a positional long-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setLong(int position, long val);

	/**
	 * Bind a positional float-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setFloat(int position, float val);

	/**
	 * Bind a positional double-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setDouble(int position, double val);

	/**
	 * Bind a positional binary-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBinary(int position, byte[] val);

	/**
	 * Bind a positional String-valued parameter using streaming.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setText(int position, String val);

	/**
	 * Bind a positional binary-valued parameter using serialization.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setSerializable(int position, Serializable val);

	/**
	 * Bind a positional Locale-valued parameter.
	 *
	 * @param position The parameter position
	 * @param locale The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setLocale(int position, Locale locale);

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBigDecimal(int position, BigDecimal number);

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBigInteger(int position, BigInteger number);

	/**
	 * Bind a positional Date-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setDate(int position, Date date);

	/**
	 * Bind a positional Date-valued parameter using just the Time portion.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setTime(int position, Date date);

	/**
	 * Bind a positional Date-valued parameter using the full Timestamp.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setTimestamp(int position, Date date);

	/**
	 * Bind a positional Calendar-valued parameter using the full Timestamp portion.
	 *
	 * @param position The parameter position
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCalendar(int position, Calendar calendar);

	/**
	 * Bind a positional Calendar-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCalendarDate(int position, Calendar calendar);

	/**
	 * Bind a named String-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setString(String name, String val);

	/**
	 * Bind a named char-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCharacter(String name, char val);

	/**
	 * Bind a named boolean-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBoolean(String name, boolean val);

	/**
	 * Bind a named byte-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setByte(String name, byte val);

	/**
	 * Bind a named short-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setShort(String name, short val);

	/**
	 * Bind a named int-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setInteger(String name, int val);

	/**
	 * Bind a named long-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setLong(String name, long val);

	/**
	 * Bind a named float-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setFloat(String name, float val);

	/**
	 * Bind a named double-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setDouble(String name, double val);

	/**
	 * Bind a named binary-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBinary(String name, byte[] val);

	/**
	 * Bind a named String-valued parameter using streaming.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setText(String name, String val);

	/**
	 * Bind a named binary-valued parameter using serialization.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setSerializable(String name, Serializable val);

	/**
	 * Bind a named Locale-valued parameter.
	 *
	 * @param name The parameter name
	 * @param locale The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setLocale(String name, Locale locale);

	/**
	 * Bind a named BigDecimal-valued parameter.
	 *
	 * @param name The parameter name
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBigDecimal(String name, BigDecimal number);

	/**
	 * Bind a named BigInteger-valued parameter.
	 *
	 * @param name The parameter name
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setBigInteger(String name, BigInteger number);

	/**
	 * Bind the date (time is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setDate(String name, Date date);

	/**
	 * Bind the time (date is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setTime(String name, Date date);

	/**
	 * Bind the date and the time of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setTimestamp(String name, Date date);

	/**
	 * Bind a named Calendar-valued parameter using the full Timestamp.
	 *
	 * @param name The parameter name
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCalendar(String name, Calendar calendar);

	/**
	 * Bind a named Calendar-valued parameter using just the Date portion.
	 *
	 * @param name The parameter name
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setCalendarDate(String name, Calendar calendar);

	/**
	 * Bind an instance of a mapped persistent class to a JDBC-style query parameter.
	 * Use {@link #setParameter(int, Object)} for null values.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setEntity(int position, Object val);

	/**
	 * Bind an instance of a mapped persistent class to a named query parameter.  Use
	 * {@link #setParameter(String, Object)} for null values.
	 *
	 * @param name the name of the parameter
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 */
	public Query setEntity(String name, Object val);
	
	
	/**
	 * Set a strategy for handling the query results. This can be used to change
	 * "shape" of the query result.
	 *
	 * @param transformer The transformer to apply
	 * @return this (for method chaining)
	 */
	public Query setResultTransformer(ResultTransformer transformer);

}







