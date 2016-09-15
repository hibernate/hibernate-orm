/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.cache;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
public class DefaultCacheConcurrencyPropertyTest {

	@Test
	@TestForIssue( jiraKey = "HHH-9763" )
	public void testExplicitDefault() {

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-only" )
				.build();
		try {
			assertEquals(
					"read-only",
					ssr.getService( ConfigurationService.class ).getSettings().get(
							AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY
					)
			);
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TheEntity.class )
					.buildMetadata();
			assertEquals(
					AccessType.READ_ONLY,
					metadata.getMetadataBuildingOptions().getMappingDefaults().getImplicitCacheAccessType()
			);
			final SessionFactoryImplementor sf = (SessionFactoryImplementor) metadata.buildSessionFactory();
			try {
				final EntityPersister persister = sf.getEntityPersister( TheEntity.class.getName() );
				assertNotNull( persister.getCacheAccessStrategy() );
			}
			finally {
				sf.close();
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
