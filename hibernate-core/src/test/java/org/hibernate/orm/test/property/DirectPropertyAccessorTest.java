/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property;


import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Rudolf
 */
@DomainModel(annotatedClasses = {
		Order.class,
		Item.class
})
@SessionFactory
public class DirectPropertyAccessorTest {
	@Test
	@JiraKey( value="HHH-3718" )
	public void testDirectIdPropertyAccess(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( s -> {
			Item i = new Item();
			s.persist( i );
			Order o = new Order();
			o.setOrderNumber( 1 );
			o.getItems().add( i );
			s.persist( o );
			s.flush();
			s.clear();

			o = ( Order ) s.getReference( Order.class, 1 );
			assertFalse( Hibernate.isInitialized( o ) );
			o.getOrderNumber();
			// If you mapped with field access, any method, except id, call initializes the proxy
			assertFalse( Hibernate.isInitialized( o ) );
			o.getName();
			assertTrue( Hibernate.isInitialized( o ) );
		} );
	}
}
