/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

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

import javax.persistence.FlushModeType;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.*;
import org.hibernate.BasicQueryContract;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings("UnusedDeclaration")
public interface Query<Q extends TypedQuery,R> extends BasicQueryContract<Q>, TypedQuery<R> {

	/**
	 * Get the query string.
	 *
	 * @return the query string
	 */
	String getQueryString();

	/**
	 * {@inheritDoc}
	 * <p/>
	 * For users of the Hibernate native APIs, we've had to rename this method
	 * as defined by Hibernate historically because the JPA contract defines a method of the same
	 * name, but returning the JPA {@link FlushModeType} rather than Hibernate's {@link FlushMode}.  For
	 * the former behavior, use {@link Query#getHibernateFlushMode()} instead.
	 *
	 * @return The FlushModeType in effect for this query.
	 */
	@Override
	FlushModeType getFlushMode();

	/**
	 * Set the maximum number of rows to retrieve.
	 *
	 * @param maxResults the maximum number of rows
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getMaxResults()
	 */
	@Override
	@SuppressWarnings("unchecked")
	Q setMaxResults(int maxResults);

	/**
	 * Set the first row to retrieve.
	 *
	 * @param firstResult a row number, numbered from <tt>0</tt>
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFirstResult()
	 */
	@Override
	@SuppressWarnings("unchecked")
	Q setFirstResult(int firstResult);

	/**
	 * Obtains the LockOptions in effect for this query.
	 *
	 * @return The LockOptions
	 *
	 * @see LockOptions
	 */
	LockOptions getLockOptions();

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
	Q setLockOptions(LockOptions lockOptions);

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
	Q setLockMode(String alias, LockMode lockMode);

	/**
	 * Obtain the comment currently associated with this query.  Provided SQL commenting is enabled
	 * (generally by enabling the {@code hibernate.use_sql_comments} config setting), this comment will also be added
	 * to the SQL query sent to the database.  Often useful for identifying the source of troublesome queries on the
	 * database side.
	 *
	 * @return The comment.
	 */
	String getComment();

	/**
	 * Set the comment for this query.
	 *
	 * @param comment The human-readable comment
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getComment()
	 */
	Q setComment(String comment);
	
	/**
	 * Add a DB query hint to the SQL.  These differ from JPA's {@link javax.persistence.QueryHint}, which is specific
	 * to the JPA implementation and ignores DB vendor-specific hints.  Instead, these are intended solely for the
	 * vendor-specific hints, such as Oracle's optimizers.  Multiple query hints are supported; the Dialect will
	 * determine concatenation and placement.
	 * 
	 * @param hint The database specific query hint to add.
	 */
	Q addQueryHint(String hint);

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
	Iterator<R> iterate();

	/**
	 * Return the query results as <tt>ScrollableResults</tt>. The
	 * scrollability of the returned results depends upon JDBC driver
	 * support for scrollable <tt>ResultSet</tt>s.<br>
	 *
	 * @see ScrollableResults
	 *
	 * @return the result iterator
	 */
	ScrollableResults scroll();

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
	ScrollableResults scroll(ScrollMode scrollMode);

	/**
	 * Return the query results as a <tt>List</tt>. If the query contains
	 * multiple results per row, the results are returned in an instance
	 * of <tt>Object[]</tt>.
	 *
	 * @return the result list
	 */
	List<R> list();

	@Override
	default List<R> getResultList() {
		return list();
	}

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or null if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	R uniqueResult();

	@Override
	default R getSingleResult() {
		return uniqueResult();
	}

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names, in no particular order
	 */
	String[] getNamedParameters();

	/**
	 * Bind a query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage context then
	 * use of this form of binding is not allowed, and {@link #setParameter(QueryParameter, Object, Type)}
	 * should be used instead
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> Q setParameter(QueryParameter<T> parameter, T val);

	/**
	 * Bind a named query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage context then
	 * use of this form of binding is not allowed, and {@link #setParameter(String, Object, Type)}
	 * should be used instead
	 *
	 * @param name the parameter name
	 * @param val the (possibly-null) parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings("unchecked")
	Q setParameter(String name, Object val);

	/**
	 * Bind a positional query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage context then
	 * use of this form of binding is not allowed, and {@link #setParameter(int, Object, Type)}
	 * should be used instead
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	@SuppressWarnings("unchecked")
	Q setParameter(int position, Object val);

	/**
	 * Bind a query parameter using the supplied Type
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Q setParameter(QueryParameter<P> parameter, P val, Type type);

	/**
	 * Bind a named query parameter using the supplied Type
	 *
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	Q setParameter(String name, Object val, Type type);

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
	Q setParameter(int position, Object val, Type type);

	/**
	 * Bind a query parameter as some form of date/time using the indicated
	 * temporal-type.
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 * @param temporalType the temporal-type to use in binding the date/time
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Q setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

	/**
	 * Bind a named query parameter as some form of date/time using
	 * the indicated temporal-type.
	 *
	 * @param name the parameter name
	 * @param val the possibly-null parameter value
	 * @param temporalType the temporal-type to use in binding the date/time
	 *
	 * @return {@code this}, for method chaining
	 */
	Q setParameter(String name, Object val, TemporalType temporalType);

	/**
	 * Bind a positional query parameter as some form of date/time using
	 * the indicated temporal-type.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 * @param temporalType the temporal-type to use in binding the date/time
	 *
	 * @return {@code this}, for method chaining
	 */
	Q setParameter(int position, Object val, TemporalType temporalType);





	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo: consider for deprecation
	//
	//		The major concern with parameter lists is "expansion" which is where we need
	// 		to dynamically adjust the query string to include a JDBC parameter placeholder
	// 		for each list value
	//
	//		For the rest, its a question of slimming-down the API

