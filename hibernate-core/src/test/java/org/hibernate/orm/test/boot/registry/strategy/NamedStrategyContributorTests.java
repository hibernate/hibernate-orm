/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.registry.strategy;

import org.hibernate.boot.registry.selector.spi.NamedStrategyContributor;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@BootstrapServiceRegistry( javaServices = @BootstrapServiceRegistry.JavaService(
		role = NamedStrategyContributor.class,
		impl = NamedStrategyContributorImpl.class
) )
public class NamedStrategyContributorTests {
	@Test
	void testWithContributions(ServiceRegistryScope registryScope) {
		check( registryScope, true );
	}

//	@Test
//	@BootstrapServiceRegistry
//	@ServiceRegistry
//	void testWithNoContributions(ServiceRegistryScope registryScope) {
//		check( registryScope, false );
//	}

	public static void check(ServiceRegistryScope registryScope, boolean javaServiceApplied) {
		registryScope.withService( StrategySelector.class, (selector) -> {
			// null ref
			checkExistence( selector, null, false );
			// resolvable because we pass the FQN
			checkExistence( selector, StrategyContractImpl.class.getName(), true );

			checkExistence( selector, StrategyContractImpl.class.getSimpleName(), javaServiceApplied );
			checkExistence( selector, "main", javaServiceApplied );
			checkExistence( selector, "alternate", javaServiceApplied );
			checkExistence( selector, "non-existent", false );
		} );
	}

	private static void checkExistence(StrategySelector selector, Object ref, boolean expectToExist) {
		try {
			final StrategyContract  resolved = selector.resolveDefaultableStrategy( StrategyContract .class, ref, (StrategyContract ) null );
			if ( expectToExist ) {
				assertThat( resolved ).isNotNull();
			}
			else {
				// generally this is the ref == null case
				assertThat( resolved ).isNull();
			}
		}
		catch (StrategySelectionException e) {
			if ( expectToExist ) {
				throw e;
			}
			// otherwise, this is expected
		}
	}
}
