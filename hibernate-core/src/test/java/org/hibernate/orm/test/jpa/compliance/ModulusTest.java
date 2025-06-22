/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = ModulusTest.Person.class,
		properties = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
)
public class ModulusTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					for ( int i = 0; i < 10; i++ ) {
						Person person;
						if ( i == 3 ) {
							person = new Person( i, "Andrea", 5 );
						}
						else if ( i == 4 ) {
							person = new Person( i, "Andrew", 5 );
						}
						else {
							person = new Person( i, "Luigi " + i, 42 );
						}
						entityManager.persist( person );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testCriteriaMod(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> person = query.from( Person.class );
					query.select( person.get( "id" ) );

					final EntityType<Person> Person_ = entityManager.getMetamodel().entity( Person.class );

					final Expression<Integer> mod = criteriaBuilder.mod(
							criteriaBuilder.literal( 45 ),
							criteriaBuilder.literal( 10 )
					);
					final Path<Integer> ageAttribute = person.get( Person_.getSingularAttribute(
							"age",
							Integer.class
					) );

					query.where( criteriaBuilder.equal( mod, ageAttribute ) );

					final List<Integer> ids = entityManager.createQuery( query ).getResultList();

					assertEquals( 2, ids.size() );
					assertTrue( ids.contains( 3 ) );
					assertTrue( ids.contains( 4 ) );
				}
		);
	}

	@Test
	public void testQueryMod(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final List<Integer> ids = entityManager.createQuery( "select p.id from Person p where p.age = 45%40" )
							.getResultList();

					assertEquals( 2, ids.size() );
					assertTrue( ids.contains( 3 ) );
					assertTrue( ids.contains( 4 ) );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		private Integer age;

		Person() {
		}

		public Person(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
