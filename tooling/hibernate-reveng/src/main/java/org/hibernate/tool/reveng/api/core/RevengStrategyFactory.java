/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.OverrideRepository;
import org.hibernate.tool.reveng.internal.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class RevengStrategyFactory {

	private static final String DEFAULT_REVERSE_ENGINEERING_STRATEGY_CLASS_NAME =
			DefaultStrategy.class.getName();

	public static RevengStrategy createReverseEngineeringStrategy(
			String reverseEngineeringClassName) {
		RevengStrategy result = null;
		try {
			Class<?> reverseEngineeringClass =
					ReflectionUtil.classForName(
							reverseEngineeringClassName == null ?
									DEFAULT_REVERSE_ENGINEERING_STRATEGY_CLASS_NAME :
									reverseEngineeringClassName);
			Constructor<?> reverseEngineeringConstructor = reverseEngineeringClass.getConstructor(new Class[] {});
			result = (RevengStrategy)reverseEngineeringConstructor.newInstance();
		}
		catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException exception) {
			throw new RuntimeException("An exporter of class '" + reverseEngineeringClassName + "' could not be created", exception);
		}
		return result;
	}

	public static RevengStrategy createReverseEngineeringStrategy(
			String reverseEngineeringClassName,
			File[] revengFiles) {
		RevengStrategy result =
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

	public static RevengStrategy createReverseEngineeringStrategy() {
		return createReverseEngineeringStrategy(null);
	}

}
