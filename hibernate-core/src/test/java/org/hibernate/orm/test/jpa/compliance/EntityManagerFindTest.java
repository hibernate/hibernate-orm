/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.Date;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TransactionRequiredException;

@Jpa(
		annotatedClasses = {EntityManagerFindTest.TestEntity.class,
		EntityManagerFindTest.TestEntity2.class},
		properties = @Setting( name = AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, value = "true")
)
public class EntityManagerFindTest {

	@Test
	public void testFindWrongTypeAsId(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() ->
										entityManager.find( TestEntity.class, "ID" )
						)
		);
	}

	@Test
	public void testFindNullId(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() ->
										entityManager.find( TestEntity.class, null )
						)
		);
	}

	@Test
	public void testFindPessimisticLockNoTransation(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								TransactionRequiredException.class,
								() ->
										entityManager.find( TestEntity.class, 1, LockModeType.PESSIMISTIC_READ )

						)
		);
	}


	@Test
	public void testFindNonExistingEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager ->
						Assertions.assertThrows(
								IllegalArgumentException.class,
								() ->
										entityManager.find( NonExistingEntity.class, 1 )
						)

		);

	}

	@Test
	public void findJavaUtilDateAsId(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					entityManager.find( TestEntity2.class, new Date() );
				}
		);
	}

	public static class NonExistingEntity {

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private int id;

		private String name;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@Temporal(TemporalType.DATE)
		protected java.util.Date id;

		private String name;
	}
}
