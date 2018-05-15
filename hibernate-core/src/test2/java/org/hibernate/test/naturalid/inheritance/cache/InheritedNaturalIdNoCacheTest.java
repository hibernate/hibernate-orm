/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance.cache;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InheritedNaturalIdNoCacheTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class, ExtendedEntity.class};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new MyEntity( "base" ) );
			session.persist( new ExtendedEntity( "extended", "ext" ) );
		});
	}

	@Test
	public void testLoadExtendedByNormal() {
		doInHibernate( this::sessionFactory, session -> {
			MyEntity user = session.byNaturalId( MyEntity.class ).using(
				"uid",
				"base"
			).load();
			ExtendedEntity extendedMyEntity = session.byNaturalId( ExtendedEntity.class )
					.using( "uid", "extended" )
					.load();
			assertNotNull( user );
			assertNotNull( extendedMyEntity );
		});

		doInHibernate( this::sessionFactory, session -> {
			ExtendedEntity user = session.byNaturalId( ExtendedEntity.class )
				.using( "uid", "base" )
				.load();
			assertNull( user );
		});
	}
}
