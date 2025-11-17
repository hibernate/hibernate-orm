/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.cfg.internal;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("removal")
public class TransactionTypeMarshalling {
	public static PersistenceUnitTransactionType fromXml(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		return PersistenceUnitTransactionType.valueOf( name );
	}

	public static String toXml(PersistenceUnitTransactionType transactionType) {
		return transactionType == null ? null : transactionType.name();
	}
}
