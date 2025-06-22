/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		properties = { @Setting(name = AvailableSettings.QUERY_STATISTICS_MAX_SIZE, value = "100") }
)
public class ExplicitQueryStatsMaxSizeTest extends QueryStatsMaxSizeTest {


	@Override
	protected int expectedQueryStatisticsMaxSize() {
		return 100;
	}

	@Test
	public void testMaxSize(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
			SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );

			assertEquals(
					expectedQueryStatisticsMaxSize(),
					sessionFactory.getSessionFactoryOptions().getQueryStatisticsMaxSize()
			);

			StatisticsImplementor statistics = (StatisticsImplementor) sessionFactory.getStatistics();

			for ( int i = 0; i < 10; i++ ) {
				statistics.queryExecuted( String.valueOf( i ), 100, i * 1000 );
			}

			assertEquals( 1000, statistics.getQueryStatistics( "1" ).getExecutionTotalTime() );

			for ( int i = 100; i < 300; i++ ) {
				statistics.queryExecuted( String.valueOf( i ), 100, i * 1000 );
			}

			assertEquals( 0, statistics.getQueryStatistics( "1" ).getExecutionTotalTime() );
		} );
	}
}
