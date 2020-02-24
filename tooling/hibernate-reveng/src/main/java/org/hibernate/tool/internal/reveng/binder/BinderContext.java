package org.hibernate.tool.internal.reveng.binder;

import java.util.Properties;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tool.api.reveng.RevengStrategy;

public class BinderContext {
	
	public static BinderContext create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		return new BinderContext(
				metadataBuildingContext, 
				metadataCollector, 
				revengStrategy, 
				properties);
	}
	
	public final MetadataBuildingContext metadataBuildingContext;
	public final InFlightMetadataCollector metadataCollector;
	public final RevengStrategy revengStrategy;
	public final Properties properties;
	
	private BinderContext(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			RevengStrategy revengStrategy,
			Properties properties) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.properties = properties;
	}

}
