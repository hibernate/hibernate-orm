/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Collection;
import jakarta.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.type.BindableType;
import org.hibernate.type.Type;

/**
 * Represents a "parameter list" binding: aka the binding of a collection of values for a single
 * query parameter.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterListBinding<T> {
	/**
	 * Sets the parameter binding values.  The inherent parameter type (if known) is assumed in regards to the
	 * individual values.
	 *
	 * @param values The bind values
	 */
	void setBindValues(Collection<T> values);

	/**
	 * Sets the parameter binding values using the explicit Type in regards to the individual values.
	 *
	 * @param values The bind values
	 * @param clarifiedType The explicit Type to use
	 */
	void setBindValues(Collection<T> values, BindableType clarifiedType);

	/**Sets the parameter binding value using the explicit TemporalType in regards to the individual values.
	 *
	 *
	 * @param values The bind values
	 * @param clarifiedTemporalType The temporal type to use
	 */
	void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType);

	/**
	 * Get the values currently bound.
	 *
	 * @return The currently bound values
	 */
	Collection<T> getBindValues();

	/**
	 * Get the Type currently associated with this binding.
	 *
	 * @return The currently associated Type
	 */
	Type getBindType();
}
