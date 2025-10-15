/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.export;

import org.hibernate.tool.reveng.internal.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ExporterFactory {

	public static Exporter createExporter(String exporterClassName) {
		Exporter result = null;
		try {
			Class<?> exporterClass = ReflectionUtil.classForName(exporterClassName);
			Constructor<?> exporterConstructor = exporterClass.getConstructor(new Class[] {});
			result = (Exporter)exporterConstructor.newInstance();
		}
		catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException exception) {
			throw new RuntimeException("An exporter of class '" + exporterClassName + "' could not be created", exception);
		}
		return result;
	}

	public static Exporter createExporter(ExporterType exporterType) {
		return createExporter(exporterType.className());
	}

}
