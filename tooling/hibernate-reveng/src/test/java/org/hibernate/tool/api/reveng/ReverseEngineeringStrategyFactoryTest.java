/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2018-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.reveng;

import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.Assert;
import org.junit.Test;


public class ReverseEngineeringStrategyFactoryTest {
	
	@Test
	public void testCreateReverseEngineeringStrategy() {
		RevengStrategy reverseEngineeringStrategy = 
				RevengStrategyFactory.createReverseEngineeringStrategy();
		Assert.assertNotNull(reverseEngineeringStrategy);
		Assert.assertEquals(
				DefaultStrategy.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy = 
				RevengStrategyFactory.createReverseEngineeringStrategy(
						TestReverseEngineeringStrategyFactory.class.getName());
		Assert.assertNotNull(reverseEngineeringStrategy);
		Assert.assertEquals(
				TestReverseEngineeringStrategyFactory.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy = 
				RevengStrategyFactory.createReverseEngineeringStrategy(null);
		Assert.assertEquals(
				DefaultStrategy.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());		
	}
	
	public static class TestReverseEngineeringStrategyFactory extends DefaultStrategy {}

}
