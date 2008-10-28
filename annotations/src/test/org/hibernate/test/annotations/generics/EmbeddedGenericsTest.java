package org.hibernate.test.annotations.generics;

/**
 * A test case for ANN-494.
 *
 * @author Edward Costello
 * @author Paolo Perrotta
 */
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.test.annotations.TestCase;

public class EmbeddedGenericsTest extends TestCase {

	Session session;
	Edition<String> edition;

	public void setUp() throws Exception {
		super.setUp();
		session = openSession();
		session.getTransaction().begin();
		edition = new Edition<String>();
		edition.name = "Second";
	}

	public void testWorksWithGenericEmbedded() {
		Book b = new Book();
		b.edition = edition;
		persist( b );
		
		Book retrieved = (Book)find( Book.class, b.id );
		assertEquals( "Second", retrieved.edition.name );
		
		clean( Book.class, b.id );
		session.close();
	}

	public void testWorksWithGenericCollectionOfElements() {
		PopularBook b = new PopularBook();
		b.editions.add( edition );
		persist( b );

		PopularBook retrieved = (PopularBook)find( PopularBook.class, b.id );
		assertEquals( "Second", retrieved.editions.iterator().next().name );

		clean( PopularBook.class, b.id );
		session.close();
	}

	protected Class[] getMappings() {
		return new Class[]{
				Book.class,
				PopularBook.class
		};
	}

	private void persist(Object data) {
		session.persist( data );
		session.getTransaction().commit();
		session.clear();
	}
	
	private Object find(Class clazz, Long id) {
		return session.get( clazz, id );
	}

	private void clean(Class<?> clazz, Long id) {
		Transaction tx = session.beginTransaction();
		session.delete( find( clazz, id ) );
		tx.commit();
	}

	@Embeddable
	public static class Edition<T> {
		T name;
	}
	
	@Entity
	public static class Book {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@Embedded
		Edition<String> edition;
	}
	
	@Entity
	public static class PopularBook {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@CollectionOfElements
		Set<Edition<String>> editions = new HashSet<Edition<String>>();
	}
}
