/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class ReverseEngineeringStrategyFactoryTest {

	@Test
	public void testCreateReverseEngineeringStrategy() {
		RevengStrategy reverseEngineeringStrategy =
				RevengStrategyFactory.createReverseEngineeringStrategy();
		assertNotNull(reverseEngineeringStrategy);
		assertEquals(
				DefaultStrategy.class.getName(),
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy =
				RevengStrategyFactory.createReverseEngineeringStrategy(
						TestReverseEngineeringStrategyFactory.class.getName());
		assertNotNull(reverseEngineeringStrategy);
		assertEquals(
				TestReverseEngineeringStrategyFactory.class.getName(),
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy =
				RevengStrategyFactory.createReverseEngineeringStrategy(null);
		assertEquals(
				DefaultStrategy.class.getName(),
				reverseEngineeringStrategy.getClass().getName());
	}

	public static class TestReverseEngineeringStrategyFactory extends DefaultStrategy {}

}
