/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import org.hibernate.orm.test.jcache.domain.Product;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.hibernate.cache.jcache.ConfigSettings.CONFIG_URI;
import static org.hibernate.cfg.CacheSettings.CACHE_REGION_FACTORY;

@ServiceRegistry(
		settings = @Setting(name = CACHE_REGION_FACTORY, value = "jcache"),
		settingProviders = @SettingProvider( settingName = CONFIG_URI, provider = JCacheConfigUrlTest.ConfigUrlProvider.class)
)
@DomainModel(annotatedClasses = Product.class)
@SessionFactory
public class JCacheConfigUrlTest {

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Product product = new Product();
			product.setName( "Acme" );
			product.setPriceCents( 100L );

			session.persist( product );
		} );
	}

	public static class ConfigUrlProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			final URL configUrl = Thread.currentThread()
					.getContextClassLoader()
					.getResource( "hibernate-config/ehcache/jcache-ehcache-config.xml" );
			assert configUrl != null;
			return "file://" + configUrl.getPath();
		}
	}
}
