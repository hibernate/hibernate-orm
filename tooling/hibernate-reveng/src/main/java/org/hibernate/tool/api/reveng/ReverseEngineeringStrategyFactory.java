package org.hibernate.tool.api.reveng;

import org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.tool.internal.util.ReflectHelper;

public class ReverseEngineeringStrategyFactory {
	
	private static final String DEFAULT_REVERSE_ENGINEERING_STRATEGY_CLASS_NAME = 
			DefaultReverseEngineeringStrategy.class.getName();
	
	public static ReverseEngineeringStrategy createReverseEngineeringStrategy(
			String reverseEngineeringClassName) {
		ReverseEngineeringStrategy result = null;
		try {
			Class<?> reverseEngineeringClass = 
					ReflectHelper.classForName(
							reverseEngineeringClassName == null ? 
									DEFAULT_REVERSE_ENGINEERING_STRATEGY_CLASS_NAME : 
									reverseEngineeringClassName);
			result = (ReverseEngineeringStrategy)reverseEngineeringClass.newInstance();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException exception) {
			throw new RuntimeException("An exporter of class '" + reverseEngineeringClassName + "' could not be created", exception);
		}
		return result;
	}
	
	public static ReverseEngineeringStrategy createReverseEngineeringStrategy() {
		return createReverseEngineeringStrategy(null);
	}
	
}
