package org.hibernate.resource.beans.container.spi;

import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/**
 * This contract and the nested LifecycleListener contract represent the changes
 * we'd like to propose to the CDI spec.  The idea being simply to allow contextual
 * registration of {@link BeanManager} lifecycle callbacks
 * <p>
 * CDI integrations implement and supply this contract using
 * {@link org.hibernate.cfg.ManagedBeanSettings#JAKARTA_CDI_BEAN_MANAGER}.
 * Hibernate registers a listener, which the integration invokes as the
 * underlying bean manager becomes available or is about to be destroyed.
 *
 * @author Steve Ebersole
 */
@SPI({ IMPLEMENT, SUPPLY })
public interface ExtendedBeanManager {
	/**
	 * Register a BeanManager LifecycleListener
	 *
	 * @param lifecycleListener The listener to register
	 */
	void registerLifecycleListener(LifecycleListener lifecycleListener);

	/**
	 * Contract for things interested in receiving notifications of
	 * BeanManager lifecycle events.
	 * <p>
	 * A "beanManagerDestroyed" notifications would probably also be generally
	 * useful, although we do not need it here and not sure WildFly can really
	 * tell us that reliably.
	 */
	@SPI(USE)
	interface LifecycleListener {
		void beanManagerInitialized(BeanManager beanManager);
		void beforeBeanManagerDestroyed(BeanManager beanManager);
	}
}
