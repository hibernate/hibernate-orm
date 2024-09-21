/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import jakarta.enterprise.inject.spi.BeanManager;

public interface TestingExtendedBeanManager {

	void notifyListenerReady(BeanManager beanManager);

	void notifyListenerShuttingDown(BeanManager beanManager);

	static TestingExtendedBeanManager create() {
		return new TestingExtendedBeanManagerImpl();
	}

}
