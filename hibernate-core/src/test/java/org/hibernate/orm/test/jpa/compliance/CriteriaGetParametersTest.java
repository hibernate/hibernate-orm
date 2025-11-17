/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.Collection;
import java.util.List;
import java.util.Set;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				CriteriaGetParametersTest.Person.class,
				CriteriaGetParametersTest.Address.class
		}
//		,
//		properties = @Setting(name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
)
public class CriteriaGetParametersTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( 1, "Andrea", 5 ) );
					entityManager.persist( new Person( 2, "Andrea", 35 ) );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> query = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = query.from( Person.class );

					query.select( person );
					query.where( criteriaBuilder.equal( person.get( "age" ), 30 ) );

					final Set<ParameterExpression<?>> parameters = query.getParameters();

					entityManager.createQuery( query ).getResultList();
					assertThat( parameters, notNullValue() );
					assertTrue( parameters.isEmpty() );
				}
		);
	}

	@Test
	public void likeExpStringExpTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cbuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Person> cquery = cbuilder.createQuery( Person.class );
					Root<Person> customer = cquery.from( Person.class );
					final EntityType<Person> Person_ = entityManager.getMetamodel().entity( Person.class );
					Join<Person, Address> a = customer.join( Person_.getCollection( "addresses", Address.class ) );
					cquery.where( cbuilder.like( a.get( "street" ), "sh\\_ll",
												cbuilder.literal( '\\' )
					) );
					cquery.select( customer );
					TypedQuery<Person> tquery = entityManager.createQuery( cquery );
					tquery.setMaxResults( 5 );
					List<Person> clist = tquery.getResultList();
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

		@ManyToMany
		@JoinTable(name = "PERSON_ADDRESSES_TABLE")
		private Collection<Address> addresses;

		Person() {
		}

		public Person(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}


		public Collection<Address> getAddresses() {
			return addresses;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		private Integer id;

		private String street;
	}
}
