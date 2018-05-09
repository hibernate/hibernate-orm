package org.hibernate.cfg.binder;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyBinder {

	private static final Logger log = LoggerFactory.getLogger(PropertyBinder.class);

	public static Property makeProperty(
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
    	log.debug("Building property " + propertyName);
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
