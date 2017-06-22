/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.proxy.tuplizer;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernateSessionBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class TuplizerTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testEntityTuplizer() throws Exception {
		//tag::entity-tuplizer-dynamic-proxy-example[]
		Cuisine _cuisine = doInHibernateSessionBuilder(
				() -> sessionFactory()
						.withOptions()
						.interceptor( new EntityNameInterceptor() ),
				session -> {
			Cuisine cuisine = ProxyHelper.newProxy( Cuisine.class, null );
			cuisine.setName( "Française" );

			Country country = ProxyHelper.newProxy( Country.class, null );
			country.setName( "France" );

			cuisine.setCountry( country );
			session.persist( cuisine );

			return cuisine;
		} );

		doInHibernateSessionBuilder(
				() -> sessionFactory()
						.withOptions()
						.interceptor( new EntityNameInterceptor() ),
				session -> {
			Cuisine cuisine = session.get( Cuisine.class, _cuisine.getId() );

			assertEquals( "Française", cuisine.getName() );
			assertEquals( "France", cuisine.getCountry().getName() );
		} );
		//end::entity-tuplizer-dynamic-proxy-example[]
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Cuisine.class };
	}
}
