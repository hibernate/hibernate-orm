/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stat.internal;

import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.internal.StatisticsImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel
@ServiceRegistry(settings = {
		@Setting( name = Environment.CACHE_REGION_PREFIX, value = ConcurrentStatisticsTest.REGION_PREFIX),
		@Setting( name = Environment.USE_SECOND_LEVEL_CACHE, value = "false")
})
@SessionFactory
public class ConcurrentStatisticsTest {
	static final String REGION_PREFIX = "my-app";
	static final String TRIVIAL_REGION_NAME = "noname";

	@Test
	public void testThatGetSecondLevelCacheStatisticsWhenSecondLevelCacheIsNotEnabledReturnsNull(SessionFactoryScope scope) {
		final CacheRegionStatistics secondLevelCacheStatistics = new StatisticsImpl( scope.getSessionFactory() )
				.getCacheRegionStatistics( StringHelper.qualify( REGION_PREFIX, TRIVIAL_REGION_NAME ) );
		assertThat( secondLevelCacheStatistics, is( nullValue() ) );
	}
}
