package org.hibernate.tool.api.reveng;

import org.junit.Assert;
import org.junit.Test;


public class ReverseEngineeringStrategyFactoryTest {
	
	@Test
	public void testCreateReverseEngineeringStrategy() {
		RevengStrategy reverseEngineeringStrategy = 
				ReverseEngineeringStrategyFactory.createReverseEngineeringStrategy();
		Assert.assertNotNull(reverseEngineeringStrategy);
		Assert.assertEquals(
				DefaultRevengStrategy.class.getName(), 
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
				DefaultRevengStrategy.class.getName(), 
				reverseEngineeringStrategy.getClass().getName());		
	}
	
	public static class TestReverseEngineeringStrategyFactory extends DefaultRevengStrategy {}

}
