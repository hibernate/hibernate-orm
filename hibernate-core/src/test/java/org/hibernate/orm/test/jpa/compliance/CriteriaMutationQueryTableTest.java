/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@Jpa(
		annotatedClasses = {
				CriteriaMutationQueryTableTest.Animal.class,
				CriteriaMutationQueryTableTest.Person.class
		}
		,
		properties = {
				@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"),
		}
)
public class CriteriaMutationQueryTableTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Animal a = new Animal( 1, 12 );
					Animal a2 = new Animal( 2, 76 );
					Person p1 = new Person( 3, "Andrea", 50 );
					Person p2 = new Person( 4, "Ines", 13 );
					Person p3 = new Person( 5, "Luca", 53 );
					Person p4 = new Person( 6, "Lucia", 76 );
					Person p5 = new Person( 7, "Corrado", 80 );
					entityManager.persist( a );
					entityManager.persist( a2 );
					entityManager.persist( p1 );
					entityManager.persist( p2 );
					entityManager.persist( p3 );
					entityManager.persist( p4 );
					entityManager.persist( p5 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

	}

	@Test
	public void testUpdateQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaUpdate<Person> updateQuery = criteriaBuilder.createCriteriaUpdate( Person.class );
					final Root<Person> person = updateQuery.from( Person.class );
					updateQuery.set( person.get( "age" ), 0 );

					updateQuery.where( criteriaBuilder.lt( person.get( "age" ), 50 ) );

					Query query = entityManager.createQuery( updateQuery );

					updateQuery.where( criteriaBuilder.lt( person.get( "age" ), 100 ) );
					int updated = query.executeUpdate();
					assertEquals( 1, updated );
					updateQuery.where( criteriaBuilder.gt( person.get( "age" ), 53 ) );

					updated = entityManager.createQuery( updateQuery ).executeUpdate();
					assertEquals( 2, updated );

				}
		);
	}

	@Test
	public void testDeleteQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaDelete<Person> deleteQuery = criteriaBuilder
							.createCriteriaDelete( Person.class );

					final Root<Person> person = deleteQuery.from( Person.class );
					deleteQuery.where( criteriaBuilder.lt( person.get( "age" ), 50 ) );

					Query query = entityManager.createQuery( deleteQuery );

					deleteQuery.where( criteriaBuilder.lt( person.get( "age" ), 100 ) );

					int deleted = query.executeUpdate();
					assertEquals( 1, deleted );

					final Person p = entityManager.find( Person.class, 4 );
					assertNull( p );

					deleteQuery.where( criteriaBuilder.gt( person.get( "age" ), 53 ) );

					deleted = entityManager.createQuery( deleteQuery ).executeUpdate();
					assertEquals( 2, deleted );
				}
		);
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PRODUCT_TYPE", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue("Animal")
	public static class Animal {
		@Id
		private Integer id;

		private int age;

		private String details;

		public Animal() {
		}

		public Animal(Integer id, int age) {
			this.id = id;
			this.age = age;
		}
	}


	@Entity(name = "Person")
	@DiscriminatorValue("Person")
	public static class Person extends Animal {

		private String name;

		public Person() {
		}

		public Person(Integer id, String name, int age) {
			super( id, age );
			this.name = name;
		}
	}
}
