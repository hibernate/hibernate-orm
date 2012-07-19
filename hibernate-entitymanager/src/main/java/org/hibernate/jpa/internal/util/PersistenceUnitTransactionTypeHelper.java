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
package org.hibernate.jpa.internal.util;

import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitTransactionTypeHelper {
	private PersistenceUnitTransactionTypeHelper() {
	}

	public static PersistenceUnitTransactionType interpretTransactionType(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( PersistenceUnitTransactionType.class.isInstance( value ) ) {
			return (PersistenceUnitTransactionType) value;
		}

		final String stringValue = value.toString();
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		else if ( stringValue.equalsIgnoreCase( "JTA" ) ) {
			return PersistenceUnitTransactionType.JTA;
		}
		else if ( stringValue.equalsIgnoreCase( "RESOURCE_LOCAL" ) ) {
			return PersistenceUnitTransactionType.RESOURCE_LOCAL;
		}
		else {
			throw new PersistenceException( "Unknown TransactionType: " + stringValue );
		}
	}
}
