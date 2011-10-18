/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.test.ejb3configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.test.Distributor;
import org.hibernate.ejb.test.Item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class InterceptorTest {
    @Test
    public void testInjectedInterceptor() {
        EntityManagerFactory emf = constructConfiguration().setInterceptor( new ExceptionInterceptor() )
                .createEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        Item i = new Item();
        i.setName( "Laptop" );
        try {
            em.getTransaction().begin();
            em.persist( i );
            em.getTransaction().commit();
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
    public void testConfiguredInterceptor() {
        EntityManagerFactory emf = constructConfiguration().setProperty(
                AvailableSettings.INTERCEPTOR,
                ExceptionInterceptor.class.getName()
        ).createEntityManagerFactory();
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
        EntityManagerFactory emf = constructConfiguration().setProperty(
                AvailableSettings.SESSION_INTERCEPTOR,
                LocalExceptionInterceptor.class.getName()
        ).setProperty( "aaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbbbb" ).createEntityManagerFactory();
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
        EntityManagerFactory emf = constructConfiguration().setProperty(
                AvailableSettings.INTERCEPTOR,
                ExceptionInterceptor.class.getName()
        ).createEntityManagerFactory();
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
        EntityManagerFactory emf = constructConfiguration().setInterceptor( new ExceptionInterceptor( true ) )
                .createEntityManagerFactory();
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


    protected Ejb3Configuration constructConfiguration() {
        Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
        ejb3Configuration.getHibernateConfiguration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
        ejb3Configuration
                .getHibernateConfiguration()
                .setProperty( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
        ejb3Configuration
                .getHibernateConfiguration()
                .setProperty( Environment.DIALECT, Dialect.getDialect().getClass().getName() );
        for ( Class clazz : getAnnotatedClasses() ) {
            ejb3Configuration.addAnnotatedClass( clazz );
        }
        return ejb3Configuration;
    }

    public Class[] getAnnotatedClasses() {
        return new Class[] {
                Item.class,
                Distributor.class
        };
    }

}
