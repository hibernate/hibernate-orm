/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

import org.hibernate.Remove;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import jakarta.annotation.Nullable;

/**
 * The contract for getting the value of a persistent attribute from its container/owner.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated no longer used
 */
@Deprecated(since = "7.4", forRemoval = true)
@Remove // replace with a different SPI
public interface Getter {
	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the property value to be retrieved.
	 *
	 * @return The extracted value.
	 *
	 * @deprecated no longer used
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	@Nullable Object get(Object owner);

	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the value to be retrieved.
	 * @param mergeMap a map of merged persistent instances to detached instances
	 * @param session The session from which this request originated.
	 *
	 * @return The extracted value.
	 *
	 * @deprecated no longer used
	 */
	@Deprecated(since = "7.4", forRemoval = true)
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
