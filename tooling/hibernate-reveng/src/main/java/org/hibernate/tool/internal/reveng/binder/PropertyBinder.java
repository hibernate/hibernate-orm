package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevEngUtils;

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
		return bindMetaAttributes(
				prop, 
				revengStrategy, 
				table,
				defaultCatalog,
				defaultSchema);
	}

    private static Property bindMetaAttributes(
    		Property property, 
    		ReverseEngineeringStrategy revengStrategy,
    		Table table,
    		String defaultCatalog,
    		String defaultSchema) {
    	Iterator<Selectable> columnIterator = property.getValue().getColumnIterator();
		while(columnIterator.hasNext()) {
			Column col = (Column) columnIterator.next();
			Map<String,MetaAttribute> map = getColumnToMetaAttributesInRevengStrategy(
					revengStrategy, 
					table, 
					defaultCatalog, 
					defaultSchema, 
					col.getName());
			if(map!=null) { 
				property.setMetaAttributes(map);
			}
		}

		return property;
    }

	private static Map<String,MetaAttribute> getColumnToMetaAttributesInRevengStrategy(
			ReverseEngineeringStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema,
			String column) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		if (result == null) {
			String catalog = RevEngUtils.getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = RevEngUtils.getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		}
		return result;
	}
	
}
