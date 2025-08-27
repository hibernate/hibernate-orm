/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.association;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {
				Incident.class,
				IncidentStatus.class,
				Kitchen.class,
				Oven.class
		}
)
public class AssociationTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBidirOneToOne(EntityManagerFactoryScope scope) {
		final String id = "10";
		scope.inTransaction(
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
	public void testMergeAndBidirOneToOne(EntityManagerFactoryScope scope) {
		final Oven persistedOven = scope.fromTransaction(
				entityManager -> {
					Oven oven = new Oven();
					Kitchen kitchen = new Kitchen();
					entityManager.persist( oven );
					entityManager.persist( kitchen );
					kitchen.setOven( oven );
					oven.setKitchen( kitchen );
					return oven;
				} );

		Oven mergedOven = scope.fromTransaction(
				entityManager -> {
					return entityManager.merge( persistedOven );
				}
		);
	}

}
