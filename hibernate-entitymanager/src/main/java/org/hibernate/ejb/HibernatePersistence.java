/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
