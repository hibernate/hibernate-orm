/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jcache.test;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.junit5.FunctionalSessionFactoryTesting;
import org.hibernate.testing.junit5.SessionFactoryProducer;
import org.hibernate.testing.junit5.SessionFactoryScope;
import org.hibernate.testing.junit5.SessionFactoryScopeContainer;

/**
 * @author Steve Ebersole
 */
@FunctionalSessionFactoryTesting
public abstract class BaseFunctionalTest implements SessionFactoryProducer, SessionFactoryScopeContainer {
	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.sessionFactoryScope = scope;
	}

	private SessionFactoryScope sessionFactoryScope;

	protected SessionFactoryScope sessionFactoryScope() {
		return sessionFactoryScope;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactoryScope.getSessionFactory();
	}

	@Override
	public SessionFactoryProducer getSessionFactoryProducer() {
		return this;
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory() {
		TestHelper.preBuildAllCaches();
		return TestHelper.buildStandardSessionFactory();
	}
}
