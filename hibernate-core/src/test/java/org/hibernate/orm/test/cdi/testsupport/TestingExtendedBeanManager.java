/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

public interface TestingExtendedBeanManager extends ExtendedBeanManager {

	void notifyListenerReady(BeanManager beanManager);

	void notifyListenerShuttingDown(BeanManager beanManager);

	boolean isReadyForUse();

	static TestingExtendedBeanManager create() {
		return new TestingExtendedBeanManagerImpl();
	}

}
