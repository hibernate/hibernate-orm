/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.size;

import java.util.ArrayList;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH014245")
@Jpa(annotatedClasses = {
		CriteriaSelectSizeCollectionTest.Customer.class,
		CriteriaSelectSizeCollectionTest.Alias.class
})
public class CriteriaSelectSizeCollectionTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Customer customer = new Customer( "1", "Phil" );
					Alias alias = new Alias( "2", "p" );
					customer.addAlias( alias );
					entityManager.persist( customer );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectCollectionSize(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					Root<Customer> customer = query.from( Customer.class );

					Expression<Integer> aliases = criteriaBuilder.size( customer.get( "aliases" ) );
					query.select( aliases );
					query.where( criteriaBuilder.equal( customer.get( "id" ), "1" ) );

					TypedQuery<Integer> tq = entityManager.createQuery( query );
					Integer size = tq.getSingleResult();
					assertThat( size, is( 1 ) );
				}
		);
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {

		@Id
		private String id;

		private String name;

		@ManyToMany(cascade = CascadeType.ALL)
		private Collection<Alias> aliases = new ArrayList<>();

		public Customer() {
		}

		public Customer(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void addAlias(Alias alias) {
			aliases.add( alias );
		}
	}

	@Entity(name = "Alias")
	@Table(name = "ALIAS_TABLE")
	public static class Alias implements java.io.Serializable {

		@Id
		private String id;

		private String alias;

		public Alias() {
		}

		public Alias(String id, String alias) {
			this.id = id;
			this.alias = alias;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}
	}

}
