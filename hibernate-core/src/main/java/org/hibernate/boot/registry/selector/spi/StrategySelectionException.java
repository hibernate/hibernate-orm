/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem performing the selection/resolution.
 *
 * @author Steve Ebersole
 */
public class StrategySelectionException extends HibernateException {

	private final String implementationClassName;
	/**
	 * Constructs a StrategySelectionException using the specified message.
	 *
	 * @param message A message explaining the exception condition.
	 */
	public StrategySelectionException(String message, String implementationClassName) {
		super( message );
		this.implementationClassName = implementationClassName;
	}

	/**
	 * Constructs a StrategySelectionException using the specified message and cause.
	 *
	 * @param message A message explaining the exception condition.
	 * @param cause The underlying cause.
	 */
	public StrategySelectionException(String message, Throwable cause, String implementationClassName) {
		super( message, cause );
		this.implementationClassName = implementationClassName;
	}

	/**
	 * Gets the selected implementation class involved with the exception.
	 *
	 * @return the implementation class name.
	 */
	public String getImplementationClassName() {
		return implementationClassName;
	}
}
