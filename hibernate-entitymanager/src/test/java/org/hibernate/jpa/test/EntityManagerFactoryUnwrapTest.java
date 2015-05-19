/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.jpa.test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerImpl;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test various unwrap scenarios for {@code EntityManagerFactory}.
 */
@TestForIssue(jiraKey = "HHH-9665")
public class EntityManagerFactoryUnwrapTest extends BaseEntityManagerFunctionalTestCase {
	private EntityManagerFactory entityManagerFactory;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { };
	}

	@Before
	public void setUp() {
		entityManagerFactory = entityManagerFactory();
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToSessionFactory() {
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );
		assertNotNull( "Unwrapping to API class SessionFactory should be ok", sessionFactory );
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToSessionFactoryImplementor() {
		SessionFactoryImplementor sessionFactoryImplementor = entityManagerFactory.unwrap( SessionFactoryImplementor.class );
		assertNotNull( "Unwrapping to SPI class SessionFactoryImplementor should be ok", sessionFactoryImplementor );
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToDeprecatedHibernateEntityManagerFactory() {
		HibernateEntityManagerFactory hibernateEntityManagerFactory = entityManagerFactory.unwrap(
				HibernateEntityManagerFactory.class
		);
		assertNotNull(
				"Unwrapping to SPI class HibernateEntityManagerFactory should be ok",
				hibernateEntityManagerFactory
		);
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToHibernateEntityManagerFactory() {
		org.hibernate.jpa.HibernateEntityManagerFactory hibernateEntityManagerFactory = entityManagerFactory.unwrap( org.hibernate.jpa.HibernateEntityManagerFactory.class );
		assertNotNull(
				"Unwrapping to SPI class HibernateEntityManagerFactory should be ok",
				hibernateEntityManagerFactory
		);
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToObject() {
		Object object = entityManagerFactory.unwrap( Object.class );
		assertNotNull( "Unwrapping to public super type Object should work", object );
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToSessionFactoryImpl() {
		SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap( SessionFactoryImpl.class );
		assertNotNull(
				"Unwrapping to SessionFactoryImpl should be ok",
				sessionFactory
		);
	}

	@Test
	public void testEntityManagerCanBeUnwrappedToEntityManagerFactoryImpl() {
		EntityManagerFactoryImpl entityManager = entityManagerFactory.unwrap( EntityManagerFactoryImpl.class );
		assertNotNull(
				"Unwrapping to EntityManagerFactoryImpl should be ok",
				entityManager
		);
	}

	@Test
	public void testEntityManagerCannotBeUnwrappedToUnrelatedType() {
		try {
			entityManagerFactory.unwrap( EntityManagerImpl.class );
			fail( "It should not be possible to unwrap to unrelated type." );
		}
		catch ( PersistenceException e ) {
			// ignore
		}
	}
}
