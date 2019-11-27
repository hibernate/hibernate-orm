package org.hibernate.tool.internal.reveng;

import java.util.Properties;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class ReverseEngineeringContext {
	
	public static ReverseEngineeringContext create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			Properties properties) {
		return new ReverseEngineeringContext(
				metadataBuildingContext, 
				metadataCollector, 
				revengStrategy, 
				properties);
	}
	
	public final MetadataBuildingContext metadataBuildingContext;
	public final InFlightMetadataCollector metadataCollector;
	public final ReverseEngineeringStrategy revengStrategy;
	public final Properties properties;
	
	private ReverseEngineeringContext(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			Properties properties) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.properties = properties;
	}

}
