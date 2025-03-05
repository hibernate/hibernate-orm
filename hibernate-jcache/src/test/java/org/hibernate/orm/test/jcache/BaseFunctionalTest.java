/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
