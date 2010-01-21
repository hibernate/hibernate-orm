package org.hibernate.test.annotations.generics;

/**
 * A test case for ANN-494.
 *
 * @author Edward Costello
 * @author Paolo Perrotta
 */
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

public class EmbeddedGenericsTest extends TestCase {

	Session session;
	Classes.Edition<String> edition;

	public void setUp() throws Exception {
		super.setUp();
		session = openSession();
		session.getTransaction().begin();
		edition = new Classes.Edition<String>();
		edition.name = "Second";
	}

	public void testWorksWithGenericEmbedded() {
		Classes.Book b = new Classes.Book();
		b.edition = edition;
		persist( b );
		
		Classes.Book retrieved = (Classes.Book)find( Classes.Book.class, b.id );
		assertEquals( "Second", retrieved.edition.name );
		
		clean( Classes.Book.class, b.id );
		session.close();
	}

	public void testWorksWithGenericCollectionOfElements() {
		Classes.PopularBook b = new Classes.PopularBook();
		b.editions.add( edition );
		persist( b );

		Classes.PopularBook retrieved = (Classes.PopularBook)find( Classes.PopularBook.class, b.id );
		assertEquals( "Second", retrieved.editions.iterator().next().name );

		clean( Classes.PopularBook.class, b.id );
		session.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Classes.Book.class,
				Classes.PopularBook.class
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

}
