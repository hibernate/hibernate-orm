package org.hibernate.tool.internal.reveng.binder;

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
	
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	private final SimpleValueBinder simpleValueBinder;
	
	private BasicPropertyBinder(BinderContext binderContext) {
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		this.simpleValueBinder = SimpleValueBinder.create(binderContext);
	}
	

	public Property bind(
			String propertyName, 
			Table table, 
			Column column, 
			Mapping mapping) {
		SimpleValue value = simpleValueBinder.bind(
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
