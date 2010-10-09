package org.hibernate.ejb.test.util;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class IsLoadedTest extends TestCase {

	public void testIsLoadedOnPrivateSuperclassProperty() {
		EntityManager em = factory.createEntityManager();
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
