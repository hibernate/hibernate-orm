package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class BasicPropertyBinder {
	
	private final MetadataBuildingContext metadataBuildingContext;	
	private final InFlightMetadataCollectorImpl metadataCollector;	
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	
	public static BasicPropertyBinder create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollectorImpl metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema) {
		return new BasicPropertyBinder(			
				metadataBuildingContext,
				metadataCollector,
				revengStrategy,
				defaultCatalog,
				defaultSchema);
	}
	
	private BasicPropertyBinder(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollectorImpl metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
	}
	

	public Property bind(
			String propertyName, 
			Table table, 
			Column column, 
			Mapping mapping) {
		SimpleValue value = SimpleValueBinder.bind(
				metadataBuildingContext, 
				metadataCollector, 
				revengStrategy, 
				table, 
				column, 
				mapping,
				false);
		return PropertyBinder.bind(
				table, 
				defaultCatalog,
				defaultSchema,
				propertyName, 
				value, 
				true, 
				true, 
				false, 
				null, 
				null,
				revengStrategy);
	}

}
