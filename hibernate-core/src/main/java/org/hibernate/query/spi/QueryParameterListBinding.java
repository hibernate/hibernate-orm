/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.type.Type;

/**
 * Represents a "parameter list" binding: aka the binding of a collection of values for a single
 * query parameter.  At some point these need to be "expanded"; see {@link QueryParameterBindingsImpl#expandListValuedParameters(String, SharedSessionContractImplementor)}
 * for details.
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
	void setBindValues(Collection<T> values, Type clarifiedType);

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
