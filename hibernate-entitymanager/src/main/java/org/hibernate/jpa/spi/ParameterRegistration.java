/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
 * For parameter information as known to JPA criteria queries, see {@link org.hibernate.jpa.criteria.expression.ParameterExpressionImpl}
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
	public boolean isJpaPositionalParameter();

	/**
	 * Access to the query that this parameter belongs to.
	 *
	 * @return The defining query
	 */
	public Query getQuery();

	/**
	 * Retrieves the parameter "mode" which describes how the parameter is defined in the actual database procedure
	 * definition (is it an INPUT parameter?  An OUTPUT parameter? etc).
	 *
	 * @return The parameter mode.
	 */
	public ParameterMode getMode();

	/**
	 * Can we bind (set) values on this parameter?  Generally this is {@code true}, but would not be in the case
	 * of parameters with OUT or REF_CURSOR mode.
	 *
	 * @return Whether the parameter is bindable (can set be called).
	 */
	public boolean isBindable();

	/**
	 * If bindable, bind the value.
	 *
	 * @param value The value to bind.
	 */
	public void bindValue(T value);

	/**
	 * If bindable, bind the value using the specific temporal type.
	 *
	 * @param value The value to bind
	 * @param specifiedTemporalType The temporal type to use in binding
	 */
	public void bindValue(T value, TemporalType specifiedTemporalType);

	/**
	 * If bindable, get the current binding.
	 *
	 * @return The current binding
	 */
	public ParameterBind<T> getBind();
}
