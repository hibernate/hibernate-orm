package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SharedCacheMode;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies the cache selection and inheritance rules described in the User Guide.
///
/// @author Steve Ebersole
@BaseUnitTest
class CacheMappingGuideTests {
	@Test
	void explicitHibernateMappingOverridesStandardSelection() {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( CacheSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class )
				.applySetting( CacheSettings.JAKARTA_SHARED_CACHE_MODE, SharedCacheMode.NONE )
				.build() ) {
			var metadata = new MetadataSources( registry )
					.addAnnotatedClasses( Root.class, StandardOnly.class ).buildMetadata();
			assertThat( metadata.getEntityBinding( Root.class.getName() ).isCached() ).isTrue();
			assertThat( metadata.getEntityBinding( StandardOnly.class.getName() ).isCached() ).isFalse();
		}
	}

	@Test
	void subclassCannotDeclareHibernateCacheSemantics() {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			assertThatThrownBy( () -> new MetadataSources( registry )
					.addAnnotatedClasses( Root.class, IllegalSubclass.class ).buildMetadata() )
					.isInstanceOf( AnnotationException.class )
					.hasMessageContaining( "only root classes may define second-level caching semantics" );
		}
	}

	@Entity(name = "CacheGuideRoot")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Root {
		@Id
		Long id;
	}

	@Entity(name = "CacheGuideIllegalSubclass")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	static class IllegalSubclass extends Root {
		String detail;
	}

	@Entity(name = "CacheGuideStandardOnly")
	@Cacheable
	static class StandardOnly {
		@Id
		Long id;
	}
}
