/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.registry.strategy;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BootstrapServiceRegistry
public class NamedStrategyContributorBaselineTests {
	@Test
	void testWithNoContributions(ServiceRegistryScope registryScope) {
		NamedStrategyContributorTests.check( registryScope, false );
	}
}
