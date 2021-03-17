/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache.jcache.config;

import java.util.Map;

import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

public class JCacheConfigRelativePathTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.CACHE_REGION_FACTORY, "jcache" );
		settings.put( ConfigSettings.CONFIG_URI, "/hibernate-config/ehcache/jcache-ehcache-config.xml" );
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
