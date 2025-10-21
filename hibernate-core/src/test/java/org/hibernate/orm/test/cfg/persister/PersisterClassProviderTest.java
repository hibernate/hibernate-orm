/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.persister;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@BaseUnitTest
public class PersisterClassProviderTest {

	@Test
	public void testPersisterClassProvider() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		ServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
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

		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		cfg = new Configuration();
		cfg.addAnnotatedClass( Gate.class );
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
			fail( "The entity persister should be overridden" );
		}
		catch (MappingException e) {
			// expected
			assertThat( e.getCause() ).isInstanceOf( GoofyException.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		assertThat( SessionFactoryRegistry.INSTANCE.hasRegistrations() ).isFalse();

		cfg = new Configuration();
		cfg.addAnnotatedClass( Portal.class );
		cfg.addAnnotatedClass( Window.class );
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
			fail( "The collection persister should be overridden but not the entity persister" );
		}
		catch (MappingException e) {
			// expected
			assertThat( e.getCause() ).isInstanceOf( GoofyException.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}


		cfg = new Configuration();
		cfg.addAnnotatedClass( Tree.class );
		cfg.addAnnotatedClass( Palmtree.class );
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( cfg.getProperties() )
				.addService( PersisterClassResolver.class, new GoofyPersisterClassProvider() )
				.build();
		try {
			sessionFactory = cfg.buildSessionFactory( serviceRegistry );
			sessionFactory.close();
			fail( "The entity persisters should be overridden in a class hierarchy" );
		}
		catch (MappingException e) {
			// expected
			assertThat( e.getCause() ).isInstanceOf( GoofyException.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}

		assertThat( SessionFactoryRegistry.INSTANCE.hasRegistrations() ).isFalse();
	}
}
