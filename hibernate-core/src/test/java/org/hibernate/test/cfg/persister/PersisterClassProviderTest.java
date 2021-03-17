/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public class PersisterClassProviderTest extends BaseUnitTestCase {
	@Test
	public void testPersisterClassProvider() throws Exception {

		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.build();
		//no exception as the GoofyPersisterClassProvider is not set
		SessionFactory sessionFactory;
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
            fail("The entity persister should be overridden");
		}
		catch ( MappingException e ) {
			assertEquals(
					"The entity persister should be overridden",
					GoofyPersisterClassProvider.NoopEntityPersister.class,
					( (GoofyException) e.getCause() ).getValue()
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );

		cfg = new Configuration();
		cfg.addAnnotatedClass( Portal.class );
		cfg.addAnnotatedClass( Window.class );
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
            fail("The collection persister should be overridden but not the entity persister");
		}
		catch ( MappingException e ) {
			assertEquals(
					"The collection persister should be overridden but not the entity persister",
					GoofyPersisterClassProvider.NoopCollectionPersister.class,
					( (GoofyException) e.getCause() ).getValue() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}


        cfg = new Configuration();
		cfg.addAnnotatedClass( Tree.class );
		cfg.addAnnotatedClass( Palmtree.class );
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
            fail("The entity persisters should be overridden in a class hierarchy");
		}
		catch ( MappingException e ) {
			assertEquals(
					"The entity persisters should be overridden in a class hierarchy",
					GoofyPersisterClassProvider.NoopEntityPersister.class,
					( (GoofyException) e.getCause() ).getValue() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );
	}
}
