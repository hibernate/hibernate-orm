package org.hibernate.tool.internal.reveng.binder;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class PropertyBinder {

	private static final Logger LOGGER = Logger.getLogger(PropertyBinder.class.getName());

	public static Property bind(
			Table table, 
			String defaultCatalog,
			String defaultSchema,
			String propertyName, 
			Value value, 
			boolean insertable, 
			boolean updatable, 
			boolean lazy, 
			String cascade, 
			String propertyAccessorName,
			ReverseEngineeringStrategy revengStrategy) {
    	LOGGER.log(Level.INFO, "Building property " + propertyName);
        Property prop = new Property();
		prop.setName(propertyName);
		prop.setValue(value);
		prop.setInsertable(insertable);
		prop.setUpdateable(updatable);
		prop.setLazy(lazy);
		prop.setCascade(cascade==null?"none":cascade);
		prop.setPropertyAccessorName(propertyAccessorName==null?"property":propertyAccessorName);
		MetaAttributesBinder.bindMetaAttributes(
				prop, 
				revengStrategy, 
				table,
				defaultCatalog,
				defaultSchema);
		return prop;
	}

}
