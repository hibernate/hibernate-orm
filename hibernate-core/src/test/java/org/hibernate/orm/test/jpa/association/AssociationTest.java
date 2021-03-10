/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.association;


import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
public class AssociationTest extends EntityManagerFactoryBasedFunctionalTest {

	@AfterEach
	public void tearDown() {
		inTransaction(
			entityManager -> {
				entityManager.createQuery( "delete from Incident" ).executeUpdate();
				entityManager.createQuery( "delete from IncidentStatus" ).executeUpdate();
				entityManager.createQuery( "delete from Oven" ).executeUpdate();
				entityManager.createQuery( "delete from Kitchen" ).executeUpdate();
			}
		);
	}

	@Test
	public void testBidirOneToOne() {
		final String id = "10";
		inTransaction(
				entityManager -> {
					Incident i = entityManager.find( Incident.class, id );
					if ( i == null ) {
						i = new Incident( id );
						IncidentStatus ist = new IncidentStatus( id );
						i.setIncidentStatus( ist );
						ist.setIncident( i );
						entityManager.persist( i );
					}
				} );
	}

	@Test
	public void testMergeAndBidirOneToOne() {
		final Oven persistedOven = fromTransaction(
				entityManager -> {
					Oven oven = new Oven();
					Kitchen kitchen = new Kitchen();
					entityManager.persist( oven );
					entityManager.persist( kitchen );
					kitchen.setOven( oven );
					oven.setKitchen( kitchen );
					return oven;
				} );

		Oven mergedOven = fromTransaction(
				entityManager -> {
					return entityManager.merge( persistedOven );
				}
		);
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Incident.class,
				IncidentStatus.class,
				Kitchen.class,
				Oven.class
		};
	}
}
