/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.jpa.test.SettingsGenerator;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class InterceptorTest {

    @Test
    public void testConfiguredInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
        EntityManager em = emf.createEntityManager();
        Item i = new Item();
        i.setName( "Laptop" );
        try {
            em.getTransaction().begin();
            em.persist( i );
            em.getTransaction().commit();
            fail( "No interceptor" );
        }
        catch ( IllegalStateException e ) {
            assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
        }
        finally {
            if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }

    @Test
    public void testConfiguredSessionInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.SESSION_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
        EntityManager em = emf.createEntityManager();
        Item i = new Item();
        i.setName( "Laptop" );
        try {
            em.getTransaction().begin();
            em.persist( i );
            em.getTransaction().commit();
            fail( "No interceptor" );
        }
        catch ( IllegalStateException e ) {
            assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
        }
        finally {
            if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }

    @Test
    public void testEmptyCreateEntityManagerFactoryAndPropertyUse() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
		EntityManager em = emf.createEntityManager();
        Item i = new Item();
        i.setName( "Laptop" );
        try {
            em.getTransaction().begin();
            em.persist( i );
            em.getTransaction().commit();
            fail( "No interceptor" );
        }
        catch ( IllegalStateException e ) {
            assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
        }
        finally {
            if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }

    @Test
    public void testOnLoadCallInInterceptor() {
		Map settings = basicSettings();
		settings.put( AvailableSettings.INTERCEPTOR, new ExceptionInterceptor( true ) );
		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( new PersistenceUnitDescriptorAdapter(), settings ).build();
		EntityManager em = emf.createEntityManager();
        Item i = new Item();
        i.setName( "Laptop" );
        em.getTransaction().begin();
        em.persist( i );
        em.flush();
        em.clear();
        try {
            em.find( Item.class, i.getName() );
            fail( "No interceptor" );
        }
        catch ( IllegalStateException e ) {
            assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
        }
        finally {
            if ( em.getTransaction() != null && em.getTransaction().isActive() ) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
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

    public Class[] getAnnotatedClasses() {
        return new Class[] {
                Item.class,
                Distributor.class
        };
    }

}
