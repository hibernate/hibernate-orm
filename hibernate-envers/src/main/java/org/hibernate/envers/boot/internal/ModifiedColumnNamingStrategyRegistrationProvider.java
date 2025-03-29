/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy;

/**
 * A {@link StrategyRegistrationProvider} for {@link ModifiedColumnNamingStrategy}s.
 *
 * @author Chris Cranford
 */
public class ModifiedColumnNamingStrategyRegistrationProvider implements StrategyRegistrationProvider {
	@Override
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		final List<StrategyRegistration> registrations = new ArrayList<>();

		registrations.add(
				new SimpleStrategyRegistrationImpl(
						ModifiedColumnNamingStrategy.class,
						LegacyModifiedColumnNamingStrategy.class,
						"default", "legacy"
				)
		);

		registrations.add(
				new SimpleStrategyRegistrationImpl(
						ModifiedColumnNamingStrategy.class,
						ImprovedModifiedColumnNamingStrategy.class,
						"improved"
				)
		);

		return registrations;
	}
}