	/**
	 * Bind multiple values to a query parameter using its inferred Type. The Hibernate type of the parameter values is
	 * first detected via the usage/position in the query and if not sufficient secondly
	 * guessed from the class of the first object in the collection. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Q setParameterList(QueryParameter<P> name, Collection<P> values);

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
	Q setParameterList(String name, Collection values);

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
	Q setParameterList(String name, Collection values, Type type);

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
	Query setParameterList(String name, Object[] values, Type type);

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
	Query setParameterList(String name, Object[] values);

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setProperties(Object bean);
	
	/**
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean a java.util.Map
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setProperties(Map bean);

	/**
	 * Bind a positional String-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setString(int position, String val);

	/**
	 * Bind a positional char-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCharacter(int position, char val);

	/**
	 * Bind a positional boolean-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBoolean(int position, boolean val);

	/**
	 * Bind a positional byte-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setByte(int position, byte val);

	/**
	 * Bind a positional short-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setShort(int position, short val);

	/**
	 * Bind a positional int-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setInteger(int position, int val);

	/**
	 * Bind a positional long-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setLong(int position, long val);

	/**
	 * Bind a positional float-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setFloat(int position, float val);

	/**
	 * Bind a positional double-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setDouble(int position, double val);

	/**
	 * Bind a positional binary-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBinary(int position, byte[] val);

	/**
	 * Bind a positional String-valued parameter using streaming.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setText(int position, String val);

	/**
	 * Bind a positional binary-valued parameter using serialization.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setSerializable(int position, Serializable val);

	/**
	 * Bind a positional Locale-valued parameter.
	 *
	 * @param position The parameter position
	 * @param locale The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setLocale(int position, Locale locale);

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBigDecimal(int position, BigDecimal number);

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBigInteger(int position, BigInteger number);

	/**
	 * Bind a positional Date-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setDate(int position, Date date);

	/**
	 * Bind a positional Date-valued parameter using just the Time portion.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setTime(int position, Date date);

	/**
	 * Bind a positional Date-valued parameter using the full Timestamp.
	 *
	 * @param position The parameter position
	 * @param date The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setTimestamp(int position, Date date);

	/**
	 * Bind a positional Calendar-valued parameter using the full Timestamp portion.
	 *
	 * @param position The parameter position
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCalendar(int position, Calendar calendar);

	/**
	 * Bind a positional Calendar-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCalendarDate(int position, Calendar calendar);

	/**
	 * Bind a named String-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setString(String name, String val);

	/**
	 * Bind a named char-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCharacter(String name, char val);

	/**
	 * Bind a named boolean-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBoolean(String name, boolean val);

	/**
	 * Bind a named byte-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setByte(String name, byte val);

	/**
	 * Bind a named short-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setShort(String name, short val);

	/**
	 * Bind a named int-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setInteger(String name, int val);

	/**
	 * Bind a named long-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setLong(String name, long val);

	/**
	 * Bind a named float-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setFloat(String name, float val);

	/**
	 * Bind a named double-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setDouble(String name, double val);

	/**
	 * Bind a named binary-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBinary(String name, byte[] val);

	/**
	 * Bind a named String-valued parameter using streaming.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setText(String name, String val);

	/**
	 * Bind a named binary-valued parameter using serialization.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setSerializable(String name, Serializable val);

	/**
	 * Bind a named Locale-valued parameter.
	 *
	 * @param name The parameter name
	 * @param locale The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setLocale(String name, Locale locale);

	/**
	 * Bind a named BigDecimal-valued parameter.
	 *
	 * @param name The parameter name
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBigDecimal(String name, BigDecimal number);

	/**
	 * Bind a named BigInteger-valued parameter.
	 *
	 * @param name The parameter name
	 * @param number The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setBigInteger(String name, BigInteger number);

	/**
	 * Bind the date (time is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setDate(String name, Date date);

	/**
	 * Bind the time (date is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setTime(String name, Date date);

	/**
	 * Bind the date and the time of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param date The date object
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setTimestamp(String name, Date date);

	/**
	 * Bind a named Calendar-valued parameter using the full Timestamp.
	 *
	 * @param name The parameter name
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCalendar(String name, Calendar calendar);

	/**
	 * Bind a named Calendar-valued parameter using just the Date portion.
	 *
	 * @param name The parameter name
	 * @param calendar The bind value
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setCalendarDate(String name, Calendar calendar);

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
	Query setEntity(int position, Object val);

	/**
	 * Bind an instance of a mapped persistent class to a named query parameter.  Use
	 * {@link #setParameter(String, Object)} for null values.
	 *
	 * @param name the name of the parameter
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 */
	Query setEntity(String name, Object val);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations

	/**
	 * Set a strategy for handling the query results. This can be used to change
	 * "shape" of the query result.
	 *
	 * @param transformer The transformer to apply
	 *
	 * @return this (for method chaining)
	 *
	 * @deprecated (since 6.0) - todo : develop a new approach to result transformers
	 */
	@Deprecated
	Query setResultTransformer(ResultTransformer transformer);

	/**
	 * @deprecated (since 6.0) use {@link javax.persistence.Tuple} if you need access to "result variables".
	 */
	@Deprecated
	default String[] getReturnAliases() {
		return null;
	}

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
	 *
	 * @deprecated (since 6.0) Bind values individually
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Q setParameters(Object[] values, Type[] types) {
		assert values.length == types.length;
		for ( int i = 0; i < values.length; i++ ) {
			setParameter( i, values[i], types[i] );
		}

		return (Q) this;
	}

}
