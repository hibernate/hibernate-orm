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

import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.test.Distributor;
import org.hibernate.ejb.test.Item;

/**
 * @author Emmanuel Bernard
 */
public class InterceptorTest extends TestCase {

	public void testInjectedInterceptor() {
		configuration.setInterceptor( new ExceptionInterceptor() );
		EntityManagerFactory emf = configuration.createEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setName( "Laptop" );
		try {
			em.getTransaction().begin();
			em.persist( i );
			em.getTransaction().commit();
		}
		catch (IllegalStateException e) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) em.getTransaction().rollback();
			em.close();
			emf.close();
		}
	}

	public void testConfiguredInterceptor() {
		configuration.setProperty( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		EntityManagerFactory emf = configuration.createEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setName( "Laptop" );
		try {
			em.getTransaction().begin();
			em.persist( i );
			em.getTransaction().commit();
			fail( "No interceptor" );
		}
		catch (IllegalStateException e) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) em.getTransaction().rollback();
			em.close();
			emf.close();
		}
	}

	public void testConfiguredSessionInterceptor() {
		configuration.setProperty( AvailableSettings.SESSION_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
		configuration.setProperty( "aaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbbbbbb" );
		EntityManagerFactory emf = configuration.createEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setName( "Laptop" );
		try {
			em.getTransaction().begin();
			em.persist( i );
			em.getTransaction().commit();
			fail( "No interceptor" );
		}
		catch (IllegalStateException e) {
			assertEquals( LocalExceptionInterceptor.LOCAL_EXCEPTION_MESSAGE, e.getMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) em.getTransaction().rollback();
			em.close();
			emf.close();
		}
	}

	public void testEmptyCreateEntityManagerFactoryAndPropertyUse() {
		configuration.setProperty( AvailableSettings.INTERCEPTOR, ExceptionInterceptor.class.getName() );
		EntityManagerFactory emf = configuration.createEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setName( "Laptop" );
		try {
			em.getTransaction().begin();
			em.persist( i );
			em.getTransaction().commit();
			fail( "No interceptor" );
		}
		catch (IllegalStateException e) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) em.getTransaction().rollback();
			em.close();
			emf.close();
		}
	}

	public void testOnLoadCallInInterceptor() {
		configuration.setInterceptor( new ExceptionInterceptor(true) );
		EntityManagerFactory emf = configuration.createEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Item i = new Item();
		i.setName( "Laptop" );
		em.getTransaction().begin();
		em.persist( i );
		em.flush();
		em.clear();
		try {
			em.find(Item.class, i.getName() );
			fail( "No interceptor" );
		}
		catch (IllegalStateException e) {
			assertEquals( ExceptionInterceptor.EXCEPTION_MESSAGE, e.getMessage() );
		}
		finally {
			if ( em.getTransaction() != null && em.getTransaction().isActive() ) em.getTransaction().rollback();
			em.close();
			emf.close();
		}
	}

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Item.class,
				Distributor.class
		};
	}
}
