/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * @author Brett Meyer
 */
public final class Bootstrap {
	private Bootstrap() {
	}

	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map integration) {
		return new EntityManagerFactoryBuilderImpl( persistenceUnitDescriptor, integration );
	}
	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map integration,
			ClassLoader providedClassLoader) {
		return new EntityManagerFactoryBuilderImpl( persistenceUnitDescriptor, integration, providedClassLoader );
	}

	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitInfo persistenceUnitInfo,
			Map integration) {
		return getEntityManagerFactoryBuilder( new PersistenceUnitInfoDescriptor( persistenceUnitInfo ), integration );
	}

	public static EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(
			PersistenceUnitInfo persistenceUnitInfo,
			Map integration,
			ClassLoader providedClassLoader) {
		return getEntityManagerFactoryBuilder( new PersistenceUnitInfoDescriptor( persistenceUnitInfo ), integration, providedClassLoader );
	}
}
