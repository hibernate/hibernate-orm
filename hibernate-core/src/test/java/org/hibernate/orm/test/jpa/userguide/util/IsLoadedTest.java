/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import jakarta.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class IsLoadedTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testIsLoadedOnPrivateSuperclassProperty() {
		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			Author a = new Author();
			Book book = new Book( a );
			em.persist( a );
			em.persist( book );
			em.flush();
			em.clear();
			book = em.find( Book.class, book.getId() );
			assertTrue( em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book ) );
			assertFalse( em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book, "author" ) );
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Author.class,
				Book.class,
				CopyrightableContent.class
		};
	}
}
