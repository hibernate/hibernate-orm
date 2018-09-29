package org.hibernate.tool.api.reveng;

import org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy;
import org.junit.Assert;
import org.junit.Test;


public class ReverseEngineeringStrategyFactoryTest {
	
	@Test
	public void testCreateReverseEngineeringStrategy() {
		ReverseEngineeringStrategy reverseEngineeringStrategy = 
				ReverseEngineeringStrategyFactory.createReverseEngineeringStrategy();
		Assert.assertNotNull(reverseEngineeringStrategy);
		Assert.assertEquals(
				DefaultReverseEngineeringStrategy.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy = 
				ReverseEngineeringStrategyFactory.createReverseEngineeringStrategy(
						TestReverseEngineeringStrategyFactory.class.getName());
		Assert.assertNotNull(reverseEngineeringStrategy);
		Assert.assertEquals(
				TestReverseEngineeringStrategyFactory.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());
		reverseEngineeringStrategy = 
				ReverseEngineeringStrategyFactory.createReverseEngineeringStrategy(null);
		Assert.assertEquals(
				DefaultReverseEngineeringStrategy.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());		
	}
	
	public static class TestReverseEngineeringStrategyFactory extends DefaultReverseEngineeringStrategy {}

}
