/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stats;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class QueryStatsMaxSizeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Employee.class,
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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
