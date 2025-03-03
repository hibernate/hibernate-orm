/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

@Jpa(
		annotatedClasses = {
				GroupByTest.Address.class,
				GroupByTest.Person.class
		}
)
@JiraKey( value = "HHH-15749")
public class GroupByTest {

	@Test
	public void testGroupBy(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Person> query = criteriaBuilder.createQuery( Person.class );
					Root<Person> root = query.from( Person.class );

					query.groupBy(
							root.get( "address1" ),
							root.get( "address2" ),
							root.get( "name" )
					);

					query.select( criteriaBuilder.construct(
							Person.class,
							root.get( "address1" ),
							root.get( "address2" ),
							root.get( "name" )
					) );

					entityManager.createQuery( query ).getResultList();
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "address1_id", nullable = false)
		private Address address1;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "address2_id", nullable = false)
		private Address address2;

		public Person() {
		}

		public Person(Integer id, String name, Address address1, Address address2) {
			this.id = id;
			this.name = name;
			this.address1 = address1;
			this.address2 = address2;
		}

		public Person(Address address1, Address address2, String name) {
			this.address1 = address1;
			this.address2 = address2;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Address getAddress1() {
			return address1;
		}

		public Address getAddress2() {
			return address2;
		}
	}

	@Entity(name = "Address")
	@Table(name = "ADRESS_TABLE")
	public static class Address {
		@Id
		private Integer id;

		private String descriptiom;

		public Address() {
		}

		public Address(Integer id, String descriptiom) {
			this.id = id;
			this.descriptiom = descriptiom;
		}

		public Integer getId() {
			return id;
		}

		public String getDescriptiom() {
			return descriptiom;
		}
	}
}
