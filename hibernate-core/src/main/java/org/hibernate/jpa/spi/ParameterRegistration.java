/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.Query;
import javax.persistence.TemporalType;

/**
 * Hibernate specific extension to the JPA {@link javax.persistence.Parameter} contract as known to the
 * {@link javax.persistence.Query} and {@link javax.persistence.StoredProcedureQuery} implementations.  Used to track
 * information known about the parameter.
 * <p/>
 * For parameter information as known to JPA criteria queries, see {@link org.hibernate.query.criteria.internal.expression.ParameterExpressionImpl}
 * instead.
 *
 * @author Steve Ebersole
 */
public interface ParameterRegistration<T> extends Parameter<T> {
	/**
	 * JPA has a different definition of positional parameters than what legacy Hibernate HQL had.  In JPA,
	 * the parameter holders are labelled (named :/).  At any rate the semantics are different and we often
	 * need to understand which we are dealing with (and applications might too).
	 *
	 * @return {@code true} if this is a JPA-style positional parameter; {@code false} would indicate
	 * we have either a named parameter ({@link #getName()} would return a non-{@code null} value) or a native
	 * Hibernate positional parameter.
	 */
	boolean isJpaPositionalParameter();

	/**
	 * Access to the query that this parameter belongs to.
	 *
	 * @return The defining query
	 */
	Query getQuery();

	/**
	 * Retrieves the parameter "mode" which describes how the parameter is defined in the actual database procedure
	 * definition (is it an INPUT parameter?  An OUTPUT parameter? etc).
	 *
	 * @return The parameter mode.
	 */
	ParameterMode getMode();

	/**
	 * Can we bind (set) values on this parameter?  Generally this is {@code true}, but would not be in the case
	 * of parameters with OUT or REF_CURSOR mode.
	 *
	 * @return Whether the parameter is bindable (can set be called).
	 */
	boolean isBindable();

	/**
	 * If bindable, bind the value.
	 *
	 * @param value The value to bind.
	 */
	void bindValue(T value);

	/**
	 * If bindable, bind the value using the specific temporal type.
	 *
	 * @param value The value to bind
	 * @param specifiedTemporalType The temporal type to use in binding
	 */
	void bindValue(T value, TemporalType specifiedTemporalType);

	/**
	 * If bindable, get the current binding.
	 *
	 * @return The current binding
	 */
	ParameterBind<T> getBind();
}
