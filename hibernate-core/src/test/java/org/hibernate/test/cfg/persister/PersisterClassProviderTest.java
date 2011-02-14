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
package org.hibernate.test.cfg.persister;

import junit.framework.TestCase;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.common.ServiceRegistryHolder;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersisterClassProviderTest extends UnitTestCase {
	public PersisterClassProviderTest(String string) {
		super( string );
	}

	public void testPersisterClassProvider() throws Exception {

		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		ServiceRegistryHolder serviceRegistryHolder = new ServiceRegistryHolder( cfg.getProperties() );
		//no exception as the GoofyPersisterClassProvider is not set
		SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistryHolder.getServiceRegistry());
		sessionFactory.close();


		cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		cfg.setPersisterClassProvider( new GoofyPersisterClassProvider() );
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			sessionFactory.close();
		}
		catch ( MappingException e ) {
			assertEquals(
					"The entity persister should be overridden",
					GoofyPersisterClassProvider.NoopEntityPersister.class,
					( (GoofyException) e.getCause() ).getValue()
			);
		}

		cfg = new Configuration();
		cfg.addAnnotatedClass( Portal.class );
		cfg.addAnnotatedClass( Window.class );
		cfg.setPersisterClassProvider( new GoofyPersisterClassProvider() );
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
			sessionFactory.close();
		}
		catch ( MappingException e ) {
			assertEquals(
					"The collection persister should be overridden but not the entity persister",
					GoofyPersisterClassProvider.NoopCollectionPersister.class,
					( (GoofyException) e.getCause() ).getValue() );
		}

		if ( serviceRegistryHolder != null ) {
			serviceRegistryHolder.destroy();
		}
	}
}
