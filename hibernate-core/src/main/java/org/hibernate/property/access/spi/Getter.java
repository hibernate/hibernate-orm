/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The contract for getting the value of a persistent attribute from its container/owner.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Getter extends Serializable {
	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the property value to be retrieved.
	 *
	 * @return The extracted value.
	 */
	@Nullable Object get(Object owner);

	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the value to be retrieved.
	 * @param mergeMap a map of merged persistent instances to detached instances
	 * @param session The session from which this request originated.
	 *
	 * @return The extracted value.
	 */
	@Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session);

	/**
	 * Retrieve the declared Java type class
	 *
	 * @return The declared java type class.
	 */
	Class<?> getReturnTypeClass();

	/**
	 * Retrieve the declared Java type
	 *
	 * @return The declared java type.
	 */
	Type getReturnType();

	/**
	 * Retrieve the member to which this property maps.  This might be the
	 * field or it might be the getter method.
	 * <p>
	 * Optional operation (may return {@code null})
	 *
	 * @return The mapped member, or {@code null}.
	 */
	@Nullable Member getMember();

	/**
	 * Retrieve the getter-method name.
	 * <p>
	 * Optional operation (may return {@code null})
	 *
	 * @return The name of the getter method, or {@code null}.
	 */
	@Nullable String getMethodName();

	/**
	 * Retrieve the getter-method.
	 * <p>
	 * Optional operation (may return {@code null})
	 *
	 * @return The getter method, or {@code null}.
	 */
	@Nullable Method getMethod();
}
