package org.hibernate.tool.api.metadata;

import java.io.File;
import java.util.Properties;

import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.metadata.JpaMetadataDescriptor;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;

public class MetadataDescriptorFactory {
	
	public static MetadataDescriptor createReverseEngineeringDescriptor(
			RevengStrategy reverseEngineeringStrategy, 
			Properties properties) {
		return new RevengMetadataDescriptor(
				reverseEngineeringStrategy, 
				properties);
	}
	
	public static MetadataDescriptor createJpaDescriptor(String persistenceUnit, Properties properties) {
		return new JpaMetadataDescriptor(persistenceUnit, properties);
	}
	
	public static MetadataDescriptor createNativeDescriptor(
			File cfgXmlFile,
			File[] mappingFiles,
			Properties properties) {
		return new NativeMetadataDescriptor(
				cfgXmlFile, 
				mappingFiles, 
				properties);
	}
	
}
