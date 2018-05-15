/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

/**
 * Represents an HQL/JPQL query or a compiled Criteria query.  Also acts as the Hibernate
 * extension to the JPA Query/TypedQuery contract
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @param <R> The query result type (for typed queries)
 */
@Incubating
@SuppressWarnings("UnusedDeclaration")
public interface Query<R> extends TypedQuery<R>, CommonQueryContract {
	/**
	 * Get the QueryProducer this Query originates from.  Generally speaking,
	 * this is the Session/StatelessSession that was used to create the Query
	 * instance.
	 *
	 * @return The producer of this query
	 */
	SharedSessionContractImplementor getSession();

	/**
	 * Get the query string.  Note that this may be {@code null} or some other
	 * less-than-useful return because the source of the query might not be a
	 * String (e.g., a Criteria query).
	 *
	 * @return the query string.
	 */
	String getQueryString();

	/**
	 * Apply the given graph using the given semantic
	 *
	 * @param graph The graph the apply.
	 * @param semantic The semantic to use when applying the graph
	 *
	 * @return this - for method chaining
	 */
	Query<R> applyGraph(RootGraph<?> graph, GraphSemantic semantic);

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#FETCH fetch semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#FETCH} as the semantic
	 */
	default Query<R> applyFetchGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.FETCH );
	}

	/**
	 * Apply the given graph using {@linkplain GraphSemantic#LOAD load semantics}
	 *
	 * @apiNote This method calls {@link #applyGraph(RootGraph, GraphSemantic)} using
	 * {@link GraphSemantic#LOAD} as the semantic
	 */
	default Query<R> applyLoadGraph(RootGraph graph) {
		return applyGraph( graph, GraphSemantic.LOAD );
	}

	/**
	 * Return the query results as an <tt>Iterator</tt>. If the query
	 * contains multiple results pre row, the results are returned in
	 * an instance of <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first
	 * SQL query returns identifiers only.<br>
	 *
	 * @return the result iterator
	 *
	 * @deprecated Deprecated functionality with no real replacement.  Use
	 * {@link #list} / {@link #getResultList} instead and open Iterator
	 * on returned List
	 */
	@Deprecated
	default Iterator<R> iterate() {
		return list().iterator();
	}

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

	Optional<R> uniqueResultOptional();

	/**
	 * Retrieve a Stream over the query results.
	 * <p/>
	 * In the initial implementation (5.2) this returns a simple sequential Stream.  The plan
	 * is to return a a smarter stream in 6.x leveraging the SQM model.
	 *
	 * <p>
	 *
	 * You should call {@link java.util.stream.Stream#close()} after processing the stream
	 * so that the underlying resources are deallocated right away.
	 *
	 * @return The results Stream
	 *
	 * @since 5.2
	 */
	Stream<R> stream();

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
	 * Add a DB query hint to the SQL.  These differ from JPA's
	 * {@link javax.persistence.QueryHint} and {@link #getHints()}, which is
	 * specific to the JPA implementation and ignores DB vendor-specific hints.
	 * Instead, these are intended solely for the vendor-specific hints, such
	 * as Oracle's optimizers.  Multiple query hints are supported; the Dialect
	 * will determine concatenation and placement.
	 *
	 * @param hint The database specific query hint to add.
	 */
	Query<R> addQueryHint(String hint);

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
	 * <li>{@link LockOptions#getLockMode()}</li>
	 * <li>{@link LockOptions#getScope()}</li>
	 * <li>{@link LockOptions#getTimeOut()}</li>
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
	 * <p>
	 * The alias-specific lock modes specified here are added to the query's internal
	 * {@link #getLockOptions() LockOptions}.
	 * <p>
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

	Query<R> setTupleTransformer(TupleTransformer<R> transformer);

	Query<R> setResultListTransformer(ResultListTransformer transformer);

	/**
	 * Get the execution options for this Query.  Many of the setter on the Query
	 * contract update the state of the returned {@link QueryOptions}.  This is
	 * important because it gives access to any primitive data in their wrapper
	 * forms rather than the primitive forms as required by JPA.  For example, that
	 * allows use to know whether a specific value has been set at all by the Query
	 * consumer.
	 *
	 * @return Return the encapsulation of this query's options, which includes access to
	 * firstRow, maxRows, timeout and fetchSize, etc.
	 */
	QueryOptions getQueryOptions();

	/**
	 * Access to information about query parameters.
	 *
	 * @return information about query parameters.
	 */
	<P extends QueryParameter<?>> ParameterMetadata<P> getParameterMetadata();

	/**
	 * Bind a query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage
	 * context then use of this form of binding is not allowed, and
	 * {@link #setParameter(QueryParameter, Object, Type)} should be used instead
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> Query<R> setParameter(QueryParameter<T> parameter, T val);

	/**
	 * Bind a named query parameter using the supplied Type
	 *
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameter(String, Object, AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameter(String name, Object val, Type type){
		return setParameter( name, val, (AllowableParameterType) type );
	}

	/**
	 * Bind a named query parameter using the supplied Type
	 *
	 * @param name the name of the parameter
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate allowable parameter type
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameter(String name, Object val, AllowableParameterType type);

	/**
	 * Bind a value to a JDBC-style query parameter.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameter(int, Object, AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameter(int position, Object val, Type type) {
		return setParameter( position, val, (AllowableParameterType) type );
	}

	/**
	 * Bind a value to a JDBC-style query parameter.
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate allowable parameter type
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameter(int position, Object val, AllowableParameterType type);

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
	 * Bind a query parameter using the supplied Type
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate type
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameter(QueryParameter, Object, AllowableParameterType)}
	 */
	@Deprecated
	default <P> Query<R> setParameter(QueryParameter<P> parameter, P val, Type type){
		return setParameter( parameter, val, (AllowableParameterType) type );
	}

	/**
	 * Bind a query parameter using the supplied Type
	 *
	 * @param parameter The query parameter memento
	 * @param val the possibly-null parameter value
	 * @param type the Hibernate allowable parameter type
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> Query<R> setParameter(QueryParameter<P> parameter, P val, AllowableParameterType type);

	Query<R> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType);

	Query<R> setParameter(
			Parameter<LocalDateTime> param,
			LocalDateTime value,
			TemporalType temporalType);

	Query<R> setParameter(
			Parameter<ZonedDateTime> param,
			ZonedDateTime value,
			TemporalType temporalType);

	Query<R> setParameter(
			Parameter<OffsetDateTime> param,
			OffsetDateTime value,
			TemporalType temporalType);

	Query<R> setParameter(String name, Instant value, TemporalType temporalType);

	Query<R> setParameter(String name, LocalDateTime value, TemporalType temporalType);

	Query<R> setParameter(String name, ZonedDateTime value, TemporalType temporalType);

	Query<R> setParameter(String name, OffsetDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, Instant value, TemporalType temporalType);

	Query<R> setParameter(int position, LocalDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType);

	Query<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - CommonQueryContract

	@Override
	Query<R> setHibernateFlushMode(FlushMode flushMode);

	@Override
	Query<R> setCacheable(boolean cacheable);

	@Override
	Query<R> setCacheRegion(String cacheRegion);

	@Override
	Query<R> setCacheMode(CacheMode cacheMode);

	@Override
	Query<R> setTimeout(int timeout);

	@Override
	Query<R> setFetchSize(int fetchSize);

	@Override
	Query<R> setReadOnly(boolean readOnly);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides - javax.persistence.Query/TypedQuery

	@Override
	Query<R> setMaxResults(int maxResult);

	@Override
	Query<R> setFirstResult(int startPosition);

	@Override
	Query<R> setHint(String hintName, Object value);

	@Override
	Query<R> setFlushMode(FlushModeType flushMode);

	@Override
	Query<R> setLockMode(LockModeType lockMode);

	/**
	 * Bind a named query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage context then
	 * use of this form of binding is not allowed, and {@link #setParameter(String, Object, Type)}
	 * should be used instead
	 *
	 * @param name the parameter name
	 * @param value the (possibly-null) parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	@Override
	Query<R> setParameter(String name, Object value);

	/**
	 * Bind a positional query parameter using its inferred Type.  If the parameter is
	 * defined in such a way that the Type cannot be inferred from its usage context then
	 * use of this form of binding is not allowed, and {@link #setParameter(int, Object, Type)}
	 * should be used instead
	 *
	 * @param position the position of the parameter in the query
	 * string, numbered from <tt>0</tt>.
	 * @param value the possibly-null parameter value
	 *
	 * @return {@code this}, for method chaining
	 */
	@Override
	Query<R> setParameter(int position, Object value);

	@Override
	<T> Query<R> setParameter(Parameter<T> param, T value);

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
	 * Bind multiple values to a positional query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly
	 * guessed from the class of the first object in the collection. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int position, Collection values);

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
	Query<R> setParameterList(String name, Collection values, Class type);

	/**
	 * Bind multiple values to a positional query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly
	 * guessed from the class of the first object in the collection. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int position, Collection values, Class type);

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameterList(String, Collection, AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameterList(String name, Collection values, Type type){
		return setParameter( name, values, (AllowableParameterType) type );
	}

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameterList(String, Collection, AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameterList(int position, Collection values, Type type){
		return setParameter( position, values, (AllowableParameterType) type );
	}

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 * @param type the Hibernate allowable parameter type of the values
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(String name, Collection values, AllowableParameterType type);

	/**
	 * Bind multiple values to a positional query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (?1)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 * @param type the Hibernate allowable parameter type of the values
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int position, Collection values, AllowableParameterType type);

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param name the name of the parameter
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameterList(String, Object[], AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameterList(String name, Object[] values, Type type){
		return setParameter( name, values, (AllowableParameterType)type );
	}

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use {@link #setParameterList(String, Object[], AllowableParameterType)}
	 */
	@Deprecated
	default Query<R> setParameterList(int position, Object[] values, Type type){
		return setParameter( position, values, (AllowableParameterType)type );
	}

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
	Query<R> setParameterList(String name, Object[] values, AllowableParameterType type);

	/**
	 * Bind multiple values to a named query parameter. This is useful for binding
	 * a list of values to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 * @param type the Hibernate type of the values
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int position, Object[] values, AllowableParameterType type);

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
	 * Bind multiple values to a named query parameter. The Hibernate type of the parameter is
	 * first detected via the usage/position in the query and if not sufficient secondly
	 * guessed from the class of the first object in the array. This is useful for binding a list of values
	 * to an expression such as <tt>foo.bar in (:value_list)</tt>.
	 *
	 * @param position the parameter positional label
	 * @param values a collection of values to list
	 *
	 * @return {@code this}, for method chaining
	 */
	Query<R> setParameterList(int position, Object[] values);

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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// deprecations

	/**
	 * @deprecated (since 5.2) Use
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	default Query<R> setResultTransformer(ResultTransformer transformer) {
		throw new NotYetImplementedException( "On 6.0 branch ResultTransformer extends the 2 new contracts" );
//		setTupleTransformer( transformer );
//		setResultListTransformer( transformer );
//		return this;
	}
}
