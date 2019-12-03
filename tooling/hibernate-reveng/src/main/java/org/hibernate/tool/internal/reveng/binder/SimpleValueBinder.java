package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

public class SimpleValueBinder {
	
	public static SimpleValueBinder create(BinderContext binderContext) {
		return new SimpleValueBinder(binderContext);
	}
	
	private final BinderContext binderContext;
	
	public SimpleValueBinder(BinderContext binderContext) {
		this.binderContext = binderContext;
	}
	
	public SimpleValue bind(
			Table table,
			Column column,
			Mapping mapping,
			boolean generatedIdentifier) {
		SimpleValue value = new SimpleValue(binderContext.metadataBuildingContext, table);
		value.addColumn(column);
		value.setTypeName(TypeUtils.determinePreferredType(
				binderContext.metadataCollector, 
				binderContext.revengStrategy,
				table, 
				column, 
				mapping, 
				generatedIdentifier));
		return value;
	}

}
