/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.jpa.test.ejb3configuration.id;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class IdentifierGeneratorStrategyProviderTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testIdentifierGeneratorStrategyProvider() {
		Map settings = new HashMap();
		settings.put(
				AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
				FunkyIdentifierGeneratorProvider.class.getName()
		);
		settings.put( AvailableSettings.LOADED_CLASSES, Collections.singletonList( Cable.class ) );

		final EntityManagerFactory entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(
				new PersistenceUnitInfoAdapter(),
				settings
		).build();

		final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
			entityManager.persist( new Cable() );
			entityManager.flush();
            Assert.fail( "FunkyException should have been thrown when the id is generated" );
        }
        catch ( FunkyException e ) {
			entityManager.close();
            entityManagerFactory.close();
        }
    }
}
