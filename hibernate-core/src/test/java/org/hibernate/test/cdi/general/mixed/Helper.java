/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.mixed;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static SeContainer createSeContainer() {
		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( HostedBean.class, InjectedHostedBean.class );
		return cdiInitializer.initialize();
	}

	/**
	 * NOTE : we use the deprecated one here to make sure this continues to work.
	 * Scott still uses this in WildFly and we need it to continue to work there
	 */
	public static TestingExtendedBeanManager createExtendedBeanManager() {
		return new TestingExtendedBeanManager() {
			private ExtendedBeanManager.LifecycleListener lifecycleListener;

			@Override
			public void registerLifecycleListener(LifecycleListener lifecycleListener) {
				if ( this.lifecycleListener != null ) {
					throw new RuntimeException( "LifecycleListener already registered" );
				}
				this.lifecycleListener = lifecycleListener;
			}

			@Override
			public void notifyListenerReady(BeanManager beanManager) {
				lifecycleListener.beanManagerInitialized( beanManager );
			}

			@Override
			public void notifyListenerShuttingDown(BeanManager beanManager) {
				lifecycleListener.beforeBeanManagerDestroyed( beanManager );
			}
		};
	}

	public interface TestingExtendedBeanManager extends ExtendedBeanManager {
		void notifyListenerReady(BeanManager beanManager);
		void notifyListenerShuttingDown(BeanManager beanManager);
	}
}
