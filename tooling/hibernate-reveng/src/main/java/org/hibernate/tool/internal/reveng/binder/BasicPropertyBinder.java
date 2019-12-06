package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.internal.reveng.DefaultAssociationInfo;

class BasicPropertyBinder extends AbstractBinder {
	
	static BasicPropertyBinder create(BinderContext binderContext) {
		return new BasicPropertyBinder(binderContext);
	}
	
	private final SimpleValueBinder simpleValueBinder;
	private final PropertyBinder propertyBinder;
	
	private BasicPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		simpleValueBinder = SimpleValueBinder.create(binderContext);
		propertyBinder = PropertyBinder.create(binderContext);
	}
	

	Property bind(String propertyName, Table table, Column column) {
		return propertyBinder.bind(
				table, 
				propertyName, 
				simpleValueBinder.bind(table, column, false), 
				DefaultAssociationInfo.create(null, null, true, true));
	}

}
