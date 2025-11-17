/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.annotations.methods;

import java.util.Set;

import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.annotations.AnEntity;
import org.hibernate.testing.annotations.AnotherEntity;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				AnEntity.class
		}
)
public class EntityManagerFactoryScopeTesting {

	@BeforeAll
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					AnEntity ae = new AnEntity(1, "AnEntity_1");
					entityManager.persist( ae );
				}
		);
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
			entityManager -> entityManager.createQuery( "delete from AnEntity" ).executeUpdate()
		);
	}

	@Test
	public void testBasicUsage(EntityManagerFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getEntityManagerFactory(), notNullValue() );
		// check we can use the EMF to create EMs
		scope.inTransaction(
				(session) -> session.createQuery( "select a from AnEntity a" ).getResultList()
		);
	}

	@Test
	public void nonAnnotatedMethodTest(EntityManagerFactoryScope scope) {
		Set<EntityType<?>> entities = scope.getEntityManagerFactory().getMetamodel().getEntities();
		assertEquals( 1, entities.size() );
		assertEquals( "AnEntity", entities.iterator().next().getName() );
		scope.inEntityManager(
				entityManager -> {
					AnEntity ae = entityManager.find( AnEntity.class, 1 );
					assertNotNull( ae );
					assertEquals( 1, ae.getId() );
					assertEquals( "AnEntity_1", ae.getName() );
				}
		);
	}

	@Jpa(
			annotatedClasses = AnotherEntity.class,
			integrationSettings = {@Setting(name = JpaComplianceSettings.JPA_QUERY_COMPLIANCE, value = "true")}
	)
	@Test
	public void annotatedMethodTest(EntityManagerFactoryScope scope) {
		assertThat( scope, notNullValue() );
		assertThat( scope.getEntityManagerFactory(), notNullValue() );
		Set<EntityType<?>> entities = scope.getEntityManagerFactory().getMetamodel().getEntities();
		assertEquals( 1, entities.size() );
		assertEquals( "AnotherEntity", entities.iterator().next().getName() );
		assertEquals( "true", scope.getEntityManagerFactory().getProperties().get( "hibernate.jpa.compliance.query" ) );
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
						entityManager -> 	entityManager.find( AnEntity.class, 1 )
				)
		);
	}

}
