/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.test.SettingsGenerator;

import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class InterceptorTest {

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class
		};
	}
	
	private EntityManagerFactory entityManagerFactory;

	@After
	public void releaseResources() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
		}
	}

	public EntityManagerFactory entityManagerFactory() {
		return entityManagerFactory;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // test deprecated Interceptor settings

    @Test
    public void testDeprecatedConfiguredInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
    }

	@Test
	public void testDeprecatedConfiguredSessionInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.SESSION_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );

		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// test Interceptor settings

	@Test
    public void testConfiguredInterceptor() {
		Map settings = basicSettings();
		settings.put( org.hibernate.cfg.AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );
		
        Item i = new Item();
        i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
    }

    @Test
    public void testConfiguredSessionInterceptor() {
		Map settings = basicSettings();
		settings.put( org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );
		
        Item i = new Item();
        i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
    }

    @Test
    public void testConfiguredSessionInterceptorWithSessionFactory() {

		StandardServiceRegistryImpl standardRegistry = (StandardServiceRegistryImpl)
				new StandardServiceRegistryBuilder().build();

		SessionFactory sessionFactory = null;

		try {
			MetadataSources metadataSources = new MetadataSources( standardRegistry );
			for(Class annotatedClass : getAnnotatedClasses()) {
				metadataSources.addAnnotatedClass( annotatedClass );
			}

			Metadata metadata = metadataSources.getMetadataBuilder().build();

			SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

			sessionFactoryBuilder.applyStatelessInterceptor( LocalExceptionInterceptor.class );
			sessionFactory = sessionFactoryBuilder.build();

			final SessionFactory sessionFactoryInstance = sessionFactory;

			Supplier<SessionFactory> sessionFactorySupplier = () -> sessionFactoryInstance;

			Item i = new Item();
			i.setName( "Laptop" );

			try {
				doInHibernate( sessionFactorySupplier, session -> {
					session.persist( i );
					fail( "No interceptor" );
					return null;
				});
			}
			catch ( IllegalStateException e ) {
				assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
			}
		}
		finally {
			if(sessionFactory != null) {
				sessionFactory.close();
			}
			standardRegistry.destroy();
		}
	}

    @Test
    public void testConfiguredSessionInterceptorSupplier() {
        Map settings = basicSettings();
        settings.put( org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR, (Supplier<Interceptor>) LocalExceptionInterceptor::new);
		buildEntityManagerFactory( settings );
        
		Item i = new Item();
		i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
    }

    @Test
    public void testEmptyCreateEntityManagerFactoryAndPropertyUse() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		buildEntityManagerFactory( settings );
		
        Item i = new Item();
        i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );
				fail( "No interceptor" );
				return null;
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
    }

    @Test
    public void testOnLoadCallInInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, new ExceptionInterceptor( true ) );
		buildEntityManagerFactory( settings );
		
        Item i = new Item();
        i.setName( "Laptop" );

		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				entityManager.persist( i );

				entityManager.persist( i );
				entityManager.flush();
				entityManager.clear();
				try {
					entityManager.find( Item.class, i.getName() );
					fail( "No interceptor" );
				}
				catch ( IllegalStateException e ) {
					assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
				}
			});
		}
		catch ( IllegalStateException e ) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
    }


    protected Map basicSettings() {
		return SettingsGenerator.generateSettings(
				Environment.HBM2DDL_AUTO, "create-drop",
				Environment.USE_NEW_ID_GENERATOR_MAPPINGS, "true",
				Environment.DIALECT, Dialect.getDialect().getClass().getName(),
				AvailableSettings.LOADED_CLASSES, Arrays.asList( getAnnotatedClasses() )
		);
    }

	private void buildEntityManagerFactory(Map settings) {
		entityManagerFactory = Bootstrap
			.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings )
			.build();
	}

}
