/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = CriteriaStatTest.Employee.class)
@SessionFactory
public class CriteriaStatTest {

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
			statistics.setStatisticsEnabled( true );

			assertThat( statistics.getQueryExecutionCount() ).isZero();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Employee> criteriaQuery = criteriaBuilder.createQuery( Employee.class );
			criteriaQuery.from( Employee.class );
			Query<Employee> query = session.createQuery( criteriaQuery );

			List<Employee> employees = query.getResultList();
			assertThat( employees ).isEmpty();

			assertThat( statistics.getQueryExecutionCount() ).isOne();
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
