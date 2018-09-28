package org.hibernate.tool.api.export;

import org.hibernate.tool.internal.util.ReflectHelper;

public class ExporterFactory {
	
	public static Exporter createExporter(String exporterClassName) {
		Exporter result = null;
		try {
			Class<?> exporterClass = ReflectHelper.classForName(exporterClassName);
			result = (Exporter)exporterClass.newInstance();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException exception) {
			throw new RuntimeException("An exporter of class '" + exporterClassName + "' could not be created", exception);
		}
		return result;
	}
	
	public static Exporter createExporter(ExporterType exporterType) {
		return createExporter(exporterType.className());
	}

}
