/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.indexedCollection;


import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class IndexedCollectionOfElementsTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testIndexedCollectionOfElements() {
		Sale sale = new Sale();
		Contact contact = new Contact();
		contact.setName( "Emmanuel" );
		sale.getContacts().add( contact );
		inTransaction(
				session -> {
					session.save( sale );
					session.flush();
					session.get( Sale.class, sale.getId() );
					assertEquals( 1, sale.getContacts().size() );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Sale.class
		};
	}
}
