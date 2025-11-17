/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.cid;

import jakarta.persistence.SharedCacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class NonAggregatedCompositeIdCachingTest {

	@Test
	@JiraKey(value = "HHH-9913")
	public void testNonAggregatedCompositeId() {
		// HHH-9913 reports a NPE when bootstrapping a SF with non-aggregated composite identifiers
		// in org.hibernate.cache.internal.CacheDataDescriptionImpl#decode
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, true )
				.build();

		try (SessionFactory sf = new MetadataSources( ssr )
				.addAnnotatedClass( It.class )
				.getMetadataBuilder()
				.applySharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE )
				.build()
				.buildSessionFactory()) {
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-9913")
	public void testNonAggregatedCompositeIdWithPkClass() {
		// HHH-9913 reports a NPE when bootstrapping a SF with non-aggregated composite identifiers
		// in org.hibernate.cache.internal.CacheDataDescriptionImpl#decode
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try (SessionFactory sf = new MetadataSources( ssr )
				.addAnnotatedClass( ItWithPkClass.class )
				.getMetadataBuilder()
				.applySharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE )
				.build()
				.buildSessionFactory()) {
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
