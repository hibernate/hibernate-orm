/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.PersistenceException;

import org.junit.Test;

import org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator;
import org.hibernate.cfg.naming.LegacyNamingStrategyDelegator;
import org.hibernate.cfg.naming.NamingStrategyDelegator;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.jpa.test.MyNamingStrategyDelegator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.MyNamingStrategy;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Gail Badner
 */
public class NamingStrategyDelegatorConfigurationTest extends BaseUnitTestCase {

	@Test
	public void testNamingStrategyDelegatorFromProperty() {

		// configure NamingStrategy
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( AvailableSettings.NAMING_STRATEGY, MyNamingStrategy.class.getName() )
			);
			assertEquals(
					MyNamingStrategy.class.getName(),
					builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY )
			);
			assertEquals( null, builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY_DELEGATOR ) );
			builder.build();
			final NamingStrategyDelegator namingStrategyDelegator =
					builder.getHibernateConfiguration().getNamingStrategyDelegator();
			assertTrue( LegacyNamingStrategyDelegator.class.isInstance( namingStrategyDelegator ) );
			assertTrue(
					MyNamingStrategy.class.isInstance(
							( (LegacyNamingStrategyDelegator)namingStrategyDelegator ).getNamingStrategy()
					)
			);
		}

		// configure ImprovedNamingStrategyDelegator
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap(
							AvailableSettings.NAMING_STRATEGY_DELEGATOR,
							ImprovedNamingStrategyDelegator.class.getName()
					)
			);
			assertEquals( null, builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY ) );
			builder.build();
			assertEquals(
					ImprovedNamingStrategyDelegator.class.getName(),
					builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY_DELEGATOR )
			);
			final NamingStrategyDelegator namingStrategyDelegator =
					builder.getHibernateConfiguration().getNamingStrategyDelegator();
			assertTrue( ImprovedNamingStrategyDelegator.class.isInstance( namingStrategyDelegator ) );
		}

		// configure LegacyNamingStrategyDelegator
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap(
							AvailableSettings.NAMING_STRATEGY_DELEGATOR,
							LegacyNamingStrategyDelegator.class.getName()
					)
			);
			assertEquals( null, builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY ) );
			builder.build();
			assertEquals(
					LegacyNamingStrategyDelegator.class.getName(),
					builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY_DELEGATOR )
			);
			final NamingStrategyDelegator namingStrategyDelegator =
					builder.getHibernateConfiguration().getNamingStrategyDelegator();
			assertTrue( LegacyNamingStrategyDelegator.class.isInstance( namingStrategyDelegator ) );
		}


		// configure custom NamingStrategyDelegator
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap(
							AvailableSettings.NAMING_STRATEGY_DELEGATOR,
							MyNamingStrategyDelegator.class.getName()
					)
			);
			assertEquals( null, builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY ) );
			assertEquals(
					MyNamingStrategyDelegator.class.getName(),
					builder.getConfigurationValues().get( AvailableSettings.NAMING_STRATEGY_DELEGATOR )
			);
			builder.build();
			final NamingStrategyDelegator namingStrategyDelegator =
					builder.getHibernateConfiguration().getNamingStrategyDelegator();
			assertTrue( MyNamingStrategyDelegator.class.isInstance( namingStrategyDelegator ) );
		}

		// configure NamingStrategy and NamingStrategyDelegator
		{
			PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
			final Map<String,String> integrationArgs = new HashMap<String,String>();
			integrationArgs.put( AvailableSettings.NAMING_STRATEGY, MyNamingStrategy.class.getName() );
			integrationArgs.put( AvailableSettings.NAMING_STRATEGY_DELEGATOR, MyNamingStrategyDelegator.class.getName() );
			try {
				EntityManagerFactoryBuilderImpl builder =  (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
						adapter,
						integrationArgs
				);
				builder.build();
				fail( "Should have thrown a PersistenceException because setting both properties is not allowed." );
			}
			catch (PersistenceException ex) {
				// expected
			}
		}
	}
}
