/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector;

import org.hibernate.service.JavaServiceLoadable;

/**
 * Responsible for providing the registrations of one or more strategy selectors.
 * <p>
 * A {@code StrategyRegistrationProvider} may be made available either by:
 * <ul>
 * <li>registering it directly with the
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistry} by calling
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder#applyStrategySelectors},
 *     or
 * <li>by making it discoverable via the Java {@link java.util.ServiceLoader} facility.
 * </ul>
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface StrategyRegistrationProvider {
	/**
	 * Get all {@link StrategyRegistration}s announced by this provider.
	 *
	 * @return All {@link StrategyRegistration}s
	 */
	Iterable<StrategyRegistration<?>> getStrategyRegistrations();
}
