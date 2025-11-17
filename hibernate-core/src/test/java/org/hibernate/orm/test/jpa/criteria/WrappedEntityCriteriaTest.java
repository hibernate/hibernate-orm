/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * @author Jan Schatteman
 */
@Jpa(
	annotatedClasses = { WrappedEntityCriteriaTest.SimpleEntity.class, WrappedEntityCriteriaTest.SimpleEntityWrapper.class },
	useCollectingStatementInspector = true
)
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-8891")
public class WrappedEntityCriteriaTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					SimpleEntity instance = new SimpleEntity("John");
					entityManager.persist( instance );
					instance = new SimpleEntity("Jack");
					entityManager.persist( instance );
					instance = new SimpleEntity("James");
					entityManager.persist( instance );
					instance = new SimpleEntity("Bill");
					entityManager.persist( instance );
					instance = new SimpleEntity("Harry");
					entityManager.persist( instance );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<SimpleEntityWrapper> query = criteriaBuilder.createQuery( SimpleEntityWrapper.class);
					Root<SimpleEntity> from = query.from(SimpleEntity.class);
					query.select(criteriaBuilder.construct( SimpleEntityWrapper.class, from));
					// the following should only execute 1 queries (as oposed to 1+n)
					sqlInspector.clear();
					entityManager.createQuery( query).getResultList();
					sqlInspector.assertExecutedCount( 1 );
					sqlInspector.assertIsSelect( 0 );
				}
		);
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		protected SimpleEntity() {
		}
		public SimpleEntity(String name) {
			this.name = name;
		}
		public Integer getId() {
			return id;
		}
		protected void setId(Integer id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	public static class SimpleEntityWrapper {
		private final SimpleEntity instance;
		public SimpleEntityWrapper(SimpleEntity instance) {
			this.instance = instance;
		}
		public SimpleEntity getBrand() {
			return instance;
		}
	}

}
