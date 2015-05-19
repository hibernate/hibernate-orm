/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
