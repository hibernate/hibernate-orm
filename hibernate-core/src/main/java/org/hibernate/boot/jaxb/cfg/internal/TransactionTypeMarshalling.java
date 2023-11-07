/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.cfg.internal;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.AccessType;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

/**
 * @author Steve Ebersole
 */
public class TransactionTypeMarshalling {
	public static PersistenceUnitTransactionType fromXml(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		return PersistenceUnitTransactionType.valueOf( name );
	}

	public static String toXml(PersistenceUnitTransactionType transactionType) {
		if ( transactionType == null ) {
			return null;
		}
		return transactionType.name();
	}
}
