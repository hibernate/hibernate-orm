/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import java.util.Map;

import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.orm.test.jcache.domain.Product;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

public class JCacheConfigUrlTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map<String,Object> settings) {
		settings.put( Environment.CACHE_REGION_FACTORY, "jcache" );
		settings.put(
				ConfigSettings.CONFIG_URI,
				"file://" + Thread.currentThread().getContextClassLoader().getResource( "hibernate-config/ehcache/jcache-ehcache-config.xml" ).getPath()
		);
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Product.class
		};
	}

	@Test
	public void test() {
		Product product = new Product();
		product.setName( "Acme" );
		product.setPriceCents( 100L );

		doInHibernate( this::sessionFactory, session -> {
			session.persist( product );
		} );
	}

}
