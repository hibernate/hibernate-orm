//$Id$
package org.hibernate.ejb.test.ejb3configuration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.HibernatePersistence;
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
		configuration.setProperty( HibernatePersistence.INTERCEPTOR, ExceptionInterceptor.class.getName() );
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
		configuration.setProperty( HibernatePersistence.SESSION_INTERCEPTOR, LocalExceptionInterceptor.class.getName() );
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
		configuration.setProperty( HibernatePersistence.INTERCEPTOR, ExceptionInterceptor.class.getName() );
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
