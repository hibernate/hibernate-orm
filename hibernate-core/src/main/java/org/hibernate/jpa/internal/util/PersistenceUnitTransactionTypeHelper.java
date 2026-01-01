/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.util;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.cfg.PersistenceSettings;

import static jakarta.persistence.PersistenceUnitTransactionType.JTA;
import static jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL;

/**
 * Helper for dealing with {@linkplain PersistenceUnitTransactionType}, mainly
 * {@linkplain #interpretTransactionType interpretting} settings based on
 * {@value PersistenceSettings#JAKARTA_TRANSACTION_TYPE}.
 *
 * @see org.hibernate.boot.jaxb.cfg.internal.TransactionTypeMarshalling
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class PersistenceUnitTransactionTypeHelper {
	private PersistenceUnitTransactionTypeHelper() {
	}

	public static PersistenceUnitTransactionType interpretTransactionType(Object value) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof PersistenceUnitTransactionType transactionType ) {
			return transactionType;
		}
		else {
			final String stringValue = value.toString().trim();
			if ( stringValue.isEmpty() ) {
				return null;
			}
			else if ( JTA.name().equalsIgnoreCase( stringValue ) ) {
				return JTA;
			}
			else if ( RESOURCE_LOCAL.name().equalsIgnoreCase( stringValue ) ) {
				return RESOURCE_LOCAL;
			}
			else {
				throw new PersistenceException( "Unknown TransactionType: '" + stringValue + "'" );
			}
		}
	}
}
