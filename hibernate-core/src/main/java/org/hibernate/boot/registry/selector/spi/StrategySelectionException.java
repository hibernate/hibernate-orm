/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem performing the selection/resolution.
 *
 * @author Steve Ebersole
 */
public class StrategySelectionException extends HibernateException {
	/**
	 * Constructs a StrategySelectionException using the specified message.
	 *
	 * @param message A message explaining the exception condition.
	 */
	public StrategySelectionException(String message) {
		super( message );
	}

	/**
	 * Constructs a StrategySelectionException using the specified message and cause.
	 *
	 * @param message A message explaining the exception condition.
	 * @param cause The underlying cause.
	 */
	public StrategySelectionException(String message, Throwable cause) {
		super( message, cause );
	}
}
