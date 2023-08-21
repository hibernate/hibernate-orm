/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ejb3configuration.id;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * @author Emmanuel Bernard
 */
public class IdentifierGeneratorStrategyProviderTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testIdentifierGeneratorStrategyProvider() {
		Map settings = ServiceRegistryUtil.createBaseSettings();
		settings.put(
				AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
				FunkyIdentifierGeneratorProvider.class.getName()
		);

		settings.put( org.hibernate.cfg.AvailableSettings.LOADED_CLASSES, Collections.singletonList( Cable.class ) );

		final EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				settings
		).build();

		final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
			entityManager.persist( new Cable() );
			entityManager.flush();
            Assertions.fail( "FunkyException should have been thrown when the id is generated" );
        }
        catch ( FunkyException e ) {
			entityManager.close();
            entityManagerFactory.close();
        }
    }
}
