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
package org.hibernate.jpa.boot.spi;

import java.util.Map;

import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

/**
 * Entry into the bootstrap process.
 *
 * @author Steve Ebersole
 */
public final class Bootstrap {
	/**
	 * Builds and returns an EntityManagerFactoryBuilder that can be used to then create an
	 * {@link javax.persistence.EntityManagerFactory}.  Essentially this represents phase 1 of a 2 phase
	 * {@link javax.persistence.EntityManagerFactory} building process.
	 *
	 * @param persistenceUnitDescriptor The persistence-unit description.  Note that this is the Hibernate abstraction
	 * hiding where this info comes from.
	 * @param integration The map of integration settings.  Generally speaking, integration settings take precedence
	 * over persistence-unit settings.
	 *
	 * @return The {@link javax.persistence.EntityManagerFactory} builder.
	 */
	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map integration) {
		return new EntityManagerFactoryBuilderImpl( persistenceUnitDescriptor, integration );
	}

	/**
	 * Builds and returns an EntityManagerFactoryBuilder that can be used to then create an
	 * {@link javax.persistence.EntityManagerFactory}.  Essentially this represents phase 1 of a 2 phase
	 * {@link javax.persistence.EntityManagerFactory} building process.
	 *
	 * This form accepts the JPA container bootstrap persistence-unit descriptor representation
	 * ({@link PersistenceUnitInfo}) and wraps it in our {@link PersistenceUnitDescriptor} abstraction.  It then
	 * just delegates the call to {@link #getEntityManagerFactoryBuilder(PersistenceUnitDescriptor, Map)}
	 *
	 * @param persistenceUnitInfo The persistence-unit description as defined by the JPA PersistenceUnitInfo contract
	 * @param integration The map of integration settings.  Generally speaking, integration settings take precedence
	 * over persistence-unit settings.
	 *
	 * @return The {@link javax.persistence.EntityManagerFactory} builder.
	 */
	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitInfo persistenceUnitInfo,
			Map integration) {
		return getEntityManagerFactoryBuilder( new PersistenceUnitInfoDescriptor( persistenceUnitInfo ), integration );
	}
}
