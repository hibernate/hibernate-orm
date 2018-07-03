/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;

/**
 * @author Steve Ebersole
 */
public abstract class BaseFunctionalTest extends BaseUnitTestCase {
	private SessionFactoryImplementor sessionFactory;

	@Before
	public void createSessionFactory() {
		assert sessionFactory == null || sessionFactory.isClosed();
		TestHelper.preBuildAllCaches();
		sessionFactory = TestHelper.buildStandardSessionFactory();
	}

	@After
	public void releaseSessionFactory() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	protected SessionFactoryImplementor sessionFactory() {
		if ( sessionFactory == null ) {
			throw new IllegalStateException( "SessionFactory is null" );
		}

		return sessionFactory;
	}
}
