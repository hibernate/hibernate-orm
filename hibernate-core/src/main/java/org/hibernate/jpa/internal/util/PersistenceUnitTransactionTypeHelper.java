/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.util;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;

import static jakarta.persistence.PersistenceUnitTransactionType.JTA;
import static jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL;

/**
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

	@SuppressWarnings("removal")
	public static jakarta.persistence.spi.PersistenceUnitTransactionType toDeprecatedForm(PersistenceUnitTransactionType type) {
		return type == null ? null : switch (type) {
			case JTA -> jakarta.persistence.spi.PersistenceUnitTransactionType.JTA;
			case RESOURCE_LOCAL -> jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
		};
	}

	@SuppressWarnings("removal")
	public static PersistenceUnitTransactionType toNewForm(jakarta.persistence.spi.PersistenceUnitTransactionType type) {
		return type == null ? null : switch (type) {
			case JTA -> PersistenceUnitTransactionType.JTA;
			case RESOURCE_LOCAL -> PersistenceUnitTransactionType.RESOURCE_LOCAL;
		};
	}
}
