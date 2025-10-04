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
import org.junit.jupiter.api.Test;

import static org.hibernate.cache.jcache.ConfigSettings.CONFIG_URI;
import static org.hibernate.cfg.CacheSettings.CACHE_REGION_FACTORY;

@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name=CACHE_REGION_FACTORY, value = "jcache"),
		@Setting(name= CONFIG_URI, value = "/hibernate-config/ehcache/jcache-ehcache-config.xml")
})
@DomainModel(annotatedClasses = Product.class)
@SessionFactory
public class JCacheConfigRelativePathTest {
	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Product product = new Product();
			product.setName( "Acme" );
			product.setPriceCents( 100L );

			session.persist( product );
		} );
	}

}
