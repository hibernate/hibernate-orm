/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Gail Badner
 */
@BaseUnitTest
public class DefaultCacheConcurrencyPropertyTest {

	@Test
	@JiraKey(value = "HHH-9763")
	public void testExplicitDefault() {

		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-only" )
				.build();
		try {
			assertThat( ssr.getService( ConfigurationService.class ).getSettings()
					.get( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY ) )
					.isEqualTo( "read-only" );
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata();
			assertThat( metadata.getMetadataBuildingOptions().getMappingDefaults().getImplicitCacheAccessType() )
					.isEqualTo( AccessType.READ_ONLY );

			try (SessionFactoryImplementor sf = metadata.buildSessionFactory()) {
				final EntityPersister persister = sf.getRuntimeMetamodels()
						.getMappingMetamodel()
						.getEntityDescriptor( TheEntity.class.getName() );
				assertThat( persister.canReadFromCache() ).isTrue();
				assertThat( persister.canWriteToCache() ).isTrue();
				assertThat( persister.getCacheAccessStrategy() ).isNotNull();
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TheEntity")
	@Table(name = "THE_ENTITY")
	@Cacheable
	@Immutable
	public static class TheEntity {
		@Id
		public Long id;
	}
}
