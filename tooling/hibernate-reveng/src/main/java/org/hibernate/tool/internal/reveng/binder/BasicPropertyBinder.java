package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class BasicPropertyBinder {
	
	public static BasicPropertyBinder create(BinderContext binderContext) {
		return new BasicPropertyBinder(binderContext);
	}
	
	private final MetadataBuildingContext metadataBuildingContext;	
	private final InFlightMetadataCollector metadataCollector;	
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	
	private BasicPropertyBinder(BinderContext binderContext) {
		this.metadataBuildingContext = binderContext.metadataBuildingContext;
		this.metadataCollector = binderContext.metadataCollector;
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
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
