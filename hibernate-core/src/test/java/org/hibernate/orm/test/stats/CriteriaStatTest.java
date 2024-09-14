/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.stats;

import java.util.List;

import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class CriteriaStatTest extends BaseCoreFunctionalTestCase  {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Test
	public void test() {
		inTransaction( session -> {
			StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
			statistics.setStatisticsEnabled( true );

			assertThat( statistics.getQueryExecutionCount() ).isZero();

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Employee> criteriaQuery = criteriaBuilder.createQuery( Employee.class );
			criteriaQuery.from( Employee.class );
			QueryImplementor<Employee> query = session.createQuery( criteriaQuery );

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
