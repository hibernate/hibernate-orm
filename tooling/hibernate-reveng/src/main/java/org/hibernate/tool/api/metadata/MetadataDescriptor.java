package org.hibernate.tool.api.metadata;

import java.util.Properties;

import org.hibernate.boot.Metadata;

public interface MetadataDescriptor {
	
	public String PREFER_BASIC_COMPOSITE_IDS = "org.hibernate.tool.api.metadata.MetadataDescriptor.PREFER_BASIC_COMPOSITE_IDS";

	Metadata createMetadata();
	
	Properties getProperties();

}
