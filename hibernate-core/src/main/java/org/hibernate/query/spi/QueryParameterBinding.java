/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Collection;
import jakarta.persistence.TemporalType;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.BindableType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * The value/type binding information for a particular query parameter.  Supports
 * both single-valued and multivalued binds
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBinding<T> {
	/**
	 * Is any value (including {@code null}) bound?  Asked another way,
	 * were any of the `#set` methods called?
	 */
	boolean isBound();

	/**
	 * Is the binding multivalued?
	 */
	boolean isMultiValued();

	QueryParameter<T> getQueryParameter();

	/**
	 * Get the Type currently associated with this binding.
	 *
	 * @return The currently associated Type
	 */
	BindableType<? super T> getBindType();

	/**
	 * If the parameter represents a temporal type, return the explicitly
	 * specified precision - if one.
	 */
	TemporalType getExplicitTemporalPrecision();

	/**
	 * Sets the parameter binding value.  The inherent parameter type (if known) is assumed
	 */
	default void setBindValue(T value) {
		setBindValue( value, false );
	}

	/**
	 * Sets the parameter binding value.  The inherent parameter type (if known) is assumed.
	 * The flag controls whether the parameter type should be resolved if necessary.
	 */
	void setBindValue(T value, boolean resolveJdbcTypeIfNecessary);

	/**
	 * Sets the parameter binding value using the explicit Type.
	 * @param value The bind value
	 * @param clarifiedType The explicit Type to use
	 */
	void setBindValue(T value, BindableType<T> clarifiedType);

	/**
	 * Sets the parameter binding value using the explicit TemporalType.
	 * @param value The bind value
	 * @param temporalTypePrecision The temporal type to use
	 */
	void setBindValue(T value, TemporalType temporalTypePrecision);

	/**
	 * Get the value current bound.
	 *
	 * @return The currently bound value
	 */
	T getBindValue();

	/**
	 * Sets the parameter binding values.  The inherent parameter type (if known) is assumed in regards to the
	 * individual values.
	 *  @param values The bind values
	 *
	 */
	void setBindValues(Collection<? extends T> values);

	/**
	 * Sets the parameter binding values using the explicit Type in regards to the individual values.
	 * @param values The bind values
	 * @param clarifiedType The explicit Type to use
	 */
	void setBindValues(Collection<? extends T> values, BindableType<T> clarifiedType);

	/**Sets the parameter binding value using the explicit TemporalType in regards to the individual values.
	 *
	 *  @param values The bind values
	 * @param temporalTypePrecision The temporal type to use
	 */
	void setBindValues(Collection<? extends T> values, TemporalType temporalTypePrecision, TypeConfiguration typeConfiguration);

	/**
	 * Get the values currently bound.
	 *
	 * @return The currently bound values
	 */
	Collection<? extends T> getBindValues();

	/**
	 * Returns the inferred mapping model expressible i.e. the model reference against which this parameter is compared.
	 *
	 * @return the inferred mapping model expressible or <code>null</code>
	 */
	MappingModelExpressible<T> getType();

	/**
	 * Sets the mapping model expressible for this parameter.
	 *
	 * @param type The mapping model expressible
	 * @return Whether the bind type was changed
	 */
	boolean setType(MappingModelExpressible<T> type);
}
