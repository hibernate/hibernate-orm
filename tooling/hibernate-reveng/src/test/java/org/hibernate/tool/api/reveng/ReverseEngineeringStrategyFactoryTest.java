/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2018-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.reveng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
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
