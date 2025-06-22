/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = QueryStatsMaxSizeTest.Employee.class,
		generateStatistics = true
)

public class QueryStatsMaxSizeTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
			SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );

			assertEquals(
					expectedQueryStatisticsMaxSize(),
					sessionFactory.getSessionFactoryOptions().getQueryStatisticsMaxSize()
			);
		} );
	}

	protected int expectedQueryStatisticsMaxSize() {
		return Statistics.DEFAULT_QUERY_STATISTICS_MAX_SIZE;
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		private String password;
	}

}
