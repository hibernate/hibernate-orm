package org.hibernate.tool.api.reveng;

import java.io.File;

import org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.tool.internal.reveng.OverrideRepository;
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
	
	public static ReverseEngineeringStrategy createReverseEngineeringStrategy(
			String reverseEngineeringClassName,
			File[] revengFiles) {
		ReverseEngineeringStrategy result = 
				createReverseEngineeringStrategy(reverseEngineeringClassName);
		if (revengFiles != null && revengFiles.length > 0) {
			OverrideRepository overrideRepository = new OverrideRepository();
			for (File file : revengFiles) {
				overrideRepository.addFile(file);
			}
			result = overrideRepository.getReverseEngineeringStrategy(result);
		}
		return result;
	}
	
	public static ReverseEngineeringStrategy createReverseEngineeringStrategy() {
		return createReverseEngineeringStrategy(null);
	}
	
}
