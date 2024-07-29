package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.reveng.util.EnhancedBasicValue;

class BasicValueBinder extends AbstractBinder {
	
	static BasicValueBinder create(BinderContext binderContext) {
		return new BasicValueBinder(binderContext);
	}
	
	private BasicValueBinder(BinderContext binderContext) {
		super(binderContext);
	}
	
	EnhancedBasicValue bind(
			Table table,
			Column column,
			boolean generatedIdentifier) {
		EnhancedBasicValue value = new EnhancedBasicValue(getMetadataBuildingContext(), table);
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
