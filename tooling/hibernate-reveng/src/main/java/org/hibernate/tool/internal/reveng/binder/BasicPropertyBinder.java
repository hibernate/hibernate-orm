package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

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
	

	Property bind(
			String propertyName, 
			Table table, 
			Column column, 
			Mapping mapping) {
		SimpleValue value = simpleValueBinder.bind(
				table, 
				column, 
				false);
		return propertyBinder.bind(
				table, 
				propertyName, 
				value, 
				true, 
				true, 
				false, 
				null, 
				null);
	}

}
