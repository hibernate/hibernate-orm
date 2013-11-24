/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
