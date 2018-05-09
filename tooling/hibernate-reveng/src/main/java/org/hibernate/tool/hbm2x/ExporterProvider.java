package org.hibernate.tool.hbm2x;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.internal.util.ReflectHelper;

public class ExporterProvider {
	
	final String exporterClassName;
	final String exporterName;
	final Map<?,?> supportedProperties;
	
	public ExporterProvider(String exporterName, String exporterClassName, Map<?,?> supportedProperties) {
		this.exporterClassName = exporterClassName;
		this.exporterName = exporterName;
		this.supportedProperties = supportedProperties;
	}
	
	public String getExporterName() {
		return exporterName;
	}
	
	public Set<?> getSupportedProperties() {
		return supportedProperties.keySet();
	}
	
	public List<?> validateProperties(Properties properties) {
		return Collections.EMPTY_LIST;
	}
	
	public Exporter createProvider() {
		try {
			return (Exporter) ReflectHelper.classForName(exporterClassName, this.getClass()).newInstance();
		}
		catch (Exception e) {
			throw new ExporterException("Could not create exporter: " + exporterClassName, e);
		}
	}

}
