/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.indexedCollection;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Sale.class
		}
)
@SessionFactory
public class IndexedCollectionOfElementsTest {

	@Test
	public void testIndexedCollectionOfElements(SessionFactoryScope scope) {
		Sale sale = new Sale();
		Contact contact = new Contact();
		contact.setName( "Emmanuel" );
		sale.getContacts().add( contact );

		scope.inTransaction(
				session -> {
					session.persist( sale );
					session.flush();
					session.get( Sale.class, sale.getId() );
					assertEquals( 1, sale.getContacts().size() );
				}
		);
	}
}
