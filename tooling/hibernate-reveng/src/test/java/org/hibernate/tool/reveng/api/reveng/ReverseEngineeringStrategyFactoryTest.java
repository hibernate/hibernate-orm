/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.reveng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.junit.jupiter.api.Test;


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
