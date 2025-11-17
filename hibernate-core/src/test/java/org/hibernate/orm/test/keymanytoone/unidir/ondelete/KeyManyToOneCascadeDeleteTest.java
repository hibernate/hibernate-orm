/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.unidir.ondelete;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@DomainModel(xmlMappings = {
		"org/hibernate/orm/test/keymanytoone/unidir/ondelete/Mapping.hbm.xml"
})
@SessionFactory
public class KeyManyToOneCascadeDeleteTest {

	@Test
	@JiraKey(value = "HHH-7807")
	public void testComponentCascadeRemoval(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Customer customer = new Customer( "Lukasz" );
					Order order1 = new Order( new Order.Id( customer, 1L ) );
					order1.setItem( "laptop" );
					Order order2 = new Order( new Order.Id( customer, 2L ) );
					order2.setItem( "printer" );
					session.getTransaction().begin();
					try {
						session.persist( customer );
						session.persist( order1 );
						session.persist( order2 );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					// Removing customer cascades to associated orders.
					session.getTransaction().begin();
					try {
						customer = session.get( Customer.class, customer.getId() );
						session.remove( customer );
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					session.getTransaction().begin();
					try {
						assertEquals(
								"0",
								session.createQuery( "select count(*) from Customer" )
										.uniqueResult()
										.toString()
						);
						assertEquals(
								"0",
								session.createQuery( "select count(*) from Order" ).uniqueResult().toString()
						);
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

				}
		);
	}
}
