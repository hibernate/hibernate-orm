/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import jakarta.persistence.PersistenceException;

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
