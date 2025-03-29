/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

import org.hibernate.service.JavaServiceLoadable;

/**
 * Discoverable contributor for named references which are resolvable via StrategySelector.
 *
 * @see StrategySelector
 * @see NamedStrategyContributions
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface NamedStrategyContributor {
	/**
	 * Allows registration of named strategy implementations.
	 * Called on bootstrap.
	 */
	void contributeStrategyImplementations(NamedStrategyContributions contributions);

	/**
	 * Allows cleanup of (presumably previously {@linkplain #contributeStrategyImplementations registered}) strategy implementations.
	 * Called on shutdown.
	 */
	void clearStrategyImplementations(NamedStrategyContributions contributions);
}
