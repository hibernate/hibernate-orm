/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stats;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting( name =  AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting( name =  AvailableSettings.USE_QUERY_CACHE, value = "false")
		}
)
public class StatisticsWithNoQueryCachingTest  {

	@Test
	@JiraKey( value = "HHH-13645")
	public void testUncachedRegion(SessionFactoryScope scope) {
		final Statistics statistics = scope.getSessionFactory().getStatistics();
		assertNull( statistics.getCacheRegionStatistics( "hibernate.test.unknown" ) );
	}
}
