/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jcache;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public abstract class BaseFunctionalTest {
	private SessionFactoryImplementor sessionFactory;

	@BeforeEach
	public void createSessionFactory() {
		assert sessionFactory == null || sessionFactory.isClosed();
		TestHelper.preBuildAllCaches();
		sessionFactory = TestHelper.buildStandardSessionFactory();
	}

	@AfterEach
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
