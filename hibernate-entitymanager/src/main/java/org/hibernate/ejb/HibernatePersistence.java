/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.HEMLogging;

/**
 * Hibernate EJB3 persistence provider implementation
 *
 * @deprecated Use {@link HibernatePersistenceProvider} instead
 *
 * @author Gavin King
 */
@Deprecated
public class HibernatePersistence extends HibernatePersistenceProvider implements PersistenceProvider, AvailableSettings {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( HibernatePersistence.class );

	public HibernatePersistence() {
	}

	@Override
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		logDeprecation();
		return super.createEntityManagerFactory( persistenceUnitName, properties );
	}

	protected void logDeprecation() {
		log.deprecatedPersistenceProvider(
				HibernatePersistence.class.getName(),
				HibernatePersistenceProvider.class.getName()
		);
	}

	@Override
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		logDeprecation();
		return super.createContainerEntityManagerFactory( info, properties );
	}

	@Override
	public void generateSchema(PersistenceUnitInfo info, Map map) {
		logDeprecation();
		super.generateSchema( info, map );
	}

	@Override
	public boolean generateSchema(String persistenceUnitName, Map map) {
		logDeprecation();
		return super.generateSchema( persistenceUnitName, map );
	}

	@Override
	public ProviderUtil getProviderUtil() {
		return super.getProviderUtil();
	}

	@Override
	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName,
			Map properties,
			ClassLoader providedClassLoader) {
		logDeprecation();
		return super.getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties, providedClassLoader );
	}

	@Override
	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(
			String persistenceUnitName,
			Map properties) {
		logDeprecation();
		return super.getEntityManagerFactoryBuilderOrNull( persistenceUnitName, properties );
	}
}
