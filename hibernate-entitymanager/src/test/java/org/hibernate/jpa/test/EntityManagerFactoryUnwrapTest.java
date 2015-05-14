//$Id$
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
package org.hibernate.jpa.test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerImpl;
import org.hibernate.testing.TestForIssue;

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
