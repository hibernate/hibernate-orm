/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

/**
 * Attribute metadata contract for a non-plural attribute.
 *
 * @param <X> The owner type
 * @param <Y> The attribute type
 */
public interface SingularAttributeMetadata<X, Y> extends AttributeMetadata<X, Y> {
	/**
	 * Retrieve the value context for this attribute
	 *
	 * @return The attributes value context
	 */
	ValueContext getValueContext();
}
