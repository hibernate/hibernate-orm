/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.cid.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = { Customer.class, Order.class } )
@SessionFactory
public class IdClassQueryRefTests {

	@Test
	public void testHqlVirtualIdReferences(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Order o where o.orderNumber = 123" ).list();
			session.createQuery( "from Order o where o.customer.id = 1" ).list();
		} );
	}

	@Test
	public void testHqlIdClassReferences(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Order o where o.id.orderNumber = 123" ).list();
		} );
	}

	@Test
	@FailureExpected(
			reason = "Mismatched expectations - some parts of the translation expect this to be the key-many-to-one" +
					" while other parts expect the basic IdClass reference"
	)
	public void testHqlIdClassReferencesBug(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Order o where o.id.customer = 1" ).list();
		} );
	}

	@Test
	@FailureExpected( reason = "This is an invalid query" )
	public void testHqlInvalidIdClassReferences(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Order o where o.id.customer.id = 1" ).list();
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Customer acme = new Customer( 1, "acme" );
			final Customer spacely = new Customer( 2, "spacely" );
			session.persist( acme );
			session.persist( spacely );

			final Order acmeOrder1 = new Order( acme, 1, 123F );
			final Order acmeOrder2 = new Order( acme, 2, 123F );
			session.persist( acmeOrder1 );
			session.persist( acmeOrder2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
