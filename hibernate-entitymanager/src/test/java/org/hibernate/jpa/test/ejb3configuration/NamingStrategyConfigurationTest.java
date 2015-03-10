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
