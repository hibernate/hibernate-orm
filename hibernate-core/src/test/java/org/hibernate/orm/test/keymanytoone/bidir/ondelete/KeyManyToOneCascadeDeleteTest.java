/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.bidir.ondelete;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Lukasz Antoniak
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/keymanytoone/bidir/ondelete/Mapping.hbm.xml"
)
@SessionFactory
public class KeyManyToOneCascadeDeleteTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-7807")
	public void testEmbeddedCascadeRemoval(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Customer customer = new Customer( "Lukasz" );
					Order order1 = new Order( customer, 1L );
					order1.setItem( "laptop" );
					Order order2 = new Order( customer, 2L );
					order2.setItem( "printer" );
					session.persist( customer );
					session.persist( order1 );
					session.persist( order2 );
					session.getTransaction().commit();

					// Removing customer cascades to associated orders.
					session.getTransaction().begin();
					customer = session.get( Customer.class, customer.getId() );
					session.remove( customer );
					session.getTransaction().commit();

					session.getTransaction().begin();

					assertThat(
							session.createQuery( "select count(*) from Customer" ).uniqueResult(),
							is( 0L )
					);
					assertThat(
							session.createQuery( "select count(*) from Order" ).uniqueResult(),
							is( 0L )
					);
				}
		);
	}
}
