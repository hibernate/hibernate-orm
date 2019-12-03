package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

public class BasicPropertyBinder extends AbstractBinder {
	
	public static BasicPropertyBinder create(BinderContext binderContext) {
		return new BasicPropertyBinder(binderContext);
	}
	
	private final SimpleValueBinder simpleValueBinder;
	private final PropertyBinder propertyBinder;
	
	private BasicPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		simpleValueBinder = SimpleValueBinder.create(binderContext);
		propertyBinder = PropertyBinder.create(binderContext);
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
