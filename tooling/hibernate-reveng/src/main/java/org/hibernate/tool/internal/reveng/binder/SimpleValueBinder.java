package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

public class SimpleValueBinder extends AbstractBinder {
	
	static SimpleValueBinder create(BinderContext binderContext) {
		return new SimpleValueBinder(binderContext);
	}
	
	private SimpleValueBinder(BinderContext binderContext) {
		super(binderContext);
	}
	
	SimpleValue bind(
			Table table,
			Column column,
			Mapping mapping,
			boolean generatedIdentifier) {
		SimpleValue value = new SimpleValue(getMetadataBuildingContext(), table);
		value.addColumn(column);
		value.setTypeName(TypeUtils.determinePreferredType(
				getMetadataCollector(), 
				getRevengStrategy(),
				table, 
				column, 
				mapping, 
				generatedIdentifier));
		return value;
	}

}
