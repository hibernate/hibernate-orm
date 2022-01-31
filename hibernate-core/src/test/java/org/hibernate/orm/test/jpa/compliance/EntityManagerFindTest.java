/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TransactionRequiredException;

@Jpa(
		annotatedClasses = EntityManagerFindTest.TestEntity.class
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

	public static class NonExistingEntity {

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private int id;

		private String name;
	}
}
