/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.util;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

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
		Author a = new Author();
		Book book = new Book(a);
		em.persist( a );
		em.persist( book );
		em.flush();
		em.clear();
		book = em.find( Book.class, book.getId() );
		assertTrue( em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book ) );
		assertFalse( em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded( book, "author" ) );
		em.getTransaction().rollback();
		em.close();
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
