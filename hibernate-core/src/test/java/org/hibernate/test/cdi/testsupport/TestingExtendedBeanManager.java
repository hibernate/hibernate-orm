/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.testsupport;

import javax.enterprise.inject.spi.BeanManager;

public interface TestingExtendedBeanManager {

	void notifyListenerReady(BeanManager beanManager);

	void notifyListenerShuttingDown(BeanManager beanManager);

	static TestingExtendedBeanManager create() {
		return new TestingExtendedBeanManagerImpl();
	}

	static TestingExtendedBeanManager createLegacy() {
		return new TestingLegacyExtendedBeanManagerImpl();
	}

}
