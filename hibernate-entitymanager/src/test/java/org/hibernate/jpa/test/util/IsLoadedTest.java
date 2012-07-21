/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
