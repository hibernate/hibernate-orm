package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

class BasicValueBinder extends AbstractBinder {
	
	static BasicValueBinder create(BinderContext binderContext) {
		return new BasicValueBinder(binderContext);
	}
	
	private BasicValueBinder(BinderContext binderContext) {
		super(binderContext);
	}
	
	SimpleValue bind(
			Table table,
			Column column,
			boolean generatedIdentifier) {
		SimpleValue value = new BasicValue(getMetadataBuildingContext(), table);
		value.addColumn(column);
		value.setTypeName(TypeUtils.determinePreferredType(
				getMetadataCollector(), 
				getRevengStrategy(),
				table, 
				column, 
				generatedIdentifier));
		if (generatedIdentifier) {
			value.setNullValue("undefined");
		}
		return value;
	}

}
