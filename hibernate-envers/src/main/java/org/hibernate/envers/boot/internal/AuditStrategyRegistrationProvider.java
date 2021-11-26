/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.DefaultAuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;

/**
 * Provides the audit strategy implementations to the {@link StrategySelector} service.
 *
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditStrategyRegistrationProvider implements StrategyRegistrationProvider {

	private static final List<StrategyRegistration> STRATEGIES = new ArrayList<>();

	static {
		STRATEGIES.add(
				new SimpleStrategyRegistrationImpl(
						AuditStrategy.class,
						DefaultAuditStrategy.class,
						"default",
						DefaultAuditStrategy.class.getName(),
						DefaultAuditStrategy.class.getSimpleName(),
						org.hibernate.envers.strategy.internal.DefaultAuditStrategy.class.getName()
				)
		);
		STRATEGIES.add(
				new SimpleStrategyRegistrationImpl(
						AuditStrategy.class,
						ValidityAuditStrategy.class,
						"validity",
						ValidityAuditStrategy.class.getName(),
						ValidityAuditStrategy.class.getSimpleName(),
						org.hibernate.envers.strategy.internal.ValidityAuditStrategy.class.getName()
				)
		);
	}

	@Override
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return STRATEGIES;
	}
}
