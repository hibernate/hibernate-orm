/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class CdiContainerScope implements AutoCloseable {
	private final Supplier<SeContainer> seContainerSupplier;

	private SeContainer seContainer;
	private TestingExtendedBeanManager extendedBeanManager;

	public CdiContainerScope(Supplier<SeContainer> seContainerSupplier) {
		this.seContainerSupplier = seContainerSupplier;
	}

	public boolean isContainerAvailable() {
		return seContainer != null;
	}

	public SeContainer getContainer() {
		if ( seContainer == null ) {
			seContainer = seContainerSupplier.get();
			if ( extendedBeanManager != null ) {
				extendedBeanManager.notifyListenerReady( seContainer.getBeanManager() );
			}
		}
		return seContainer;
	}

	public BeanManager getBeanManager() {
		return getContainer().getBeanManager();
	}

	public TestingExtendedBeanManager getExtendedBeanManager() {
		if ( extendedBeanManager == null ) {
			extendedBeanManager = new TestingExtendedBeanManagerImpl();
		}
		return extendedBeanManager;
	}

	public void triggerReadyForUse() {
		assert extendedBeanManager != null;
		extendedBeanManager.notifyListenerReady( getBeanManager() );
	}

	@Override
	public void close() throws Exception {
		if ( seContainer != null ) {
			if ( extendedBeanManager != null && extendedBeanManager.isReadyForUse() ) {
				extendedBeanManager.notifyListenerShuttingDown( seContainer.getBeanManager() );
			}
			if ( seContainer.isRunning() ) {
				seContainer.close();
			}
			seContainer = null;
		}
		extendedBeanManager = null;
	}
}
