//$
package org.hibernate.test.annotations.collectionelement.indexedCollection;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class IndexedCollectionOfElementsTest extends TestCase {

	public void testIndexedCollectionOfElements() throws Exception {
		Sale sale = new Sale();
		Contact contact = new Contact();
		contact.setName( "Emmanuel" );
		sale.getContacts().add(contact);
		Session s = openSession(  );
		s.getTransaction().begin();
		s.save( sale );
		s.flush();
		s.get( Sale.class, sale.getId() );
		assertEquals( 1, sale.getContacts().size() );
		s.getTransaction().rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Sale.class
		};
	}
}
