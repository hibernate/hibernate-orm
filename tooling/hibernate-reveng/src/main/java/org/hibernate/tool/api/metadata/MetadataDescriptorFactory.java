package org.hibernate.tool.api.metadata;

import java.io.File;
import java.util.Properties;

import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.internal.metadata.JdbcMetadataDescriptor;
import org.hibernate.tool.internal.metadata.JpaMetadataDescriptor;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;

public class MetadataDescriptorFactory {
	
	public static MetadataDescriptor createJdbcDescriptor(
			ReverseEngineeringStrategy reverseEngineeringStrategy, 
			Properties properties,
			boolean preferBasicCompositeIds) {
		return new JdbcMetadataDescriptor(
				reverseEngineeringStrategy, 
				properties,
				preferBasicCompositeIds);
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
