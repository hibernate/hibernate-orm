/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.onetoone.hhh9798;

import jakarta.persistence.PersistenceException;

import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

@JiraKey(value = "HHH-9798")
@DomainModel( annotatedClasses = { Shipment.class, Item.class } )
@SessionFactory
public class OneToOneJoinTableTest {

	@Test
	public void storeNonUniqueRelationship(SessionFactoryScope scope) {
		scope.inSession(
				(session) -> {
					try {
						Transaction tx = session.beginTransaction();

						Item someItem = new Item( "Some Item" );
						session.persist( someItem );

						Shipment shipment1 = new Shipment( someItem );
						session.persist( shipment1 );

						Shipment shipment2 = new Shipment( someItem );
						session.persist( shipment2 );

						tx.commit();

						fail();
					}
					catch (PersistenceException e) {
						assertTyping( ConstraintViolationException.class, e );
						// expected
					}
					finally {
						if ( session != null ) {
							session.getTransaction().rollback();
							session.close();
						}
					}
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete Shipment" ).executeUpdate();
					session.createQuery( "delete Item" ).executeUpdate();
				}
		);
	}
}
