/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import java.util.Set;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.annotations.AnotherEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(H2Dialect.class)
public class MoreEntityManagerFactoryScopeTesting {

	@Jpa(
			annotatedClasses = {
					AnEntity.class
			}
	)
	@Test
	public void testBasicUsage(EntityManagerFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getEntityManagerFactory(), notNullValue() );
		// check we can use the EMF to create EMs
		scope.inTransaction(
				(session) -> session.createQuery( "select a from AnEntity a" ).getResultList()
		);
	}

	@Jpa(
			annotatedClasses = AnotherEntity.class,
			queryComplianceEnabled = true
	)
	@Test
	public void annotatedMethodTest(EntityManagerFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getEntityManagerFactory(), notNullValue() );
		Set<EntityType<?>> entities = scope.getEntityManagerFactory().getMetamodel().getEntities();
		assertEquals( 1, entities.size() );
		assertEquals( "AnotherEntity", entities.iterator().next().getName() );
		assertEquals( Boolean.TRUE, scope.getEntityManagerFactory().getProperties().get( "hibernate.jpa.compliance.query" ) );
		scope.inTransaction(
				entityManager -> {
					AnotherEntity aoe = new AnotherEntity( 2, "AnotherEntity_1" );
					entityManager.persist( aoe );
				}
		);
		scope.inTransaction(
				entityManager -> {
					AnotherEntity aoe = entityManager.find( AnotherEntity.class, 2 );
					assertNotNull( aoe );
					assertEquals( 2, aoe.getId() );
					assertEquals( "AnotherEntity_1", aoe.getName() );
				}
		);
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> scope.inTransaction(
						entityManager -> {
							AnEntity ae = entityManager.find( AnEntity.class, 1 );
						}
				)
		);
	}

}
