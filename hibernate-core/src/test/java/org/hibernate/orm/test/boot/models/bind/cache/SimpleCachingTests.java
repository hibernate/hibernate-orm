/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleCachingTests {
	@SuppressWarnings("JUnitMalformedDeclaration")
	@Test
	@ServiceRegistry
	void simpleTest(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var metadataCollector = context.getMetadataCollector();
					final RootClass entityBinding = (RootClass) metadataCollector.getEntityBinding( CacheableEntity.class.getName() );
					assertThat( entityBinding.isCached() ).isTrue();
					assertThat( entityBinding.getCacheRegionName() ).isEqualTo( "org.hibernate.testing.entity" );
					assertThat( entityBinding.getCacheConcurrencyStrategy() ).isEqualToIgnoringCase( "read-write" );
					assertThat( entityBinding.getNaturalIdCacheRegionName() ).isEqualTo( "org.hibernate.testing.natural-id" );

					final PersistentClass subEntityBinding = metadataCollector.getEntityBinding( CacheableEntitySub.class.getName() );
					assertThat( subEntityBinding.isCached() ).isFalse();
				},
				scope.getRegistry(),
				CacheableEntity.class,
				CacheableEntitySub.class
		);
	}

	@Entity(name="CacheableEntity")
	@Table(name="CacheableEntity")
	@Cacheable
	@Cache( region = "org.hibernate.testing.entity", usage = READ_WRITE)
	@NaturalIdCache( region = "org.hibernate.testing.natural-id" )
	@Inheritance(strategy = JOINED)
	public static class CacheableEntity {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="CacheableEntitySub")
	@Table(name="CacheableEntitySub")
	@Cacheable(false)
	public static class CacheableEntitySub extends CacheableEntity {
		private String someText;
	}
}
