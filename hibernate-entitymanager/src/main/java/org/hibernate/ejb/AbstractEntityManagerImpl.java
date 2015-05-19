/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ejb;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.jpa.internal.EntityManagerFactoryImpl;

/**
 * @deprecated Use {@link org.hibernate.jpa.spi.AbstractEntityManagerImpl} instead
 */
@Deprecated
@SuppressWarnings("unchecked")
public abstract class AbstractEntityManagerImpl
		extends org.hibernate.jpa.spi.AbstractEntityManagerImpl
		implements HibernateEntityManagerImplementor, Serializable {

	protected AbstractEntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType type,
			SynchronizationType synchronizationType,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		super( entityManagerFactory, type, synchronizationType, transactionType, properties );
	}
}
