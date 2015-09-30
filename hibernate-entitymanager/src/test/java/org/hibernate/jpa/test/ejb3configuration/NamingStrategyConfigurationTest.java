/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Collections;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.MyNamingStrategy;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;


/**
 * @author Gail Badner
 */
public class NamingStrategyConfigurationTest extends BaseUnitTestCase {

	@Test
	public void testNamingStrategyFromProperty() {

		// configure NamingStrategy
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( AvailableSettings.PHYSICAL_NAMING_STRATEGY, MyNamingStrategy.class.getName() )
			);
			builder.build();
			assertEquals(
					MyNamingStrategy.class.getName(),
					builder.getConfigurationValues().get( AvailableSettings.PHYSICAL_NAMING_STRATEGY )
			);

			assertTyping(
					MyNamingStrategy.class,
					builder.getMetadata().getMetadataBuildingOptions().getPhysicalNamingStrategy()
			);
		}
	}
}
