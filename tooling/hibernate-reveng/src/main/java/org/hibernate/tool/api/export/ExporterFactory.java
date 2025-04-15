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
package org.hibernate.tool.api.export;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.tool.util.ReflectionUtil;

public class ExporterFactory {
	
	public static Exporter createExporter(String exporterClassName) {
		Exporter result = null;
		try {
			Class<?> exporterClass = ReflectionUtil.classForName(exporterClassName);
			Constructor<?> exporterConstructor = exporterClass.getConstructor(new Class[] {}); 
			result = (Exporter)exporterConstructor.newInstance();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException exception) {
			throw new RuntimeException("An exporter of class '" + exporterClassName + "' could not be created", exception);
		}
		return result;
	}
	
	public static Exporter createExporter(ExporterType exporterType) {
		return createExporter(exporterType.className());
	}

}
