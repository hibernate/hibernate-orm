/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import jakarta.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class GetIdentifierTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testSimpleId() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Book book = new Book();
			em.persist( book );
			em.flush();
			assertEquals( book.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( book ) );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	@JiraKey(value = "HHH-7561")
	public void testProxyObject() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Book book = new Book();
			em.persist( book );
			em.flush();
			em.clear(); // Clear persistence context to receive proxy object below.
			Book proxy = em.getReference( Book.class, book.getId() );
			assertTrue( proxy instanceof HibernateProxy );
			assertEquals( book.getId(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( proxy ) );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Author author = new Author();
			Article article = new Article( author );
			em.persist( author );
			em.persist( article );
			em.flush();
			em.clear(); // Clear persistence context to receive proxy relation below.
			article = em.find( Article.class, article.getId() );
			assertTrue( article.getAuthor() instanceof HibernateProxy );
			assertEquals(
					author.getId(),
					em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( article.getAuthor() )
			);

		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testEmbeddedId() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Umbrella umbrella = new Umbrella();
			umbrella.setId( new Umbrella.PK() );
			umbrella.getId().setBrand( "Burberry" );
			umbrella.getId().setModel( "Red Hat" );
			em.persist( umbrella );
			em.flush();
			assertEquals(
					umbrella.getId(),
					em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( umbrella )
			);
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testIdClass() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Sickness sick = new Sickness();

			sick.setClassification( "H1N1" );
			sick.setType( "Flu" );
			em.persist( sick );
			em.flush();
			Sickness.PK id = new Sickness.PK();
			id.setClassification( sick.getClassification() );
			id.setType( sick.getType() );
			assertEquals( id, em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier( sick ) );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Umbrella.class,
				Sickness.class,
				Author.class,
				Article.class
		};
	}
}
