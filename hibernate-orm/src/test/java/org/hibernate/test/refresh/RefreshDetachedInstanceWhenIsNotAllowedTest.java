/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.refresh;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.test.refresh.TestEntity;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class RefreshDetachedInstanceWhenIsNotAllowedTest extends BaseCoreFunctionalTestCase {
	private TestEntity testEntity;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY, "false" );
	}

	@Before
	public void setUp() {
		testEntity = new TestEntity();
		doInHibernate( this::sessionFactory, session -> {
			session.save( testEntity );
		} );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRefreshDetachedInstance() {
		final Session session = openSession();
		session.refresh( testEntity );
	}
}
