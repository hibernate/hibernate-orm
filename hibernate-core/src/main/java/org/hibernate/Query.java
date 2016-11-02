/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated (since 5.2) use {@link org.hibernate.query.Query} instead
 */
@Deprecated
@SuppressWarnings("UnusedDeclaration")
public interface Query<R> extends TypedQuery<R>, CommonQueryContract {

	/**
	 * Get the query string.
	 *
	 * @return the query string
	 */
	String getQueryString();

	/**
	 * Obtain the FlushMode in effect for this query.  By default, the query inherits the FlushMode of the Session
	 * from which it originates.
	 *
	 * @return The query FlushMode.
	 *
	 * @see FlushMode
	 */
	FlushMode getHibernateFlushMode();

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getHibernateFlushMode()
	 */
	@SuppressWarnings("unchecked")
	default Query<R> setHibernateFlushMode(FlushMode flushMode) {
		setFlushMode( flushMode );
		return this;
	}

	/**
	 * (Re)set the current FlushMode in effect for this query.
	 *
	 * @param flushMode The new FlushMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getHibernateFlushMode()
	 *
	 * @deprecated (since 5.2) use {@link #setHibernateFlushMode} instead
	 */
	@Deprecated
	Query<R> setFlushMode(FlushMode flushMode);

	/**
	 * For users of the Hibernate native APIs, we've had to rename this method
	 * as defined by Hibernate historically because the JPA contract defines a method of the same
	 * name, but returning the JPA {@link FlushModeType} rather than Hibernate's {@link FlushMode}.  For
	 * the former behavior, use {@link org.hibernate.query.Query#getHibernateFlushMode()} instead.
	 *
	 * @return The FlushModeType in effect for this query.
	 */
	FlushModeType getFlushMode();

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
	CacheMode getCacheMode();

	/**
	 * (Re)set the current CacheMode in effect for this query.
	 *
	 * @param cacheMode The new CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getCacheMode()
	 */
	Query<R> setCacheMode(CacheMode cacheMode);

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
	boolean isCacheable();

	/**
	 * Enable/disable second level query (result) caching for this query.
	 *
	 * @param cacheable Should the query results be cacheable?
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #isCacheable
	 */
	Query<R> setCacheable(boolean cacheable);

	/**
	 * Obtain the name of the second level query cache region in which query results will be stored (if they are
	 * cached, see the discussion on {@link #isCacheable()} for more information).  {@code null} indicates that the
	 * default region should be used.
	 *
	 * @return The specified cache region name into which query results should be placed; {@code null} indicates
	 * the default region.
	 */
	String getCacheRegion();

	/**
	 * Set the name of the cache region where query results should be cached (if cached at all).
	 *
	 * @param cacheRegion the name of a query cache region, or {@code null} to indicate that the default region
	 * should be used.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getCacheRegion()
	 */
	Query<R> setCacheRegion(String cacheRegion);

	/**
	 * Obtain the query timeout <b>in seconds</b>.  This value is eventually passed along to the JDBC query via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.  Zero indicates no timeout.
	 *
	 * @return The timeout <b>in seconds</b>
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	Integer getTimeout();

	/**
	 * Set the query timeout <b>in seconds</b>.
	 *
	 * NOTE it is important to understand that any value set here is eventually passed directly through to the JDBC
	 * Statement which expressly disallows negative values.  So negative values should be avoided as a general rule.
	 *
	 * @param timeout the timeout <b>in seconds</b>
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getTimeout()
	 */
	Query<R> setTimeout(int timeout);

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
	Integer getFetchSize();

	/**
	 * Sets a JDBC fetch size hint for the query.
	 *
	 * @param fetchSize the fetch size hint
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #getFetchSize()
	 */
	Query<R> setFetchSize(int fetchSize);

	/**
	 * Should entities and proxies loaded by this Query be put in read-only mode? If the
	 * read-only/modifiable setting was not initialized, then the default
	 * read-only/modifiable setting for the persistence context is returned instead.
	 *
	 * @see #setReadOnly(boolean)
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * The read-only/modifiable setting has no impact on entities/proxies returned by the
	 * query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code true} if the entities and proxies loaded by the query will be put
	 * in read-only mode; {@code false} otherwise (they will be modifiable)
	 */
	boolean isReadOnly();

	/**
	 * Set the read-only/modifiable mode for entities and proxies
	 * loaded by this Query. This setting overrides the default setting
	 * for the persistence context.
	 * @see org.hibernate.engine.spi.PersistenceContext#isDefaultReadOnly()
	 *
	 * To set the default read-only/modifiable setting used for
	 * entities and proxies that are loaded into the session:
	 * @see org.hibernate.engine.spi.PersistenceContext#setDefaultReadOnly(boolean)
	 * @see Session#setDefaultReadOnly(boolean)
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
	 * returned by the query that existed in the session beforeQuery the query was executed.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @param readOnly {@code true} indicates that entities and proxies loaded by the query
	 * are to be put in read-only mode; {@code false} indicates that entities and proxies
	 * loaded by the query will be put in modifiable mode
	 */
	Query<R> setReadOnly(boolean readOnly);

