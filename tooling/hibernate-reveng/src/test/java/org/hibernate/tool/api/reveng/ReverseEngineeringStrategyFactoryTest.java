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
