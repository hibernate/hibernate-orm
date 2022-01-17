/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.Session;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

/**
 * Defines the aspects of query definition that apply to all forms of
 * querying - HQL, Criteria and ProcedureCall
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface CommonQueryContract {

	/**
	 * The FlushModeType in effect for this query.  By default, the query
	 * inherits the FlushMode of the Session from which it originates.
	 *
	 * @see #getHibernateFlushMode
	 * @see Session#getHibernateFlushMode
	 */
	FlushModeType getFlushMode();

	/**
	 * Set the FlushMode in to use for this query.
	 *
	 * @implNote Setting to {@code null} ultimately indicates to use the
	 * FlushMode of the Session.  Use {@link #setHibernateFlushMode} passing
	 * {@link FlushMode#MANUAL} instead to indicate that no automatic flushing
	 * should occur
	 *
	 * @see #getHibernateFlushMode()
	 * @see Session#getHibernateFlushMode()
	 */
	CommonQueryContract setFlushMode(FlushModeType flushMode);

	/**
	 * The FlushMode in effect for this query.  By default, the query
	 * inherits the FlushMode of the Session from which it originates.
	 *
	 * @see Session#getHibernateFlushMode
	 */
	FlushMode getHibernateFlushMode();

	/**
	 * Set the current FlushMode in effect for this query.
	 *
	 * @implNote Setting to {@code null} ultimately indicates to use the
	 * FlushMode of the Session.  Use {@link FlushMode#MANUAL} instead to
	 * indicate that no automatic flushing should occur
	 *
	 * @see #getHibernateFlushMode()
	 * @see Session#getHibernateFlushMode()
	 */
	CommonQueryContract setHibernateFlushMode(FlushMode flushMode);

	/**
	 * Obtain the query timeout <b>in seconds</b>.  This value is eventually passed along to the JDBC query via
	 * {@link java.sql.Statement#setQueryTimeout(int)}.  Zero indicates no timeout.
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
	CommonQueryContract setTimeout(int timeout);

	String getComment();

	CommonQueryContract setComment(String comment);

	/**
	 * Apply hints to the query
	 */
	CommonQueryContract setHint(String hintName, Object value);

	/**
	 * Bind the given argument to a named query parameter
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use one of the forms accept a "type".
	 *
	 * @see #setParameter(String, Object, Class)
	 * @see #setParameter(String, Object, BindableType)
	 */
	CommonQueryContract setParameter(String name, Object value);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameter(String, Object)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(String, Object, BindableType)
	 */
	<P> CommonQueryContract setParameter(String name, P value, Class<P> type);

	/**
	 * Bind the given argument to a named query parameter using the given
	 * {@link BindableType}.
	 *
	 * @see BindableType#parameterType
	 */
	<P> CommonQueryContract setParameter(String name, P value, BindableType<P> type);

	/**
	 * Bind an {@link Instant} value to the named query parameter using just the portion
	 * indicated by the given {@link TemporalType}.
	 */
	CommonQueryContract setParameter(String name, Instant value, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(String name, Calendar value, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(String name, Date value, TemporalType temporalType);

	/**
	 * Bind the given argument to a positional query parameter.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use one of the forms accept a "type".
	 *
	 * @see #setParameter(int, Object, Class)
	 * @see #setParameter(int, Object, BindableType)
	 */
	CommonQueryContract setParameter(int position, Object value);

	/**
	 * Bind the given argument to a positional query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameter(int, Object)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(int, Object, BindableType)
	 */
	<P> CommonQueryContract setParameter(int position, P value, Class<P> type);

	/**
	 * Bind the given argument to a positional query parameter using the given
	 * {@link BindableType}.
	 *
	 * @see BindableType#parameterType
	 */
	<P> CommonQueryContract setParameter(int position, P value, BindableType<P> type);

	/**
	 * Bind an {@link Instant} value to the positional query parameter using just the portion
	 * indicated by the given {@link TemporalType}.
	 */
	CommonQueryContract setParameter(int position, Instant value, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(int position, Date value, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(int position, Calendar value, TemporalType temporalType);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter}.
	 * <p>
	 * If the type of the parameter cannot be inferred from the context in which
	 * it occurs, use on of the forms accept a "type".
	 *
	 * @see #setParameter(QueryParameter, Object, BindableType)
	 *
	 * @param parameter the query parameter memento
	 * @param value the argument, which might be null
	 *
	 * @return {@code this}, for method chaining
	 */
	<T> CommonQueryContract setParameter(QueryParameter<T> parameter, T value);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter} using the given Class reference to attempt to
	 * determine the {@link BindableType} to use.  If unable to determine
	 * an appropriate {@link BindableType}, {@link #setParameter(QueryParameter, Object)} is used
	 *
	 * @param parameter the query parameter memento
	 * @param value the argument, which might be null
	 * @param type a {@link BindableType} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameter(QueryParameter, Object, BindableType)
	 */
	<P> CommonQueryContract setParameter(QueryParameter<P> parameter, P value, Class<P> type);

	/**
	 * Bind an argument to the query parameter represented by the given
	 * {@link QueryParameter} using the given {@link BindableType}.
	 *
	 * @param parameter the query parameter memento
	 * @param val the argument, which might be null
	 * @param type an {@link BindableType} representing the type of the parameter
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameter(QueryParameter<P> parameter, P val, BindableType<P> type);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	<T> CommonQueryContract setParameter(Parameter<T> param, T value);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	/**
	 * {@link jakarta.persistence.Query} override
	 */
	CommonQueryContract setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @see #setParameterList(java.lang.String, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(String name, @SuppressWarnings("rawtypes") Collection values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(java.lang.String, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to a named query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String name, Collection<? extends P> values, BindableType<P> type);


	/**
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(String name, Object[] values);

	/**
	 * Bind multiple arguments to a named query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(java.lang.String, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String name, P[] values, Class<P> javaType);


	/**
	 * Bind multiple arguments to a named query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(String name, P[] values, BindableType<P> type);

	/**
	 * Bind multiple arguments to a positional query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(int position, @SuppressWarnings("rawtypes") Collection values);

	/**
	 * Bind multiple arguments to a positional query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(int, Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to a positional query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int position, Collection<? extends P> values, BindableType<P> type);

	/**
	 * Bind multiple arguments to a positional query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setParameterList(int position, Object[] values);

	/**
	 * Bind multiple arguments to a positional query parameter using the given
	 * Class reference to attempt to determine the {@link BindableType}
	 * to use.  If unable to determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(int, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int position, P[] values, Class<P> javaType);

	/**
	 * Bind multiple arguments to a positional query parameter using the passed type-mapping.
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(int position, P[] values, BindableType<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}.
	 * <p>
	 * The type of the parameter is inferred from the context in which it
	 * occurs, and from the type of the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param values a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given Class reference to attempt
	 * to determine the {@link BindableType} to use.  If unable to
	 * determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(QueryParameter, java.util.Collection, BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, inferring the {@link BindableType}.
	 *
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}
	 * <p>
	 * The type of the parameter is inferred between the context in which it
	 * occurs, the type associated with the QueryParameter and the type of
	 * the first given argument.
	 *
	 * @param parameter the parameter memento
	 * @param values a collection of arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter} using the given Class reference to attempt
	 * to determine the {@link BindableType} to use.  If unable to
	 * determine an appropriate {@link BindableType},
	 * {@link #setParameterList(String, Collection)} is used
	 *
	 * @see BindableType#parameterType(Class)
	 * @see #setParameterList(QueryParameter, Object[], BindableType)
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType);

	/**
	 * Bind multiple arguments to the query parameter represented by the
	 * given {@link QueryParameter}, inferring the {@link BindableType}.
	 *
	 * Bind multiple arguments to a named query parameter.
	 * <p/>
	 * The "type mapping" for the binding is inferred from the type of
	 * the first collection element
	 *
	 * @apiNote This is used for binding a list of values to an expression such as {@code entity.field in (:values)}.
	 *
	 * @return {@code this}, for method chaining
	 */
	<P> CommonQueryContract setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type);

	/**
	 * Bind the property values of the given bean to named parameters of the query,
	 * matching property names with parameter names and mapping property types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean any JavaBean or POJO
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setProperties(Object bean);

	/**
	 * Bind the values of the given Map for each named parameters of the query,
	 * matching key names with parameter names and mapping value types to
	 * Hibernate types using heuristics.
	 *
	 * @param bean a {@link Map} of names to arguments
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonQueryContract setProperties(@SuppressWarnings("rawtypes") Map bean);
}