	/**
	 * Return the Hibernate types of the query results.
	 *
	 * @return an array of types
	 *
	 * @deprecated (since 5.2) with no replacement; to be removed in 6.0
	 */
	@Deprecated
	Type[] getReturnTypes();

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
	Query<R> setLockOptions(LockOptions lockOptions);

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
	Query<R> setLockMode(String alias, LockMode lockMode);

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
	Query<R> setComment(String comment);

	/**
	 * Add a DB query hint to the SQL.  These differ from JPA's {@link javax.persistence.QueryHint}, which is specific
	 * to the JPA implementation and ignores DB vendor-specific hints.  Instead, these are intended solely for the
	 * vendor-specific hints, such as Oracle's optimizers.  Multiple query hints are supported; the Dialect will
	 * determine concatenation and placement.
	 *
	 * @param hint The database specific query hint to add.
	 */
	Query<R> addQueryHint(String hint);

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
	 * support for scrollable <tt>ResultSet</tt>s.
	 *
	 * <p>
	 *
	 * You should call {@link ScrollableResults#close()} after processing the <tt>ScrollableResults</tt>
	 * so that the underlying resources are deallocated right away.
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
	 * <p>
	 *
	 * You should call {@link ScrollableResults#close()} after processing the <tt>ScrollableResults</tt>
	 * so that the underlying resources are deallocated right away.
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

	default List<R> getResultList() {
		return list();
	}

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or {@code null} if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 *
	 * @throws NonUniqueResultException if there is more than one matching result
	 */
	R uniqueResult();

	default R getSingleResult() {
		return uniqueResult();
	}

	/**
	 * Access to information about query parameters.
	 *
	 * @return information about query parameters.
	 */
	ParameterMetadata getParameterMetadata();

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names, in no particular order
	 *
	 * @deprecated (since 5.2) use {@link ParameterMetadata#getNamedParameterNames()}
	 */
	@Deprecated
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
	<T> Query<R> setParameter(QueryParameter<T> parameter, T val);

	<T> Query<R> setParameter(Parameter<T> param, T value);

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
	Query<R> setParameter(String name, Object val);

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
	Query<R> setParameter(int position, Object val);

	/**
	 * Bind a query parameter using the supplied Type
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P val, Type type);

	/**
	 * Bind a named query parameter using the supplied Type
	 *
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameter(String name, Object val, Type type);

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
	Query<R> setParameter(int position, Object val, Type type);

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
	<P> Query<R> setParameter(QueryParameter<P> parameter, P val, TemporalType temporalType);

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
	<P> Query<R> setParameter(String name, P val, TemporalType temporalType);

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
	<P> Query<R> setParameter(int position, P val, TemporalType temporalType);





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
	 * @param parameter the parameter memento
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameterList(QueryParameter<P> parameter, Collection<P> values);

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
	Query<R> setParameterList(String name, Collection values);

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
	Query<R> setParameterList(String name, Collection values, Type type);

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
	Query<R> setParameterList(String name, Object[] values, Type type);

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
	Query<R> setParameterList(String name, Object[] values);

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setProperties(Object bean);

	/**
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean a java.util.Map
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setProperties(Map bean);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	Query<R> setMaxResults(int maxResult);

	@Override
	Query<R> setFirstResult(int startPosition);

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	Query<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	Query<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// proposed deprecations

	/**
	 * Bind a positional String-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setString(int position, String val) {
		setParameter( position, val, StringType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional char-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCharacter(int position, char val) {
		setParameter( position, val, CharacterType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional boolean-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBoolean(int position, boolean val) {
		setParameter( position, val, determineProperBooleanType( position, val, BooleanType.INSTANCE ) );
		return this;
	}

	/**
	 * Bind a positional byte-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setByte(int position, byte val) {
		setParameter( position, val, ByteType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional short-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setShort(int position, short val) {
		setParameter( position, val, ShortType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional int-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setInteger(int position, int val) {
		setParameter( position, val, IntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional long-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLong(int position, long val) {
		setParameter( position, val, LongType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional float-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setFloat(int position, float val) {
		setParameter( position, val, FloatType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional double-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDouble(int position, double val) {
		setParameter( position, val, DoubleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional binary-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBinary(int position, byte[] val) {
		setParameter( position, val, BinaryType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional String-valued parameter using streaming.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setText(int position, String val) {
		setParameter( position, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional binary-valued parameter using serialization.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setSerializable(int position, Serializable val) {
		setParameter( position, val );
		return this;
	}

	/**
	 * Bind a positional Locale-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLocale(int position, Locale val) {
		setParameter( position, val, LocaleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigDecimal(int position, BigDecimal val) {
		setParameter( position, val, BigDecimalType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional BigDecimal-valued parameter.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigInteger(int position, BigInteger val) {
		setParameter( position, val, BigIntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDate(int position, Date val) {
		setParameter( position, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using just the Time portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTime(int position, Date val) {
		setParameter( position, val, TimeType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Date-valued parameter using the full Timestamp.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTimestamp(int position, Date val) {
		setParameter( position, val, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Calendar-valued parameter using the full Timestamp portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendar(int position, Calendar val) {
		setParameter( position, val, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a positional Calendar-valued parameter using just the Date portion.
	 *
	 * @param position The parameter position
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendarDate(int position, Calendar val) {
		setParameter( position, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named String-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setString(String name, String val) {
		setParameter( name, val, StringType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named char-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCharacter(String name, char val) {
		setParameter( name, val, CharacterType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named boolean-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBoolean(String name, boolean val) {
		setParameter( name, val, determineProperBooleanType( name, val, BooleanType.INSTANCE ) );
		return this;
	}

	/**
	 * Bind a named byte-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setByte(String name, byte val) {
		setParameter( name, val, ByteType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named short-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setShort(String name, short val) {
		setParameter( name, val, ShortType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named int-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setInteger(String name, int val) {
		setParameter( name, val, IntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named long-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLong(String name, long val) {
		setParameter( name, val, LongType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named float-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setFloat(String name, float val) {
		setParameter( name, val, FloatType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named double-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDouble(String name, double val) {
		setParameter( name, val, DoubleType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named binary-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBinary(String name, byte[] val) {
		setParameter( name, val, BinaryType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named String-valued parameter using streaming.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setText(String name, String val) {
		setParameter( name, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named binary-valued parameter using serialization.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setSerializable(String name, Serializable val) {
		setParameter( name, val );
		return this;
	}

	/**
	 * Bind a named Locale-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setLocale(String name, Locale val) {
		setParameter( name, val, TextType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named BigDecimal-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigDecimal(String name, BigDecimal val) {
		setParameter( name, val, BigDecimalType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named BigInteger-valued parameter.
	 *
	 * @param name The parameter name
	 * @param val The bind value
	 *
	 * @return {@code this}, for method chaining
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setBigInteger(String name, BigInteger val) {
		setParameter( name, val, BigIntegerType.INSTANCE );
		return this;
	}

	/**
	 * Bind the val (time is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param val The val object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setDate(String name, Date val) {
		setParameter( name, val, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind the time (val is truncated) of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param val The val object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTime(String name, Date val) {
		setParameter( name, val, TimeType.INSTANCE );
		return this;
	}

	/**
	 * Bind the value and the time of a given Date object to a named query parameter.
	 *
	 * @param name The name of the parameter
	 * @param value The value object
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setTimestamp(String name, Date value) {
		setParameter( name, value, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named Calendar-valued parameter using the full Timestamp.
	 *
	 * @param name The parameter name
	 * @param value The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendar(String name, Calendar value) {
		setParameter( name, value, TimestampType.INSTANCE );
		return this;
	}

	/**
	 * Bind a named Calendar-valued parameter using just the Date portion.
	 *
	 * @param name The parameter name
	 * @param value The bind value
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setCalendarDate(String name, Calendar value) {
		setParameter( name, value, DateType.INSTANCE );
		return this;
	}

	/**
	 * Bind an instance of a mapped persistent class to a JDBC-style query parameter.
	 * Use {@link #setParameter(int, Object)} for null values.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	Query<R> setEntity(int position, Object val);

	/**
	 * Bind an instance of a mapped persistent class to a named query parameter.  Use
	 * {@link #setParameter(String, Object)} for null values.
	 *
	 * @param name the name of the parameter
	 * @param val a non-null instance of a persistent class
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 5.2) use {@link #setParameter(int, Object)} or {@link #setParameter(int, Object, Type)}
	 * instead
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	Query<R> setEntity(String name, Object val);

	/**
	 * @deprecated added only to allow default method definition for deprecated methods here.
	 */
	@Deprecated
	Type determineProperBooleanType(int position, Object value, Type defaultType);

	/**
	 * @deprecated added only to allow default method definition for deprecated methods here.
	 */
	@Deprecated
	Type determineProperBooleanType(String name, Object value, Type defaultType);


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
	 * @deprecated (since 5.2)
	 * @todo develop a new approach to result transformers
	 */
	@Deprecated
	Query<R> setResultTransformer(ResultTransformer transformer);

	/**
	 * @deprecated (since 5.2) use {@link javax.persistence.Tuple} if you need access to "result variables".
	 */
	@Deprecated
	String[] getReturnAliases();

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
	 * @deprecated (since 5.2) Bind values individually
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setParameters(Object[] values, Type[] types) {
		assert values.length == types.length;
		for ( int i = 0; i < values.length; i++ ) {
			setParameter( i, values[i], types[i] );
		}

		return this;
	}
}
