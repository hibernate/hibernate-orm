/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class NamingStrategySelectorTests {
	@Test
	void testShortNames(ServiceRegistryScope scope) {
		scope.withService( StrategySelector.class, (service) -> {
			final ImplicitDatabaseObjectNamingStrategy single = service.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, SingleNamingStrategy.STRATEGY_NAME );
			assertThat( single ).isInstanceOf( SingleNamingStrategy.class );

			final ImplicitDatabaseObjectNamingStrategy legacy = service.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, LegacyNamingStrategy.STRATEGY_NAME );
			assertThat( legacy ).isInstanceOf( LegacyNamingStrategy.class );

			final ImplicitDatabaseObjectNamingStrategy standard = service.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, StandardNamingStrategy.STRATEGY_NAME );
			assertThat( standard ).isInstanceOf( StandardNamingStrategy.class );
		} );
	}

	@Test
	void testMissing(ServiceRegistryScope scope) {
		scope.withService( StrategySelector.class, (service) -> {
			final ImplicitDatabaseObjectNamingStrategy single = service.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, null );
			assertThat( single ).isNull();
		} );
	}
}
