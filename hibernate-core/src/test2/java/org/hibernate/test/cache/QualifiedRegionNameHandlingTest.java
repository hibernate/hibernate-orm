/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

/**
 * @author Steve Ebersole
 */
public class QualifiedRegionNameHandlingTest extends BaseNonConfigCoreFunctionalTestCase {
	private static final String PREFIX = "app1";

	private static final String LOCAL_NAME = "a.b.c";

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		ssrb.applySetting( AvailableSettings.USE_QUERY_CACHE, "true" );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_PREFIX, PREFIX );
	}

	@Test
	public void testValidCall() {
		MatcherAssert.assertThat(
				sessionFactory().getCache().unqualifyRegionName( PREFIX + '.' + LOCAL_NAME ),
				CoreMatchers.is( LOCAL_NAME )
		);
	}

	@Test
	public void testUnqualifiedNameUsed() {
		try {
			sessionFactory().getCache().unqualifyRegionName( LOCAL_NAME );
		}
		catch (IllegalArgumentException expected) {
		}
	}
}
