/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;


import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Represents the thing-to-be-instantiated in a dynamic instantiation expression.  Hibernate
 * supports 3 "natures" of dynamic instantiation target; see {@link DynamicInstantiationNature} for further details.
 *
 * @author Steve Ebersole
 */
public interface SqmDynamicInstantiationTarget<T> extends SqmExpressible<T> {

	/**
	 * Retrieves the enum describing the nature of this target.
	 *
	 * @return The nature of this target.
	 */
	DynamicInstantiationNature getNature();

	JavaType<T> getTargetTypeDescriptor();

	/**
	 * For {@link DynamicInstantiationNature#CLASS} this will return the Class to be instantiated.  For
	 * {@link DynamicInstantiationNature#MAP} and {@link DynamicInstantiationNature#LIST} this will return {@code Map.class}
	 * and {@code List.class} respectively.
	 *
	 * @return The type to be instantiated.
	 */
	default Class<?> getJavaType() {
		return getTargetTypeDescriptor().getJavaTypeClass();
	}
}
