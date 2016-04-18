/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import javax.persistence.PersistenceException;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

/**
 * @author Steve Ebersole
 */
public class Helper {

	public static PersistenceException persistenceException(
			PersistenceUnitDescriptor persistenceUnit,
			String message) {
		return persistenceException( persistenceUnit, message, null );
	}

	public static PersistenceException persistenceException(
			PersistenceUnitDescriptor persistenceUnit,
			String message,
			Exception cause) {
		return new PersistenceException(
				getExceptionHeader( persistenceUnit ) + message,
				cause
		);
	}

	private static String getExceptionHeader(PersistenceUnitDescriptor persistenceUnit) {
		return "[PersistenceUnit: " + persistenceUnit.getName() + "] ";
	}
}
