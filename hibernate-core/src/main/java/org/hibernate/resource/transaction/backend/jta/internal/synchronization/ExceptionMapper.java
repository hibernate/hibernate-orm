/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import java.io.Serializable;
import jakarta.transaction.SystemException;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * A pluggable strategy for defining how the {@link jakarta.transaction.Synchronization} registered by Hibernate handles
 * exceptions.
 *
 * @author Steve Ebersole
 */
public interface ExceptionMapper extends Serializable {
	/**
	 * Map a JTA {@link jakarta.transaction.SystemException} to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param systemException The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	RuntimeException mapStatusCheckFailure(String message, SystemException systemException, SessionImplementor sessionImplementor);

	/**
	 * Map an exception encountered during a managed flush to the appropriate runtime-based exception.
	 *
	 * @param message The message to use for the returned exception
	 * @param failure The causal exception
	 *
	 * @return The appropriate exception to throw
	 */
	RuntimeException mapManagedFlushFailure(String message, RuntimeException failure, SessionImplementor session);
}
